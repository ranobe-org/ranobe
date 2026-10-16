package org.ranobe.ranobe.export;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Lang;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.util.ListUtils;
import org.ranobe.ranobe.util.SourceUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;

/**
 * Runs one job from wherever it left off: fetches the chapter list, the text of every chapter
 * (from the offline copy when there is one) and its images into the work folder, then builds
 * the EPUB. Finished chapters are remembered in the job and on disk, so a resumed job never
 * fetches anything twice.
 * <p>
 * Network failures are retried with backoff and a lost connection is waited out without using
 * up retries. A chapter that still fails is left out, and a run of failures stops the job since
 * the source is most likely down or blocking us.
 */
class ExportRunner {
    private static final int FETCH_ATTEMPTS = 4;
    private static final int MAX_FAILURES_IN_A_ROW = 5;
    private static final int SAVE_EVERY = 10;
    private static final long MIN_FREE_BYTES = 50L * 1024 * 1024;
    private static final String[] STORED_FORMATS = {ImageInfo.JPEG, ImageInfo.PNG, ImageInfo.GIF};
    private final Context context;
    private final ExportJob job;
    private final JobControl control;
    private final File workDir;
    private final File imageDir;
    private final Listener listener;
    private final NetworkMonitor network;
    private final Source source;
    ExportRunner(Context context, ExportJob job, JobControl control, File workDir, Listener listener) {
        this.context = context.getApplicationContext();
        this.job = job;
        this.control = control;
        this.workDir = workDir;
        this.imageDir = new File(workDir, "images");
        this.listener = listener;
        this.network = new NetworkMonitor(context);
        this.source = SourceManager.getSource(job.novel.sourceId);
    }

