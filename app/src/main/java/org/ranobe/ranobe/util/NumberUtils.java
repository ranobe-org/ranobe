package org.ranobe.ranobe.util;

public class NumberUtils {
    public static float toFloat(String value) {
        String number = value.replaceAll("[^\\d.]+|\\.(?!\\d)", "");
        if (number.length() > 0) {
            return Float.parseFloat(number);
        }
        return 0F;
    }

    public static int toInt(String value) {
        String number = value.replaceAll("\\D", "");
        if (number.length() > 0) {
            return Integer.parseInt(value);
        }
        return 0;
    }
}
