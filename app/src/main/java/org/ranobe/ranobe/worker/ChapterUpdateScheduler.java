package org.ranobe.ranobe.worker;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ChapterUpdateScheduler {
    private static final long INTERVAL_HOURS = 12;
    private static final long BACKOFF_DELAY_MINUTES = 15;

    public static void schedule(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                NewChapterWorker.class,
                INTERVAL_HOURS,
                TimeUnit.HOURS
        )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_MINUTES, TimeUnit.MINUTES)
                .addTag(NewChapterWorker.TAG)
                .build();

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
