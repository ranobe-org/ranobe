package org.ranobe.ranobe.worker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.ranobe.ranobe.R;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.sources.SourceManager;
import org.ranobe.ranobe.ui.main.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class NewChapterWorker extends Worker {
    public static final String TAG = "NewChapterWorker";
    private static final int NOTIFICATION_ID = 1001;

    public NewChapterWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<Novel> novels = RanobeDatabase.database().novels().listSync();
            if (novels.isEmpty()) return Result.success();

            List<String> updatedNovels = new ArrayList<>();
            int totalNewChapters = 0;

            for (Novel novel : novels) {
                try {
                    Source source = SourceManager.getSource(novel.sourceId);
                    List<Chapter> chapters = source.chapters(novel);
                    int fetchedCount = chapters.size();
                    int knownCount = novel.lastKnownChapterCount;

                    if (knownCount > 0 && fetchedCount > knownCount) {
                        int newCount = fetchedCount - knownCount;
                        totalNewChapters += newCount;
                        updatedNovels.add(novel.name + " (" + newCount + " new)");
                    }

                    // Always update the known count after a successful fetch
                    RanobeDatabase.database().novels().updateLastKnownChapterCount(fetchedCount, novel.url);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to check chapters for novel: " + novel.name, e);
                }
            }

            if (!updatedNovels.isEmpty()) {
                showNotification(updatedNovels, totalNewChapters);
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Worker failed", e);
            return Result.retry();
        }
    }

    private void showNotification(List<String> updatedNovels, int totalNewChapters) {
        Context context = getApplicationContext();

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Ranobe.EXTRA_NAVIGATE_TO_LIBRARY, true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = totalNewChapters + " new chapter" + (totalNewChapters == 1 ? "" : "s") + " available";

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
                .setBigContentTitle(title);
        for (String line : updatedNovels) {
            style.addLine(line);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Ranobe.NOTIF_CHAPTER_UPDATE_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_library)
                .setContentTitle(title)
                .setContentText(updatedNovels.size() + " novel" + (updatedNovels.size() == 1 ? "" : "s") + " updated")
                .setStyle(style)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
