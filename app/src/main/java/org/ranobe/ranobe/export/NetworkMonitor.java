package org.ranobe.ranobe.export;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Lets exports wait out a lost connection instead of burning their retries on it.
 */
class NetworkMonitor {
    private static final long POLL_MS = 3000;

    private final ConnectivityManager connectivity;

    NetworkMonitor(Context context) {
        connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    boolean isConnected() {
        if (connectivity == null) return true;
        try {
            Network network = connectivity.getActiveNetwork();
            if (network == null) return false;
            NetworkCapabilities caps = connectivity.getNetworkCapabilities(network);
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        } catch (SecurityException e) {
            // can't tell, let the request find out
            return true;
        }
    }

    /**
     * Blocks until there is a connection, or the job is stopped.
     */
    void awaitConnected(JobControl control, Runnable onWaiting) {
        if (isConnected()) return;
        onWaiting.run();
        while (!isConnected()) control.sleep(POLL_MS);
    }
}
