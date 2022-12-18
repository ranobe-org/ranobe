package org.ranobe.ranobe.util;

import org.ranobe.ranobe.models.ChapterItem;

import java.util.ArrayList;
import java.util.Collections;
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

    public static List<ChapterItem> sortById(List<ChapterItem> items) {
        List<ChapterItem> sorted = new ArrayList<>(items);
        Collections.sort(sorted, (a, b) -> Float.compare(a.id, b.id));
        return sorted;
    }
}
