package org.ranobe.ranobe.util;

import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;

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

    public static List<Novel> sortByName(List<Novel> items, boolean asc) {
        List<Novel> sorted = new ArrayList<>(items);
        Collections.sort(sorted, (a, b) -> asc ? a.name.compareToIgnoreCase(b.name) : b.name.compareToIgnoreCase(a.name));
        return sorted;
    }
}
