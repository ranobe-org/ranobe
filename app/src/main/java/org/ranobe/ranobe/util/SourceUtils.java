package org.ranobe.ranobe.util;

public class SourceUtils {
    public static Long generateId(String url) {
        long hash = 1125899906842597L;
        for(int i = 0; i < url.length(); i++) {
            hash = 31 * hash + url.charAt(i);
        }
        return hash;
    }
}
