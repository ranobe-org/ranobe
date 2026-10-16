package org.ranobe.ranobe.export;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Pause, resume and cancel buttons on the export notifications.
 */
public class ExportActionReceiver extends BroadcastReceiver {
    static final String ACTION_PAUSE = "org.ranobe.ranobe.export.PAUSE";
    static final String ACTION_RESUME = "org.ranobe.ranobe.export.RESUME";
    static final String ACTION_CANCEL = "org.ranobe.ranobe.export.CANCEL";
    static final String EXTRA_JOB_ID = "job_id";

    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra(EXTRA_JOB_ID);
        String action = intent.getAction();
        if (id == null || action == null) return;
        ExportQueue queue = ExportQueue.get(context);
        switch (action) {
            case ACTION_PAUSE:
                queue.pause(id);
                break;
            case ACTION_RESUME:
                queue.resume(id);
                break;
            case ACTION_CANCEL:
                queue.cancel(id);
                break;
        }
    }
}
