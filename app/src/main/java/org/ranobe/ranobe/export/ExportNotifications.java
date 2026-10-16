package org.ranobe.ranobe.export;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.ui.downloads.DownloadsActivity;

import java.util.List;

/**
 * The ongoing progress notification and the per job ones left behind when a job finishes,
 * fails or pauses.
 */
final class ExportNotifications {
    static final int FOREGROUND_ID = 7001;
    private static final String CHANNEL_PROGRESS = "epub_progress";
    private static final String CHANNEL_RESULT = "epub_result";

    private final Context context;
    private final NotificationManagerCompat manager;

    ExportNotifications(Context context) {
        this.context = context.getApplicationContext();
        this.manager = NotificationManagerCompat.from(this.context);
        createChannels();
    }

    private static int jobNotificationId(ExportJob job) {
        return 7100 + (job.id.hashCode() & 0xffff);
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        NotificationChannel progress = new NotificationChannel(CHANNEL_PROGRESS,
                context.getString(R.string.export_channel_progress), NotificationManager.IMPORTANCE_LOW);
        progress.setShowBadge(false);
        NotificationChannel result = new NotificationChannel(CHANNEL_RESULT,
                context.getString(R.string.export_channel_result), NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(progress);
        nm.createNotificationChannel(result);
    }

    /**
     * Progress of the running job, or the next queued one.
     */
    Notification foreground(List<ExportJob> jobs) {
        ExportJob current = null;
        int active = 0;
        for (ExportJob job : jobs) {
            if (!job.isActive()) continue;
            active++;
            if (current == null || job.status == ExportJob.Status.RUNNING) current = job;
        }

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_PROGRESS)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(openDownloads())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        if (current == null) {
            return b.setContentTitle(context.getString(R.string.export_epub)).build();
        }
        b.setContentTitle(current.novel.name)
                .setContentText(ExportText.status(context, current))
                .setProgress(100, current.percent(), ExportText.indeterminate(current))
                .addAction(0, context.getString(R.string.export_pause), action(ExportActionReceiver.ACTION_PAUSE, current))
                .addAction(0, context.getString(R.string.export_cancel), action(ExportActionReceiver.ACTION_CANCEL, current));
        if (active > 1)
            b.setSubText(context.getResources().getQuantityString(R.plurals.export_more_queued, active - 1, active - 1));
        return b.build();
    }

    void showForeground(List<ExportJob> jobs) {
        notify(FOREGROUND_ID, foreground(jobs));
    }

    /**
     * What is left once a job stops running, replaced if it runs again.
     */
    void showResult(ExportJob job) {
        NotificationCompat.Builder b;
        String text;
        switch (job.status) {
            case COMPLETED:
                Uri uri = Uri.parse(job.outputUri);
                text = job.outputName;
                b = new NotificationCompat.Builder(context, CHANNEL_RESULT)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle(context.getString(R.string.export_done_title))
                        .setContentIntent(activity(job, "open", Intent.createChooser(EpubOutput.viewIntent(uri), job.outputName)))
                        .addAction(0, context.getString(R.string.export_share),
                                activity(job, "share", Intent.createChooser(EpubOutput.shareIntent(uri, job.novel.name), job.outputName)));
                break;
            case FAILED:
            case INCOMPLETE:
                text = job.message;
                b = new NotificationCompat.Builder(context, CHANNEL_RESULT)
                        .setSmallIcon(android.R.drawable.stat_notify_error)
                        .setContentTitle(context.getString(R.string.export_failed_title, job.novel.name))
                        .setContentIntent(openDownloads())
                        .addAction(0, context.getString(R.string.retry), action(ExportActionReceiver.ACTION_RESUME, job));
                break;
            case PAUSED:
                text = job.message != null ? job.message : ExportText.status(context, job);
                b = new NotificationCompat.Builder(context, CHANNEL_PROGRESS)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle(job.novel.name)
                        .setProgress(100, job.percent(), false)
                        .setSilent(true)
                        .setContentIntent(openDownloads())
                        .addAction(0, context.getString(R.string.export_resume), action(ExportActionReceiver.ACTION_RESUME, job))
                        .addAction(0, context.getString(R.string.export_cancel), action(ExportActionReceiver.ACTION_CANCEL, job));
                break;
            default:
                return;
        }
        b.setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(job.status != ExportJob.Status.PAUSED);
        notify(jobNotificationId(job), b.build());
    }

    void clear(ExportJob job) {
        manager.cancel(jobNotificationId(job));
    }

    private void notify(int id, Notification notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.notify(id, notification);
    }

    // opened with the home screen under it, so back lands in the app rather than closing it
    // when the app wasn't running
    private PendingIntent openDownloads() {
        return TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(new Intent(context, DownloadsActivity.class))
                .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent activity(ExportJob job, String what, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, (job.id + what).hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private PendingIntent action(String action, ExportJob job) {
        Intent intent = new Intent(context, ExportActionReceiver.class)
                .setAction(action)
                .putExtra(ExportActionReceiver.EXTRA_JOB_ID, job.id);
        return PendingIntent.getBroadcast(context, (job.id + action).hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
