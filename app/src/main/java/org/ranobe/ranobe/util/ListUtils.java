package org.ranobe.ranobe.util;

import org.ranobe.ranobe.models.ChapterItem;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
    public static List<ChapterItem> searchByName(String keyword, List<ChapterItem> items) {
        List<ChapterItem> result = new ArrayList<>();
        for (ChapterItem item : items) {
            if (item.name.toLowerCase().contains(keyword)) {
                result.add(item);
            }
        }
        return result;
    }
}
