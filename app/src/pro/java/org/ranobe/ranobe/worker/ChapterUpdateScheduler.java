package org.ranobe.ranobe.worker;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ChapterUpdateScheduler {
    private static final long INTERVAL_HOURS = 12;

    public static void schedule(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                NewChapterWorker.class,
                INTERVAL_HOURS,
                TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NewChapterWorker.TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }

    public static void cancel(Context context) {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelUniqueWork(NewChapterWorker.TAG);
        wm.cancelAllWorkByTag(NewChapterWorker.TAG);
    }
}
