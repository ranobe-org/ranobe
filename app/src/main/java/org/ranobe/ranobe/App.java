package org.ranobe.ranobe;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.google.android.material.color.DynamicColors;

import org.ranobe.ranobe.config.Ranobe;

public class App extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static Context getContext() {
        return App.context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(Ranobe.DEBUG, "app launched");
        DynamicColors.applyToActivitiesIfAvailable(this);
        App.context = getApplicationContext();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel chapterUpdates = new NotificationChannel(
                Ranobe.NOTIF_CHAPTER_UPDATE_CHANNEL_ID,
                Ranobe.NOTIF_CHAPTER_UPDATE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        chapterUpdates.setDescription("Alerts when new chapters are available for library novels");
        manager.createNotificationChannel(chapterUpdates);
    }
}
