package org.ranobe.ranobe.services.download;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.content.ContextCompat;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;

public class DownloadNotifier {
    private final NotificationManager notificationManager;
    private final Notification.Builder builder;
    private final Context context;

    public DownloadNotifier(Context context) {
        this.context = context;
        notificationManager = ContextCompat.getSystemService(context, NotificationManager.class);
        builder = getNotificationBuilder();
    }

    public void showNotification() {
        createNotificationChannel();
        notifyNow();
    }

    @SuppressLint("DefaultLocale")
    public void setProgress(int current, int max) {
        builder.setProgress(max, current, false)
                .setContentTitle(String.format("Downloading... %d/%d", current, max));
        notifyNow();
    }

    public void complete() {
        builder.setOngoing(false)
                .setContentTitle("Download completed.");
        notifyNow();
    }

    private void notifyNow() {
        notificationManager.notify(Ranobe.NOTIF_DOWNLOAD_CONTINUE_ID, builder.build());
    }

    private Notification.Builder getNotificationBuilder() {
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_download)
                .setOngoing(true)
                .setContentTitle("Downloading started...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(Ranobe.NOTIF_DOWNLOAD_CHANNEL_NAME);
        }
        builder.setPriority(Notification.PRIORITY_DEFAULT);
        return builder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(
                    Ranobe.NOTIF_DOWNLOAD_CHANNEL_NAME,
                    Ranobe.NOTIF_DOWNLOAD_CHANNEL_NAME,
                    importance
            );
            notificationManager.createNotificationChannel(channel);
        }
    }
}
