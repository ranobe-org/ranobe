package org.ranobe.ranobe.worker;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class ChapterUpdateScheduler {
    private static final String WORK_NAME = "chapter_update_check";
    private static final long INTERVAL_HOURS = 12;

    public static void schedule(Context context) {
//        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
//                NewChapterWorker.class,
//                INTERVAL_HOURS,
//                TimeUnit.HOURS
//        ).build();

        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelAllWorkByTag(WORK_NAME);
        wm.cancelAllWorkByTag(NewChapterWorker.TAG);
        wm.enqueue(new OneTimeWorkRequest.Builder(NewChapterWorker.class)
                        .setInitialDelay(10, TimeUnit.SECONDS)
                .addTag(NewChapterWorker.TAG)
                .build());

//        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//                WORK_NAME,
//                ExistingPeriodicWorkPolicy.KEEP,
//                request
//        );
    }

    public static void cancel(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME);
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelAllWorkByTag(WORK_NAME);
        wm.cancelAllWorkByTag(NewChapterWorker.TAG);
        wm.cancelUniqueWork(WORK_NAME);
        wm.cancelUniqueWork(NewChapterWorker.TAG);
    }
}
