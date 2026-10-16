package org.ranobe.ranobe.export;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ranobe.ranobe.R;
import org.ranobe.ranobe.models.Novel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Every EPUB export, newest first. Jobs run one at a time on a background thread while
 * {@link ExportService} keeps the app alive and shows progress. The list is saved after
 * every meaningful change so jobs survive the app being killed, they come back paused.
 * <p>
 * All public methods are called on the main thread.
 */
public class ExportQueue {
    private static final long PROGRESS_THROTTLE_MS = 300;
    // job level retries for errors the runner didn't handle itself, before giving up
    private static final long[] AUTO_RETRY_DELAYS_MS = {15_000, 60_000, 180_000};

    @SuppressLint("StaticFieldLeak")
    private static ExportQueue instance;

    private final Context context;
    private final File stateFile;
    private final File workRoot;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<ExportJob> jobs = new CopyOnWriteArrayList<>();
    private final MutableLiveData<List<ExportJob>> live = new MutableLiveData<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile ExportJob running;
    private volatile JobControl control;
    private boolean draining;
    private boolean dispatchPending;
    private final Runnable dispatch = this::dispatch;

    private ExportQueue(Context context) {
        this.context = context.getApplicationContext();
        File stateDir = new File(this.context.getFilesDir(), "exports");
        //noinspection ResultOfMethodCallIgnored
        stateDir.mkdirs();
        stateFile = new File(stateDir, "jobs.json");
        workRoot = new File(this.context.getCacheDir(), "epub");
        restore();
        live.setValue(new ArrayList<>(jobs));
    }

