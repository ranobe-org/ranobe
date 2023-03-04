package org.ranobe.ranobe.database.converters;


import androidx.room.TypeConverter;

import java.util.Arrays;
import java.util.List;

public class ListConverter {
    @TypeConverter
    public static String listToString(List<String> value) {
        if (value == null  || value.isEmpty()) return "";
        return String.join("\t", value);
    }

    @TypeConverter
    public static List<String> stringToList(String value) {
        return Arrays.asList(value.split("\t"));
    }
}
