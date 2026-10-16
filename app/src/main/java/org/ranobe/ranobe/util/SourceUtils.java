package org.ranobe.ranobe.util;

import org.jsoup.nodes.Element;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class SourceUtils {
    public static final Pattern IMAGE_TAG = Pattern.compile("\\[img](.+?)\\[/img]");

    public static String imageTag(String url) {
        return "[img]".concat(url).concat("[/img]");
    }

    public static Long generateId(String url) {
        long hash = 1125899906842597L;
        for (int i = 0; i < url.length(); i++) {
            hash = 31 * hash + url.charAt(i);
        }
        return hash;
    }

    /**
     * Chapter html as the app's content: one paragraph per block, separated by blank lines,
     * with images as [img]url[/img].
     */
    public static String contentFromHtml(Element root) {
        if (root == null) return "";
        StringBuilder out = new StringBuilder();
        for (Element block : root.select("p, h1, h2, h3, h4, blockquote, li, img")) {
            if (block.tagName().equals("img")) {
                String src = block.hasAttr("data-src") ? block.absUrl("data-src") : block.absUrl("src");
                if (src.isEmpty()) src = block.attr("src").trim();
                if (src.startsWith("http")) appendBlock(out, imageTag(src));
                continue;
            }
            // nested blocks are handled on their own (select() counts the block itself too)
            int nested = block.select("p, li, blockquote").size() - (block.is("p, li, blockquote") ? 1 : 0);
            if (nested > 0) continue;
            String text = block.text().trim();
            if (!text.isEmpty()) appendBlock(out, text);
        }
        // plain text with no paragraph tags at all
        if (out.length() == 0) return root.wholeText().trim();
        return out.toString();
    }

    private static void appendBlock(StringBuilder out, String block) {
        if (out.length() > 0) out.append("\n\n");
        out.append(block);
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

    public static String parseIsoDate(String timestamp) {
        Date date = isoToDate(timestamp);
        return date == null ? "" : SimpleDateFormat.getDateTimeInstance().format(date);
    }

    // "2019-12-03T17:45:12Z" -> "Dec 3, 2019", in the device's locale
    public static String parseIsoDay(String timestamp) {
        Date date = isoToDate(timestamp);
        return date == null ? "" : SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(date);
    }

    // UTC timestamps with or without milliseconds
    private static Date isoToDate(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return null;
        for (String pattern : new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'"}) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = format.parse(timestamp);
                if (date != null) return date;
            } catch (Exception ignored) {
                // try the next pattern
            }
        }
        return null;
    }
}
