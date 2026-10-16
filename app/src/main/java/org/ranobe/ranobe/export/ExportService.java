package org.ranobe.ranobe.export;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import org.ranobe.ranobe.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the app alive while EPUB exports run, showing their progress. The work itself happens in
 * {@link ExportQueue}, this service starts when a job is queued and stops once none are left.
 */
public class ExportService extends Service {
    private static final long WAKE_LOCK_TIMEOUT_MS = 6 * 60 * 60 * 1000L;

    private final Map<String, ExportJob.Status> seen = new HashMap<>();
    private final Runnable onChanged = this::update;
    private ExportQueue queue;
    private ExportNotifications notifications;
    private PowerManager.WakeLock wakeLock;
    private boolean stopping;

    static void start(Context context) {
        try {
            ContextCompat.startForegroundService(context, new Intent(context, ExportService.class));
        } catch (RuntimeException e) {
            // not allowed from the background on newer Android, the queue still runs while the app is open
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        queue = ExportQueue.get(this);
        notifications = new ExportNotifications(this);
        for (ExportJob job : queue.snapshot()) seen.put(job.id, job.status);

        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        // the screen going off must not stall a long export
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":epub");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
        queue.addListener(onChanged);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        stopping = false;
        // must happen right away for every start, even when there turns out to be nothing to do
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC : 0;
        ServiceCompat.startForeground(this, ExportNotifications.FOREGROUND_ID, notifications.foreground(queue.snapshot()), type);
        update();
        // the queue restores jobs as paused after the process dies, nothing to restart here
        return START_NOT_STICKY;
    }

    private void update() {
        if (stopping) return;
        List<ExportJob> jobs = queue.snapshot();
        for (ExportJob job : jobs) {
            ExportJob.Status before = seen.put(job.id, job.status);
            if (before == job.status) continue;
            if (job.isActive()) {
                notifications.clear(job);
            } else if (before == ExportJob.Status.QUEUED || before == ExportJob.Status.RUNNING) {
                notifications.showResult(job);
            }
        }
        if (queue.hasActive()) {
            notifications.showForeground(jobs);
            return;
        }
        stopping = true;
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    // Android 15 caps data sync services at a few hours a day
    @Override
    public void onTimeout(int startId, int fgsType) {
        queue.pauseAll(getString(R.string.export_paused_by_system));
        update();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        queue.removeListener(onChanged);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
