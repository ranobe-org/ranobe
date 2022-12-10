package org.ranobe.ranobe.sources;

import org.ranobe.ranobe.sources.en.ReadLightNovel;

public class SourceManager {
    public static Source getSource (int sourceId) {
        try {
            return ReadLightNovel.class.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
