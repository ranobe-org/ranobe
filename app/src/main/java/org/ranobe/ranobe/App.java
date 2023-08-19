package org.ranobe.ranobe;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
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
    }
}
