package org.ranobe.ranobe.services.download;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import org.ranobe.ranobe.config.Ranobe;

public class DownloadService extends Service implements DownloadManager.DownloadStatusListener {
    private DownloadNotifier notifier;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int sourceId = intent.getIntExtra(Ranobe.KEY_SOURCE_ID, 1);
        String novelUrl = intent.getStringExtra(Ranobe.KEY_NOVEL_URL);

        notifier = new DownloadNotifier(getApplicationContext(), novelUrl.hashCode());
        notifier.showNotification();

        new DownloadManager(sourceId, novelUrl, this).start();
        return START_NOT_STICKY;
    }

    @Override
    public void complete(int current, int max) {
        if (current == max) {
            notifier.complete();
        } else {
            notifier.setProgress(current, max);
        }
    }

    @Override
    public void error(String error) {
        // Unused
    }
}
