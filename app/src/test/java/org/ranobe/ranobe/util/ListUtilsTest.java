package org.ranobe.ranobe.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ranobe.ranobe.util.ListUtils.searchByName;
import static org.ranobe.ranobe.util.ListUtils.sortById;

import org.junit.Before;
import org.junit.Test;
import org.ranobe.ranobe.models.Chapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListUtilsTest {
    private final List<Chapter> items = new ArrayList<>();

    @Before
    public void setUp() {
        for (int i = 0; i < 20; i++) {
            Chapter item = new Chapter(String.valueOf(i));
            item.id = i;
            item.name = String.format("item_%d", i);
            items.add(item);
        }
    }

    @Test
    public void searchByName_ShouldReturnEmptyForNoResults() {
        assertTrue(searchByName("item_21", items).isEmpty());
        assertTrue(searchByName("item_21", new ArrayList<>()).isEmpty());
    }

    @Test
    public void searchByName_ShouldReturnMatchedResults() {
        assertEquals(20, searchByName("item", items).size());
        assertEquals(11, searchByName("item_1", items).size());
        assertEquals(1, searchByName("19", items).size());
    }

    @Test
    public void sortById_ShouldReturnValidResult() {
        List<Chapter> reversed = new ArrayList<>(items);
        Collections.reverse(reversed);
        assertEquals(items, sortById(reversed));
    }
}