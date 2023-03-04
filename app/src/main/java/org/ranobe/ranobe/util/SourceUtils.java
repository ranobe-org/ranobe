package org.ranobe.ranobe.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SourceUtils {
    public static Long generateId(String url) {
        long hash = 1125899906842597L;
        for (int i = 0; i < url.length(); i++) {
            hash = 31 * hash + url.charAt(i);
        }
        return hash;
    }

    public static String cleanContent(String raw) {
        return raw.replaceAll("\n\n", "\n");
    }

    // does simple concatenation and nothing else
    public static String buildUrl(String... args) {
        String url = "";
        for (String a : args) {
            if (a == null) {
                a = "";
            }
            url = url.concat(a);
        }
        return url;
    }

    public static String getDate(int timestamp) {
        try {
            return SimpleDateFormat.getDateTimeInstance().format(new Date(timestamp * 1000L));
        } catch (Exception e) {
            return "";
        }
    }
}
