package org.ranobe.ranobe.models;

import android.graphics.Color;

public class ReaderTheme {
    private final int text;
    private final int background;

    public ReaderTheme(String text, String background) {
        this.text = Color.parseColor(text);
        this.background = Color.parseColor(background);
    }

    public int getText() {
        return text;
    }

    public int getBackground() {
        return background;
    }
}
