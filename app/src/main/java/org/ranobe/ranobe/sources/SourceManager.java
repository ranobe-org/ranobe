package org.ranobe.ranobe.sources;

import org.ranobe.ranobe.sources.en.ReadLightNovel;

import java.util.HashMap;

public class SourceManager {
    public static Source getSource(int sourceId) {
        try {
            Class<?> klass = getSources().get(sourceId);
            if (klass == null) {
                throw new ClassNotFoundException("Source not found with source id : " + sourceId);
            }
            return (Source) klass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return new ReadLightNovel();
        }
    }

    public static HashMap<Integer, Class<?>> getSources() {
        HashMap<Integer, Class<?>> sources = new HashMap<>();
        sources.put(1, ReadLightNovel.class);

        return sources;
    }
}
