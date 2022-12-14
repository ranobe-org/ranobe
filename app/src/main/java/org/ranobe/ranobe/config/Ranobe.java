package org.ranobe.ranobe.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class Ranobe {
    public static final String DEBUG = "ranobe.debug";
    public static final String PACKAGE_NAME = "org.ranobe.ranobe";
    public static final String SETTINGS_THEME_MODE = "shared_pref_theme_mode";

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

    public static void storeThemeMode(Context context, int theme) {
        getEditor(context).putInt(Ranobe.SETTINGS_THEME_MODE, theme).apply();
    }

    public static int getThemeMode(Context context) {
        return getSharedPref(context).getInt(Ranobe.SETTINGS_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }
}
