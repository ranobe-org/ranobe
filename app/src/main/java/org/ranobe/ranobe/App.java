package org.ranobe.ranobe;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

public class App extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static Context getContext() {
        return App.context;
    }

    public void onCreate() {
        super.onCreate();
        App.context = getApplicationContext();
    }
}
