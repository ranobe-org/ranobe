package org.ranobe.ranobe.export;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Where finished books go: Downloads/Ranobe, visible to every reader app and file manager.
 * The file only shows up once it is complete, a failed build leaves nothing behind.
 */
public abstract class EpubOutput {
    public static final String FOLDER = "Ranobe";
    static final String MIME = "application/epub+zip";

    /**
     * Free bytes where the book will be written.
     */
    @SuppressWarnings("deprecation")
    static long freeSpace(Context context) {
        File dir = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || !canWritePublic(context)
                ? context.getExternalFilesDir(null)
                : Environment.getExternalStorageDirectory();
        return dir == null ? Long.MAX_VALUE : dir.getUsableSpace();
    }

    static EpubOutput create(Context context, String displayName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return new MediaStoreOutput(context, displayName);
        return new FileOutput(context, displayName);
    }

    /**
     * A file name that is safe on every file system, "Title.epub".
     */
    static String fileName(String title) {
        String name = title == null ? "" : title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        // dots at the ends upset some file systems
        name = name.replaceAll("^\\.+|\\.+$", "").trim();
        if (name.isEmpty()) name = "novel";
        if (name.length() > 100) name = name.substring(0, 100).trim();
        return name + ".epub";
    }

    public static Intent viewIntent(Uri uri) {
        return new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static Intent shareIntent(Uri uri, String title) {
        return new Intent(Intent.ACTION_SEND)
                .setType(MIME)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    private static boolean canWritePublic(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    abstract OutputStream open() throws IOException;

    /**
     * Makes the finished file visible and returns its uri.
     */
    abstract Uri commit() throws IOException;

    abstract void abort();

    @RequiresApi(Build.VERSION_CODES.Q)
    private static class MediaStoreOutput extends EpubOutput {
        private final ContentResolver resolver;
        private final String displayName;
        private Uri uri;

        MediaStoreOutput(Context context, String displayName) {
            this.resolver = context.getContentResolver();
            this.displayName = displayName;
        }

        @Override
        OutputStream open() throws IOException {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, displayName);
            values.put(MediaStore.Downloads.MIME_TYPE, MIME);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER);
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("Couldn't create the file in Downloads");
            OutputStream out = resolver.openOutputStream(uri, "w");
            if (out == null) throw new IOException("Couldn't write to Downloads");
            return out;
        }

        @Override
        Uri commit() {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, values, null, null);
            return uri;
        }

        @Override
        void abort() {
            if (uri == null) return;
            try {
                resolver.delete(uri, null, null);
            } catch (Exception ignored) {
                // pending entries are cleaned up by the system anyway
            }
            uri = null;
        }
    }

    // Android 9 and older: public Downloads with the storage permission, the app's own folder without
    private static class FileOutput extends EpubOutput {
        private final Context context;
        private final String displayName;
        private File target;
        private File part;

        FileOutput(Context context, String displayName) {
            this.context = context.getApplicationContext();
            this.displayName = displayName;
        }

        // "Name.epub", then "Name (1).epub" and so on
        private static File unique(File dir, String name) {
            File file = new File(dir, name);
            String base = name.substring(0, name.length() - ".epub".length());
            for (int i = 1; file.exists(); i++) file = new File(dir, base + " (" + i + ").epub");
            return file;
        }

        @Override
        @SuppressWarnings("deprecation")
        OutputStream open() throws IOException {
            File base = canWritePublic(context)
                    ? Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    : context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (base == null) throw new IOException("Storage isn't available");
            File dir = new File(base, FOLDER);
            if (!dir.isDirectory() && !dir.mkdirs())
                throw new IOException("Couldn't create " + dir);
            target = unique(dir, displayName);
            part = new File(dir, target.getName() + ".part");
            return new FileOutputStream(part);
        }

        @Override
        @SuppressWarnings("deprecation")
        Uri commit() throws IOException {
            if (!part.renameTo(target)) throw new IOException("Couldn't save " + target.getName());
            // let file managers and readers see it straight away
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)));
            return FileProvider.getUriForFile(context, context.getPackageName() + ".files", target);
        }

        @Override
        void abort() {
            //noinspection ResultOfMethodCallIgnored
            if (part != null) part.delete();
        }
    }
}
