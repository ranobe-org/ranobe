package org.ranobe.ranobe.export;

import android.content.Context;
import android.text.format.Formatter;

import org.ranobe.ranobe.R;

/**
 * The one line status of a job, shared by the notification and the downloads screen.
 */
public final class ExportText {
    private ExportText() {
    }

    public static String status(Context context, ExportJob job) {
        switch (job.status) {
            case QUEUED:
                return context.getString(R.string.export_status_queued);
            case PAUSED:
                return context.getString(R.string.export_status_paused);
            case FAILED:
                return context.getString(R.string.export_status_failed);
            case INCOMPLETE:
                return context.getResources().getQuantityString(R.plurals.export_missing_chapters, job.missingChapters, job.missingChapters);
            case COMPLETED:
                return context.getString(R.string.export_status_completed, EpubOutput.FOLDER, Formatter.formatShortFileSize(context, job.outputSize));
            default:
                return running(context, job);
        }
    }

    private static String running(Context context, ExportJob job) {
        if (job.waitingForNetwork) return context.getString(R.string.export_status_waiting_network);
        switch (job.phase) {
            case CHAPTERS:
                return context.getString(R.string.export_status_chapters);
            case BUILDING:
                return context.getString(R.string.export_status_building, job.percent());
            default:
                int total = job.chapterCount();
                return context.getString(R.string.export_status_chapter, Math.min(total, job.chaptersDone() + 1), total);
        }
    }

    /**
     * True while the progress can't be told, the bar should spin.
     */
    public static boolean indeterminate(ExportJob job) {
        return job.status == ExportJob.Status.RUNNING && (job.waitingForNetwork || job.phase == ExportJob.Phase.CHAPTERS);
    }
}
