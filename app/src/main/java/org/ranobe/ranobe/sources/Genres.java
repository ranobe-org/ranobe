package org.ranobe.ranobe.sources;

import org.jsoup.nodes.Element;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Genre links from a details page, so a tapped genre can list the source's novels in it.
 */
public final class Genres {
    private Genres() {
    }

    /**
     * name -> absolute link for every genre anchor, relative links resolved against {@code base}.
     */
    public static Map<String, String> links(Iterable<Element> anchors, String base) {
        Map<String, String> links = new LinkedHashMap<>();
        for (Element a : anchors) {
            String name = a.text().trim();
            String href = a.attr("href").trim();
            if (name.isEmpty() || href.isEmpty()) continue;
            links.put(name, absolute(href, base));
        }
        return links;
    }

    public static String absolute(String href, String base) {
        if (href.startsWith("http")) return href;
        if (href.startsWith("//")) return "https:" + href;
        String root = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return root + (href.startsWith("/") ? href : "/" + href);
    }

    // ".../genre/action/" -> ".../genre/action/page/2/", WordPress style
    public static String wordpressPage(String url, int page) {
        if (page <= 1) return url;
        return (url.endsWith("/") ? url : url + "/") + "page/" + page + "/";
    }

    // "...?a=b" -> "...?a=b&page=2"
    public static String queryPage(String url, String param, int page) {
        if (page <= 1) return url;
        return url + (url.contains("?") ? "&" : "?") + param + "=" + page;
    }
}
