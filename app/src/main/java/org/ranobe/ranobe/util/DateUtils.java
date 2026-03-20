package org.ranobe.ranobe.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateUtils {

    // Convert timestamp (milliseconds) to a formatted string
    private static String formatTimestamp(long timestamp, String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // Default readable format
    public static String formatDefault(long timestamp) {
        return formatTimestamp(timestamp, "yyyy-MM-dd HH:mm:ss");
    }

    // Short date format
    public static String formatShortDate(long timestamp) {
        return formatTimestamp(timestamp, "dd MMM yyyy");
    }

    // Relative time (e.g., "5 minutes ago") - simple version
    public static String getRelativeTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        }
    }
}