    @MainThread
    public static ExportQueue get(Context context) {
        if (instance == null) instance = new ExportQueue(context);
        return instance;
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteRecursively(child);
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    public LiveData<List<ExportJob>> jobs() {
        return live;
    }

    public List<ExportJob> snapshot() {
        return new ArrayList<>(jobs);
    }

    /**
     * Called on the main thread after any change, for {@link ExportService}.
     */
    void addListener(Runnable listener) {
        listeners.add(listener);
    }

    void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    boolean hasActive() {
        for (ExportJob job : jobs) if (job.isActive()) return true;
        return false;
    }

    @Nullable
    public ExportJob find(String id) {
        for (ExportJob job : jobs) if (job.id.equals(id)) return job;
        return null;
    }

    /**
     * The newest job for this novel, if any.
     */
    @Nullable
    public ExportJob findForNovel(String novelUrl) {
        if (novelUrl == null) return null;
        for (ExportJob job : jobs) if (novelUrl.equals(job.novel.url)) return job;
        return null;
    }

    /**
     * Queues an export of the chapters numbered {@code from} to {@code to}, NaN for no limit.
     */
    public ExportJob enqueue(Novel novel, float from, float to) {
        ExportJob job = new ExportJob(novel, from, to);
        jobs.add(0, job);
        changed();
        kick();
        return job;
    }

    public void pause(String id) {
        ExportJob job = find(id);
        if (job == null) return;
        synchronized (this) {
            if (!job.isActive()) return;
            job.status = ExportJob.Status.PAUSED;
            job.message = null;
            job.waitingForNetwork = false;
            if (job == running && control != null) control.stop(JobControl.Stop.PAUSE, null);
        }
        changed();
    }

    /**
     * Picks a paused, failed or incomplete job up where it left off.
     */
    public void resume(String id) {
        ExportJob job = find(id);
        if (job == null || job.isActive() || job.status == ExportJob.Status.COMPLETED) return;
        if (job.status == ExportJob.Status.INCOMPLETE) {
            // try the missing chapters again
            synchronized (job.chapters) {
                for (ExportJob.Item item : job.chapters) {
                    if (item.missing) {
                        item.done = false;
                        item.missing = false;
                    }
                }
            }
        }
        job.status = ExportJob.Status.QUEUED;
        job.message = null;
        changed();
        kick();
    }

    /**
     * Builds the book from what was fetched, leaving out chapters that couldn't be.
     */
    public void buildAnyway(String id) {
        ExportJob job = find(id);
        if (job == null || job.status != ExportJob.Status.INCOMPLETE) return;
        job.allowMissing = true;
        job.status = ExportJob.Status.QUEUED;
        job.message = null;
        changed();
        kick();
    }

    /**
     * Stops every job, e.g. when the system ends the foreground service.
     */
    void pauseAll(String reason) {
        for (ExportJob job : jobs) {
            if (!job.isActive()) continue;
            pause(job.id);
            job.message = reason;
        }
        changed();
    }

    /**
     * Stops the job and throws away what it fetched. A finished book stays in Downloads.
     */
    public void cancel(String id) {
        ExportJob job = find(id);
        if (job == null) return;
        synchronized (this) {
            jobs.remove(job);
            if (job == running && control != null) {
                // the worker cleans up once the runner has stopped
                control.stop(JobControl.Stop.CANCEL, null);
            } else {
                io.execute(() -> deleteRecursively(workDir(job)));
            }
        }
        new ExportNotifications(context).clear(job);
        changed();
    }

    private File workDir(ExportJob job) {
        return new File(workRoot, job.id);
    }

    @MainThread
    private void kick() {
        if (hasActive()) ExportService.start(context);
        if (draining) return;
        draining = true;
        worker.execute(this::drain);
    }

    // worker thread: runs queued jobs, oldest first, until none are left
    private void drain() {
        while (true) {
            ExportJob next = null;
            JobControl jobControl = new JobControl();
            // picked and marked running under the lock so a pause or cancel can't slip in between
            synchronized (this) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    ExportJob job = jobs.get(i);
                    if (job.status == ExportJob.Status.QUEUED) {
                        next = job;
                        break;
                    }
                }
                if (next != null) {
                    next.status = ExportJob.Status.RUNNING;
                    next.waitingForNetwork = false;
                    running = next;
                    control = jobControl;
                }
            }
            if (next == null) {
                // re-check on the main thread, which is where jobs get queued
                main.post(() -> {
                    draining = false;
                    for (ExportJob job : jobs) {
                        if (job.status == ExportJob.Status.QUEUED) {
                            kick();
                            return;
                        }
                    }
                });
                return;
            }
            // a stray interrupt from an earlier job must not cut this one short
            //noinspection ResultOfMethodCallIgnored
            Thread.interrupted();
            runJob(next, jobControl);
        }
    }

    private void runJob(ExportJob job, JobControl jobControl) {
        postChanged();

        ExportRunner.Listener listener = new ExportRunner.Listener() {
            @Override
            public void onProgress(ExportJob j) {
                postProgress();
            }

            @Override
            public void onChanged(ExportJob j) {
                postChanged();
            }
        };

        for (int attempt = 0; ; attempt++) {
            try {
                new ExportRunner(context, job, jobControl, workDir(job), listener).run();
                // the runner sets COMPLETED or INCOMPLETE
                if (job.status == ExportJob.Status.COMPLETED) deleteRecursively(workDir(job));
                break;
            } catch (JobControl.StoppedException e) {
                if (jobControl.stopped() == null) {
                    // interrupted without anyone asking, treat it like any other error
                    if (retryLater(job, jobControl, attempt, e)) continue;
                }
                break;
            } catch (ExportRunner.FatalException e) {
                fail(job, e);
                break;
            } catch (Throwable t) {
                t.printStackTrace();
                if (retryLater(job, jobControl, attempt, t)) continue;
                break;
            }
        }

        synchronized (this) {
            if (jobControl.stopped() == JobControl.Stop.PAUSE && job.status == ExportJob.Status.RUNNING) {
                // the runner paused itself, e.g. storage ran out
                job.status = ExportJob.Status.PAUSED;
                job.message = jobControl.reason();
            }
            if (jobControl.stopped() == null && job.status == ExportJob.Status.RUNNING) {
                // never leave a job looking busy when nothing works on it
                job.status = ExportJob.Status.FAILED;
                job.message = context.getString(R.string.export_error_unknown);
            }
            job.waitingForNetwork = false;
            running = null;
            control = null;
        }
        if (jobControl.stopped() == JobControl.Stop.CANCEL) deleteRecursively(workDir(job));
        postChanged();
    }

    // waits and asks for another go, or marks the job failed once out of retries
    private boolean retryLater(ExportJob job, JobControl jobControl, int attempt, Throwable t) {
        if (jobControl.stopped() != null) return false;
        if (attempt >= AUTO_RETRY_DELAYS_MS.length) {
            fail(job, t);
            return false;
        }
        long delay = AUTO_RETRY_DELAYS_MS[attempt];
        job.message = context.getString(R.string.export_retrying_in, delay / 1000, ExportRunner.describe(context, t));
        postChanged();
        try {
            jobControl.sleep(delay);
        } catch (JobControl.StoppedException e) {
            return false;
        }
        job.message = null;
        return true;
    }

    private synchronized void fail(ExportJob job, Throwable t) {
        if (job.status != ExportJob.Status.RUNNING) return;
        job.status = ExportJob.Status.FAILED;
        job.message = ExportRunner.describe(context, t);
    }

    private void postChanged() {
        main.post(this::changed);
    }

    private void postProgress() {
        main.post(() -> {
            if (dispatchPending) return;
            dispatchPending = true;
            main.postDelayed(dispatch, PROGRESS_THROTTLE_MS);
        });
    }

    @MainThread
    private void changed() {
        save();
        main.removeCallbacks(dispatch);
        dispatch();
    }

    @MainThread
    private void dispatch() {
        dispatchPending = false;
        live.setValue(new ArrayList<>(jobs));
        for (Runnable listener : listeners) listener.run();
    }

    private void save() {
        String json;
        try {
            JSONArray array = new JSONArray();
            for (ExportJob job : jobs) array.put(job.toJson());
            json = new JSONObject().put("version", 1).put("jobs", array).toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        io.execute(() -> write(json));
    }

    // write then rename, a crash mid write never loses the previous state
    private void write(String json) {
        File temp = new File(stateFile.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(temp)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (!temp.renameTo(stateFile)) {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private void restore() {
        if (!stateFile.isFile()) return;
        try (InputStream in = new FileInputStream(stateFile)) {
            byte[] data = new byte[(int) stateFile.length()];
            int read = 0;
            while (read < data.length) {
                int n = in.read(data, read, data.length - read);
                if (n < 0) break;
                read += n;
            }
            JSONArray array = new JSONObject(new String(data, 0, read, StandardCharsets.UTF_8)).getJSONArray("jobs");
            for (int i = 0; i < array.length(); i++) {
                try {
                    ExportJob job = ExportJob.fromJson(array.getJSONObject(i));
                    if (job.isActive()) {
                        // the app was killed mid export, wait for the user to carry on
                        job.status = ExportJob.Status.PAUSED;
                        job.message = context.getString(R.string.export_interrupted);
                    }
                    jobs.add(job);
                } catch (JSONException | IllegalArgumentException e) {
                    // skip a job we can't read rather than lose them all
                    e.printStackTrace();
                }
            }
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        // leftovers of jobs that no longer exist
        io.execute(() -> {
            File[] dirs = workRoot.listFiles();
            if (dirs == null) return;
            for (File dir : dirs) {
                if (find(dir.getName()) == null) deleteRecursively(dir);
            }
        });
    }
}
