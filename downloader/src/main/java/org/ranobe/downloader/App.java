package org.ranobe.downloader;

import android.app.Application;

import org.ranobe.core.network.HttpClient;


public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HttpClient.initialize(getApplicationContext());
    }
}
