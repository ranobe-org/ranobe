package org.ranobe.ranobe.sources;

import org.ranobe.ranobe.sources.en.LightNovelBtt;
import org.ranobe.ranobe.sources.en.LightNovelPub;
import org.ranobe.ranobe.sources.en.Ranobe;
import org.ranobe.ranobe.sources.en.ReadLightNovel;
import org.ranobe.ranobe.sources.en.VipNovel;
import org.ranobe.ranobe.sources.ru.RanobeHub;

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
        sources.put(2, VipNovel.class);
        sources.put(3, LightNovelBtt.class);
        sources.put(4, LightNovelPub.class);
        sources.put(5, RanobeHub.class);
        sources.put(6, Ranobe.class);

        return sources;
    }
}