    // keeps JPEG, PNG and GIF as they are, anything else (usually WebP) is re-encoded
    private static File storeImage(File temp, File dir, String name) throws IOException {
        try {
            String format = ImageInfo.format(temp);
            if (format != null && ImageInfo.isEpubCore(format)) {
                File target = new File(dir, name + "." + format);
                if (!temp.renameTo(target))
                    throw new IOException("Couldn't save " + target.getName());
                return target;
            }

            Bitmap bitmap = decode(temp);
            if (bitmap == null) throw new IOException("Unsupported or damaged image");
            try {
                // keep transparency when there is some, JPEG would turn it black
                boolean alpha = bitmap.hasAlpha();
                File target = new File(dir, name + "." + (alpha ? ImageInfo.PNG : ImageInfo.JPEG));
                File converted = new File(dir, name + ".convert");
                try (OutputStream out = new FileOutputStream(converted)) {
                    if (!bitmap.compress(alpha ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 90, out)) {
                        throw new IOException("Couldn't convert image");
                    }
                }
                if (!converted.renameTo(target)) {
                    //noinspection ResultOfMethodCallIgnored
                    converted.delete();
                    throw new IOException("Couldn't save " + target.getName());
                }
                return target;
            } finally {
                bitmap.recycle();
            }
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    // big illustrations can run out of memory, fall back to a smaller copy rather than drop them
    private static Bitmap decode(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        for (int sample = 1; sample <= 4; sample *= 2) {
            options.inSampleSize = sample;
            try {
                return BitmapFactory.decodeFile(file.getPath(), options);
            } catch (OutOfMemoryError e) {
                // try again at a smaller size
            }
        }
        return null;
    }

    private static File copy(File from, File to) throws IOException {
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
        }
        return to;
    }

    /**
     * The finished image for {@code name} from an earlier run, or null.
     */
    private static File existing(File dir, String name) {
        for (String format : STORED_FORMATS) {
            File file = new File(dir, name + "." + format);
            if (file.length() > 0) return file;
        }
        return null;
    }

    static String formatIndex(float index) {
        if (index == Math.rint(index)) return String.valueOf((long) index);
        return String.valueOf(index);
    }

    /**
     * A short, user facing explanation of a failure.
     */
    static String describe(Context context, Throwable e) {
        if (e instanceof FatalException) return e.getMessage();
        if (e instanceof UnknownHostException || e instanceof ConnectException) {
            return context.getString(R.string.export_error_offline);
        }
        if (e instanceof SocketTimeoutException)
            return context.getString(R.string.export_error_timeout);
        if (e instanceof OutOfMemoryError) return context.getString(R.string.export_error_memory);
        String message = e.getLocalizedMessage();
        return message == null || message.trim().isEmpty() ? context.getString(R.string.export_error_unknown) : message;
    }

    void run() throws Exception {
        if (!imageDir.isDirectory() && !imageDir.mkdirs())
            throw new IOException("Couldn't create " + imageDir);

        if (job.chapterCount() == 0) fetchChapters();
        recheckFinishedChapters();
        downloadCover();

        job.phase = ExportJob.Phase.CONTENT;
        listener.onChanged(job);
        List<ExportJob.Item> items = items();
        List<ExportJob.Item> failedInARow = new ArrayList<>();
        int sinceSave = 0;
        for (int i = 0; i < items.size(); i++) {
            ExportJob.Item item = items.get(i);
            if (item.done) continue;
            ensureSpace(workDir.getUsableSpace(), MIN_FREE_BYTES);
            try {
                fetchChapter(i, item);
                failedInARow.clear();
                job.message = null;
            } catch (JobControl.StoppedException e) {
                throw e;
            } catch (Exception e) {
                job.message = describe(context, e);
                item.missing = true;
                failedInARow.add(item);
                if (failedInARow.size() >= MAX_FAILURES_IN_A_ROW) {
                    // try these again on the next run rather than leave them all out
                    for (ExportJob.Item failed : failedInARow) {
                        failed.missing = false;
                        failed.done = false;
                    }
                    throw e;
                }
            }
            item.done = true;
            if (item.missing || ++sinceSave >= SAVE_EVERY) {
                sinceSave = 0;
                listener.onChanged(job);
            } else {
                listener.onProgress(job);
            }
        }
        listener.onChanged(job);

        int missing = job.missingCount();
        if (missing > 0 && !job.allowMissing) {
            job.missingChapters = missing;
            job.status = ExportJob.Status.INCOMPLETE;
            job.message = context.getResources().getQuantityString(R.plurals.export_missing_chapters, missing, missing);
            return;
        }
        build();
    }

    private List<ExportJob.Item> items() {
        synchronized (job.chapters) {
            return new ArrayList<>(job.chapters);
        }
    }

    private void fetchChapters() throws Exception {
        job.phase = ExportJob.Phase.CHAPTERS;
        listener.onChanged(job);
        List<Chapter> all = withRetries(() -> {
            List<Chapter> chapters = source.chapters(job.novel);
            if (chapters == null || chapters.isEmpty())
                throw new IOException("The source returned no chapters");
            return chapters;
        });
        List<ExportJob.Item> picked = new ArrayList<>();
        for (Chapter c : ListUtils.sortById(all)) {
            if (job.inRange(c.id)) picked.add(ExportJob.Item.of(c));
        }
        if (picked.isEmpty()) {
            throw new FatalException(context.getString(job.hasRange() ? R.string.export_no_chapters_in_range : R.string.export_no_chapters));
        }
        synchronized (job.chapters) {
            job.chapters.clear();
            job.chapters.addAll(picked);
        }
        listener.onChanged(job);
    }

    // the work folder is in the cache, Android may have cleared some of it while the job was paused
    private void recheckFinishedChapters() {
        boolean changed = false;
        List<ExportJob.Item> items = items();
        for (int i = 0; i < items.size(); i++) {
            ExportJob.Item item = items.get(i);
            if (item.done && !item.missing && !chapterFile(i).isFile()) {
                item.done = false;
                changed = true;
            }
        }
        if (changed) listener.onChanged(job);
    }

    // a missing cover isn't worth failing over, the book just goes without one
    private void downloadCover() {
        String cover = job.novel.cover;
        if (cover == null || !cover.trim().startsWith("http") || existing(workDir, "cover") != null)
            return;
        try {
            withRetries(() -> fetchImage(cover.trim(), workDir, "cover"));
        } catch (JobControl.StoppedException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchChapter(int position, ExportJob.Item item) throws Exception {
        String content = offlineContent(item);
        if (content == null) {
            content = withRetries(() -> {
                Chapter result = source.chapter(item.toChapter(job.novel));
                if (result == null || result.content == null || result.content.trim().isEmpty()) {
                    throw new IOException(context.getString(R.string.export_empty_chapter));
                }
                return result.content;
            });
        }
        String text = localizeImages(content);
        File target = chapterFile(position);
        File temp = new File(target.getPath() + ".part");
        try (OutputStream out = new FileOutputStream(temp)) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
        }
        if (!temp.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
            throw new IOException("Couldn't save " + target.getName());
        }
    }

    // chapters downloaded for offline reading are already in the database
    private String offlineContent(ExportJob.Item item) {
        try {
            Chapter saved = RanobeDatabase.database().chapters().getSync(item.url);
            if (saved != null && saved.content != null && !saved.content.trim().isEmpty())
                return saved.content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Points every image of the chapter at a copy in the work folder. Images that can't be
     * fetched keep their url and are left out of the book.
     */
    private String localizeImages(String content) {
        Matcher matcher = SourceUtils.IMAGE_TAG.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            control.check();
            String url = matcher.group(1).trim();
            String replacement = url;
            String name = "i" + Long.toHexString(SourceUtils.generateId(url));
            File file = existing(imageDir, name);
            try {
                if (file == null && url.startsWith("http")) {
                    file = withRetries(() -> fetchImage(url, imageDir, name));
                } else if (file == null && new File(url).isFile()) {
                    // saved by the offline download
                    file = storeImage(copy(new File(url), new File(imageDir, name + ".part")), imageDir, name);
                }
            } catch (JobControl.StoppedException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (file != null) replacement = "images/" + file.getName();
            matcher.appendReplacement(result, Matcher.quoteReplacement(SourceUtils.imageTag(replacement)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private File fetchImage(String url, File dir, String name) throws IOException {
        File temp = new File(dir, name + ".part");
        try {
            HttpClient.DOWNLOAD(url, new HashMap<>(), temp);
            if (temp.length() == 0) throw new IOException("Empty image");
            return storeImage(temp, dir, name);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private void build() throws Exception {
        job.phase = ExportJob.Phase.BUILDING;
        job.buildDone = 0;
        job.buildTotal = 0;
        job.message = null;
        listener.onChanged(job);

        EpubBuilder.Book book = new EpubBuilder.Book();
        book.id = "urn:uuid:" + job.id;
        book.title = title();
        book.author = job.novel.authors == null ? null : String.join(", ", job.novel.authors);
        book.description = job.novel.summary;
        book.language = language();
        book.source = job.novel.url;
        book.cover = existing(workDir, "cover");
        book.baseDir = workDir;

        long bytes = book.cover == null ? 0 : book.cover.length();
        List<ExportJob.Item> items = items();
        for (int i = 0; i < items.size(); i++) {
            File text = chapterFile(i);
            if (!text.isFile()) continue;
            book.sections.add(new EpubBuilder.Section(chapterTitle(items.get(i)), text));
            bytes += text.length();
        }
        File[] images = imageDir.listFiles();
        if (images != null) for (File image : images) bytes += image.length();
        if (book.sections.isEmpty())
            throw new FatalException(context.getString(R.string.export_no_chapters));
        // text compresses well, the images don't
        ensureSpace(EpubOutput.freeSpace(context), bytes + 20L * 1024 * 1024);

        String fileName = EpubOutput.fileName(book.title);
        EpubOutput output = EpubOutput.create(context, fileName);
        CountingOutputStream counter;
        Uri uri;
        try {
            counter = new CountingOutputStream(output.open());
            try (OutputStream out = new BufferedOutputStream(counter, 64 * 1024)) {
                EpubBuilder.write(book, out, (done, total) -> {
                    control.check();
                    job.buildDone = done;
                    job.buildTotal = total;
                    listener.onProgress(job);
                });
            }
            uri = output.commit();
        } catch (Throwable t) {
            output.abort();
            throw t;
        }

        job.outputUri = uri.toString();
        job.outputName = fileName;
        job.outputSize = counter.count;
        job.missingChapters = job.missingCount();
        job.message = null;
        job.status = ExportJob.Status.COMPLETED;
    }

    private <T> T withRetries(Callable<T> task) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= FETCH_ATTEMPTS; attempt++) {
            control.check();
            network.awaitConnected(control, this::waitingForNetwork);
            try {
                T result = task.call();
                job.waitingForNetwork = false;
                return result;
            } catch (JobControl.StoppedException e) {
                throw e;
            } catch (Exception e) {
                control.check();
                last = e;
            }
            if (attempt < FETCH_ATTEMPTS) control.sleep(Math.min(30_000L, 2000L << (attempt - 1)));
        }
        throw last;
    }

    private void waitingForNetwork() {
        job.waitingForNetwork = true;
        listener.onProgress(job);
    }

    // pausing beats failing, the user can free up space and resume
    private void ensureSpace(long free, long needed) {
        if (free >= needed) return;
        control.stop(JobControl.Stop.PAUSE, context.getString(R.string.export_storage_full));
        control.check();
    }

    private File chapterFile(int position) {
        return new File(workDir, String.format(Locale.ROOT, "c%05d.txt", position + 1));
    }

    private String language() {
        String lang = source.metadata().lang;
        if (lang == null || lang.trim().isEmpty()) return "en";
        if (Lang.eng.equals(lang)) return "en";
        return lang.trim();
    }

    private String title() {
        String name = job.novel.name == null || job.novel.name.trim().isEmpty() ? "Novel" : job.novel.name.trim();
        if (!job.hasRange()) return name;
        List<ExportJob.Item> items = items();
        if (items.isEmpty()) return name;
        String first = formatIndex(items.get(0).index);
        String last = formatIndex(items.get(items.size() - 1).index);
        return context.getString(R.string.export_book_title_range, name, first.equals(last) ? first : first + "-" + last);
    }

    private String chapterTitle(ExportJob.Item item) {
        if (item.name != null && !item.name.trim().isEmpty()) return item.name.trim();
        return context.getString(R.string.export_chapter_title, formatIndex(item.index));
    }

    interface Listener {
        // frequent, may be throttled
        void onProgress(ExportJob job);

        // something worth saving happened
        void onChanged(ExportJob job);
    }

    /**
     * An error retrying won't fix.
     */
    static class FatalException extends Exception {
        FatalException(String message) {
            super(message);
        }
    }

    private static class CountingOutputStream extends FilterOutputStream {
        long count;

        CountingOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            count += len;
        }
    }
}
