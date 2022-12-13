package org.ranobe.ranobe.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import org.ranobe.ranobe.R;

import java.util.Arrays;
import java.util.List;

public class Ranobe {
    public static final String DEBUG = "ranobe.debug";
    public static final String PACKAGE_NAME = "org.ranobe.ranobe";

    public static final String SETTINGS_THEME = "shared_pref_theme";
    public static final String SETTINGS_THEME_MODE = "shared_pref_theme_mode";
    public static final List<Integer> ACCENT_LIST = Arrays.asList(
            R.color.pink,
            R.color.purple,
            R.color.deep_purple,
            R.color.indigo,
            R.color.blue,
            R.color.light_blue,
            R.color.cyan,
            R.color.teal,
            R.color.green,
            R.color.light_green,
            R.color.lime,
            R.color.yellow,
            R.color.amber,
            R.color.orange,
            R.color.deep_orange,
            R.color.red,
            R.color.brown,
            R.color.grey,
            R.color.blue_grey,

            R.color.red_300,
            R.color.pink_300,
            R.color.purple_300,
            R.color.deep_purple_300,
            R.color.red_300,
            R.color.indigo_300,
            R.color.blue_300,
            R.color.light_blue_300,
            R.color.cyan_300,
            R.color.teal_300,
            R.color.green_300,
            R.color.light_green_300,
            R.color.lime_300,
            R.color.yellow_300,
            R.color.amber_300,
            R.color.orange_300,
            R.color.deep_orange_300,
            R.color.brown_300,
            R.color.grey_300,
            R.color.blue_grey_300
    );

    private static SharedPreferences.Editor getEditor(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                Ranobe.PACKAGE_NAME, Context.MODE_PRIVATE
        );
        return sharedPreferences.edit();
    }

    private static SharedPreferences getSharedPref(Context context) {
        return context.getSharedPreferences(
                Ranobe.PACKAGE_NAME, Context.MODE_PRIVATE
        );
    }

    public static void storeTheme(Context context, int theme) {
        getEditor(context).putInt(Ranobe.SETTINGS_THEME, theme).apply();
    }

    public static int getTheme(Context context) {
        return getSharedPref(context).getInt(Ranobe.SETTINGS_THEME, R.color.blue);
    }

    public static void storeThemeMode(Context context, int theme) {
        getEditor(context).putInt(Ranobe.SETTINGS_THEME_MODE, theme).apply();
    }

    public static int getThemeMode(Context context) {
        return getSharedPref(context).getInt(Ranobe.SETTINGS_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
