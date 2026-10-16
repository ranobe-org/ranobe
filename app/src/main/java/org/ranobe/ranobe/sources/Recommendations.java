package org.ranobe.ranobe.sources;

import org.jsoup.nodes.Element;
import org.ranobe.ranobe.models.Novel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parsers for the "you may also like" blocks that several sites share.
 */
public final class Recommendations {
    private static final int MAX = 20;

    private Recommendations() {
    }

    /**
     * WordPress Madara theme sites (Wuxia World, Light Novel Heaven, WordRain69):
     * the "YOU MAY ALSO LIKE" row of .related-reading-wrap blocks.
     */
    public static List<Novel> madara(Element doc, int sourceId, String selfUrl) {
        List<Novel> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (selfUrl != null) seen.add(selfUrl);
        for (Element wrap : doc.select(".related-reading-wrap")) {
            Element link = wrap.selectFirst(".related-reading-content a[href], .related-reading-img a[href]");
            if (link == null) continue;
            String url = link.absUrl("href");
            if (url.isEmpty()) url = link.attr("href").trim();
            if (url.isEmpty() || !seen.add(url)) continue;

            Novel novel = new Novel(url, sourceId);
            String title = link.attr("title").trim();
            novel.name = title.isEmpty() ? link.text().trim() : title;
            Element img = wrap.selectFirst("img");
            if (img != null)
                novel.cover = fullSizeImage(firstNonEmpty(img.attr("data-src"), img.attr("src")));
            items.add(novel);
            if (items.size() >= MAX) break;
        }
        return items;
    }

    // Madara serves thumbnails like cover-75x106.jpg, the original sits next to it
    public static String fullSizeImage(String url) {
        if (url == null) return "";
        return url.trim().replaceAll("-\\d+x\\d+(\\.\\w+)$", "$1");
    }

    public static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}
