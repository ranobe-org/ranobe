package org.ranobe.core.util;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtils {
    private static final Pattern floatingPoint = Pattern.compile("[+-]?\\d+(\\.\\d+)?");
    private static final String allNumbersRegex = "\\D";

    public static float toFloat(String value) {
        if (value == null) return 0F;

        Matcher matcher = floatingPoint.matcher(value);
        if (!matcher.find()) return 0F;

        String number = matcher.group();
        return number.length() == 0 ? 0F : Float.parseFloat(number);
    }

    public static int toInt(String value) {
        if (value == null) return 0;
        String number = value.replaceAll(allNumbersRegex, "");
        return number.length() > 0 ? Integer.parseInt(number) : 0;
    }

    public static int getRandom(int size) {
        return new Random().nextInt(size);
    }
}
