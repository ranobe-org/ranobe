package org.ranobe.ranobe.util;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtils {
    private static final Pattern floatingPoint = Pattern.compile("[+-]?\\d+(\\.\\d+)?");

    public static float toFloat(String value) {
        Matcher matcher = NumberUtils.floatingPoint.matcher(value);
        if (matcher.find()) {
            String number = matcher.group();
            if (number.length() > 0) {
                return Float.parseFloat(number);
            }
        }
        return 0F;
    }

    public static int toInt(String value) {
        String number = value.replaceAll("\\D", "");
        if (number.length() > 0) {
            return Integer.parseInt(number);
        }
        return 0;
    }

    public static int getRandom(int size) {
        return new Random().nextInt(size);
    }
}
