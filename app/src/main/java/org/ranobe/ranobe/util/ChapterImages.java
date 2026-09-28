package org.ranobe.ranobe.util;

import android.content.Context;
import android.util.Log;

import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.network.HttpClient;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;

public class ChapterImages {
    public static String saveLocally(Context context, String content) {
        if (content == null || content.isEmpty()) return content;

        File dir = new File(context.getFilesDir(), Ranobe.CHAPTER_IMAGES_DIR);
        if (!dir.exists() && !dir.mkdirs()) return content;

        Matcher matcher = SourceUtils.IMAGE_TAG.matcher(content);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            String replacement = url;
            if (url.startsWith("http")) {
                File file = new File(dir, String.valueOf(SourceUtils.generateId(url)));
                if (file.length() > 0 || download(url, file)) {
                    replacement = file.getAbsolutePath();
                }
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(SourceUtils.imageTag(replacement)));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static boolean download(String url, File file) {
        File temp = new File(file.getPath() + ".part");
        try {
            HttpClient.DOWNLOAD(url, new HashMap<>(), temp);
            return temp.renameTo(file);
        } catch (Exception e) {
            Log.w("ChapterImages", "image download failed: " + url, e);
            temp.delete();
            return false;
        }
    }
}
