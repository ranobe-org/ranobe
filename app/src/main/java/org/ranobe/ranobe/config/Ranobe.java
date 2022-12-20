package org.ranobe.ranobe.config;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

import org.ranobe.ranobe.App;
import org.ranobe.ranobe.models.ReaderTheme;

import java.util.HashMap;

public class Ranobe {
    public static final String DEBUG = "ranobe.debug";
    public static final String PACKAGE_NAME = "org.ranobe.ranobe";
    public static final String SETTINGS_THEME_MODE = "shared_pref_theme_mode";
    public static final String SETTINGS_READER_THEME = "shared_pref_reader_theme";
    public static final String SETTINGS_READER_FONT = "shared_pref_reader_font";
    public static final String SETTING_SELECTED_SOURCE = "shared_pref_selected_source";

    public static final String[] SILLY_EMOJI = new String[]{
            "( ╥﹏╥) ノシ",
            "༼ つ ◕_◕ ༽つ",
            "(❍ᴥ❍ʋ)",
            "(⊙＿⊙')",
            "(=____=)"
    };

    public static final HashMap<String, ReaderTheme> themes = new HashMap<String, ReaderTheme>() {{
        put("basic", new ReaderTheme("#fffbe0", "#222831"));
        put("basic_inverse", new ReaderTheme("#222831", "#fffbe0"));
        put("basic_dim", new ReaderTheme("#fef8e6", "#222831"));
        put("basic_dim_inverse", new ReaderTheme("#222831", "#fef8e6"));
        put("grey", new ReaderTheme("#eae7e7", "#161c2e"));
        put("grey_inverse", new ReaderTheme("#161c2e", "#eae7e7"));
        put("terminal", new ReaderTheme("#161c2e", "#feff89"));
        put("green", new ReaderTheme("#74f6a7", "#161c2e"));
    }};

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

    public static void storeReaderTheme(Context context, String theme) {
        getEditor(context).putString(Ranobe.SETTINGS_READER_THEME, theme).apply();
    }

    public static String getReaderTheme(Context context) {
        return getSharedPref(context).getString(Ranobe.SETTINGS_READER_THEME, null);
    }

    public static void storeReaderFont(Context context, Float size) {
        getEditor(context).putFloat(Ranobe.SETTINGS_READER_FONT, size).apply();
    }

    public static Float getReaderFont(Context context) {
        return getSharedPref(context).getFloat(Ranobe.SETTINGS_READER_FONT, 15);
    }

    public static void saveCurrentSource(int sourceId) {
        getEditor(App.getContext()).putInt(Ranobe.SETTING_SELECTED_SOURCE, sourceId).apply();
    }

    public static  int getCurrentSource() {
        return getSharedPref(App.getContext()).getInt(Ranobe.SETTING_SELECTED_SOURCE, 3);
    }
}
