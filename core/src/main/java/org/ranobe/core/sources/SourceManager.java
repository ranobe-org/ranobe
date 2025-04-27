package org.ranobe.core.sources;


import org.ranobe.core.sources.en.AllNovel;
import org.ranobe.core.sources.en.AzyNovel;
import org.ranobe.core.sources.en.BoxNovel;
import org.ranobe.core.sources.en.FreeWebNovel;
import org.ranobe.core.sources.en.LightNovelBtt;
import org.ranobe.core.sources.en.LightNovelHeaven;
import org.ranobe.core.sources.en.LightNovelPub;
import org.ranobe.core.sources.en.LightNovelWorld;
import org.ranobe.core.sources.en.NewNovel;
import org.ranobe.core.sources.en.NovelBin;
import org.ranobe.core.sources.en.ReadLightNovel;
import org.ranobe.core.sources.en.ReadWebNovels;
import org.ranobe.core.sources.en.VipNovel;
import org.ranobe.core.sources.en.WordRain69;
import org.ranobe.core.sources.en.WuxiaWorld;
import org.ranobe.core.sources.ru.RanobeHub;

import java.util.HashMap;
import java.util.Map;

public class SourceManager {
    private SourceManager() throws IllegalAccessException {
        throw new IllegalAccessException("Cannot initialize this class ;)");
    }

    public static Source getSource(int sourceId) {
        try {
            Class<?> klass = getSources().get(sourceId);
            if (klass == null) {
                throw new ClassNotFoundException("Source not found with source id : " + sourceId);
            }
            return (Source) klass.newInstance();
        } catch (Exception e) {
            return new ReadLightNovel();
        }
    }

    public static Source getSourceByDomain(String domain) {
        try {
            Class<?> klass = getSourcesByDomain().get(domain);
            if (klass == null) {
                throw new ClassNotFoundException("Source not found with domain : " + domain);
            }
            return (Source) klass.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<Integer, Class<?>> getSources() {
        HashMap<Integer, Class<?>> sources = new HashMap<>();
        sources.put(1, ReadLightNovel.class);
        sources.put(2, VipNovel.class);
        sources.put(3, LightNovelBtt.class);
        sources.put(4, LightNovelPub.class);
        sources.put(5, RanobeHub.class);
        sources.put(7, AllNovel.class);
        sources.put(8, AzyNovel.class);
        sources.put(9, LightNovelHeaven.class);
        sources.put(10, NewNovel.class);
        sources.put(11, ReadWebNovels.class);
        sources.put(12, BoxNovel.class);
        sources.put(13, WuxiaWorld.class);
        sources.put(15, LightNovelWorld.class);
        sources.put(16, FreeWebNovel.class);
        sources.put(17, WordRain69.class);
        sources.put(18, NovelBin.class);

        return sources;
    }

    public static Map<String, Class<?>> getSourcesByDomain() {
        HashMap<String, Class<?>> sources = new HashMap<>();
        sources.put("readlightnovel.me", ReadLightNovel.class);
        sources.put("vipnovel.com", VipNovel.class);
        sources.put("lightnovelbtt.com", LightNovelBtt.class);
        sources.put("light-novelpub.com", LightNovelPub.class);
        sources.put("ranobehub.org", RanobeHub.class);
        sources.put("allnovel.org", AllNovel.class);
        sources.put("azynovel.com", AzyNovel.class);
        sources.put("lightnovelheaven.com", LightNovelHeaven.class);
        sources.put("newnovel.org", NewNovel.class);
        sources.put("readwebnovels.net", ReadWebNovels.class);
        sources.put("boxnovel.com", BoxNovel.class);
        sources.put("wuxiaworld.site", WuxiaWorld.class);
        sources.put("lightnovelworld.org", LightNovelWorld.class);
        sources.put("freewebnovel.com", FreeWebNovel.class);
        sources.put("wordrain69.com", WordRain69.class);
        sources.put("novelbin.me", NovelBin.class);

        return sources;
    }
}
