package org.ranobe.core.util;


import org.ranobe.core.models.Chapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListUtils {
    public static List<Chapter> searchByName(String keyword, List<Chapter> items) {
        List<Chapter> result = new ArrayList<>();
        for (Chapter item : items) {
            if (item.name.toLowerCase().contains(keyword)) {
                result.add(item);
            }
        }
        return result;
    }

    public static List<Chapter> sortById(List<Chapter> items) {
        List<Chapter> sorted = new ArrayList<>(items);
        Collections.sort(sorted, (a, b) -> Float.compare(a.id, b.id));
        return sorted;
    }
}
