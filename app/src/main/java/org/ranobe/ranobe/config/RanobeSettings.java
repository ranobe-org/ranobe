package org.ranobe.ranobe.config;

public class RanobeSettings {
    private static RanobeSettings INSTANCE = null;
    private int currentSource;

    private RanobeSettings() {
        currentSource = Ranobe.getCurrentSource();
    }

    public static RanobeSettings get() {
        if (INSTANCE != null) return INSTANCE;
        return INSTANCE = new RanobeSettings();
    }

    public void save() {
        Ranobe.saveCurrentSource(currentSource);
    }

    public int getCurrentSource() {
        return currentSource;
    }

    public RanobeSettings setCurrentSource(int sourceId) {
        currentSource = sourceId;
        return INSTANCE;
    }
}
