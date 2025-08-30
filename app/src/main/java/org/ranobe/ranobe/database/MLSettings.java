package org.ranobe.ranobe.database;

import com.google.mlkit.nl.translate.TranslateLanguage;
import java.util.Set;

public class MLSettings {
    private String from;
    private String to;

    public MLSettings(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public static Set<String> getSupportedLanguages() {
        return TranslateLanguage.getAllLanguages();
    }

    public static String fromShortToDisplay(String code) {
        switch (code) {
            case "en":
                return "English";
            case "es":
                return "Spanish";
            case "ja":
                return "Japanese";
            default:
                return code;
        }
    }

    public boolean isValid() {
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) return false;
        if (!getSupportedLanguages().contains(to) || !getSupportedLanguages().contains(from)) return false;
        if (from.equals(to)) return false;
        return true;
    }

    public String getFrom() { return from; }
    public String getTo() { return to; }

    public String getFromDisplay() { return fromShortToDisplay(from); }
    public String getToDisplay() { return fromShortToDisplay(to); }
}