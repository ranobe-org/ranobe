package org.ranobe.ranobe.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadService extends Service {

    public static final String ACTION_DOWNLOAD_COMPLETE = "org.ranobe.ranobe.DOWNLOAD_COMPLETE";
    public static final String ACTION_DOWNLOAD_FAILED = "org.ranobe.ranobe.DOWNLOAD_FAILED";
    public static final String EXTRA_CHAPTER_URL = "chapter_url";

    public static final String CHANNEL_ID = "download_notification";
    private static final int NOTIFICATION_ID = 2001;

    private static final Queue<DownloadItem> queue = new ConcurrentLinkedQueue<>();
    private static final Set<String> pendingUrls = Collections.synchronizedSet(new HashSet<>());
    private static boolean isProcessing = false;

    private ExecutorService executor;
    private NotificationManager notificationManager;
    private int completedCount = 0;

    public static void enqueue(Context context, Chapter chapter, int sourceId) {
        if (pendingUrls.contains(chapter.url)) return;
        pendingUrls.add(chapter.url);
        queue.add(new DownloadItem(chapter, sourceId));
        context.startService(new Intent(context, DownloadService.class));
    }

    public static boolean isPending(String chapterUrl) {
        return pendingUrls.contains(chapterUrl);
    }

    public static int getPendingCount() {
        return pendingUrls.size();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ensureChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("Starting downloads…"));
        if (!isProcessing) {
            isProcessing = true;
            completedCount = 0;
            processQueue(startId);
        }
        return START_NOT_STICKY;
    }

    private void processQueue(int startId) {
        executor.execute(() -> {
            while (!queue.isEmpty()) {
                DownloadItem item = queue.poll();
                if (item == null) continue;

                int remaining = queue.size() + 1;
                updateNotification("Downloading: " + item.chapter.name, remaining);

                try {
                    Chapter result = new Repository(item.sourceId).chapterSync(item.chapter);
                    if (result != null && result.content != null && !result.content.isEmpty()) {
                        RanobeDatabase.database().chapters().save(result);
                        completedCount++;
                        Intent broadcast = new Intent(ACTION_DOWNLOAD_COMPLETE);
                        broadcast.setPackage(getPackageName());
                        broadcast.putExtra(EXTRA_CHAPTER_URL, item.chapter.url);
                        sendBroadcast(broadcast);
                    } else {
                        broadcastFailed(item.chapter.url);
                    }
                } catch (Exception e) {
                    broadcastFailed(item.chapter.url);
                } finally {
                    pendingUrls.remove(item.chapter.url);
                }
            }

            isProcessing = false;
            stopForeground(true);
            stopSelf(startId);
        });
    }

    private void broadcastFailed(String url) {
        Intent broadcast = new Intent(ACTION_DOWNLOAD_FAILED);
        broadcast.setPackage(getPackageName());
        broadcast.putExtra(EXTRA_CHAPTER_URL, url);
        sendBroadcast(broadcast);
    }

    private void updateNotification(String text, int remaining) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text + " (" + remaining + " left)"));
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloading chapters")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_download)
                .setOngoing(true)
                .setSilent(true)
                .build();
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Chapter Downloads",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows progress while downloading chapters for offline reading");
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static class DownloadItem {
        final Chapter chapter;
        final int sourceId;

        DownloadItem(Chapter chapter, int sourceId) {
            this.chapter = chapter;
            this.sourceId = sourceId;
        }
    }
}
