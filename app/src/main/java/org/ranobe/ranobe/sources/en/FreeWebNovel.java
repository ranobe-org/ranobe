package org.ranobe.ranobe.sources.en;


import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Lang;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.util.NumberUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FreeWebNovel implements Source {
    private static final String BASE_URL = "https://freewebnovel.com";
    private static final int SOURCE_ID = 16;
    private static final int CHAPTER_PAGE_SIZE = 200;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = BASE_URL;
        source.name = "Free Web Novel";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://freewebnovel.com/static/freewebnovel/favicon.ico";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = BASE_URL.concat("/sort/latest-release/" + page + "/");
        return parseNovel(HttpClient.GET(web, new HashMap<>()));
    }
    private List<Novel> parseNovel(String response) {
        // https://freewebnovel.com/sort/latest-release/2/
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(response);

        for (Element element : doc.select("div.li-row")) {
            String url = element.select("div.pic > a").attr("href").trim();

            if (!url.isEmpty()) {
                Novel item = new Novel(BASE_URL + url);
                item.sourceId = SOURCE_ID;
                item.name = element.select("div.txt > h3.tit > a").text().trim();
                item.cover = getCover(element.select("div.pic"));
                items.add(item);
            }
        }
        return items;
    }

    private String getCover(Elements containers) {
        if (containers == null || containers.isEmpty()) return "";
        Element container = containers.first();
        if (container == null) return "";

        // 1. Check <picture> -> <source> srcset
        Element source = container.select("picture > source").first();
        if (source != null && source.hasAttr("srcset")) {
            String srcset = source.attr("srcset").trim();
            if (!srcset.isEmpty()) {
                String[] parts = srcset.split(",");
                String bestPart = parts[parts.length - 1].trim();
                int spaceIndex = bestPart.indexOf(' ');
                if (spaceIndex != -1) {
                    bestPart = bestPart.substring(0, spaceIndex).trim();
                }
                if (!bestPart.isEmpty()) {
                    return resolveUrl(bestPart);
                }
            }
        }

        // 2. Fallback to <img> src, data-src, data-original
        Element img = container.select("img").first();
        if (img != null) {
            String dataSrc = img.attr("data-src").trim();
            String dataOriginal = img.attr("data-original").trim();
            String src = img.attr("src").trim();

            String rawUrl = !dataSrc.isEmpty() ? dataSrc : (!dataOriginal.isEmpty() ? dataOriginal : src);
            if (!rawUrl.isEmpty()) {
                return resolveUrl(rawUrl);
            }
        }

        return "";
    }

    private String resolveUrl(String url) {
        if (url.startsWith("//")) {
            return "https:" + url;
        } else if (url.startsWith("/")) {
            return BASE_URL + url;
        } else if (!url.startsWith("http") && !url.isEmpty()) {
            return BASE_URL + "/" + url;
        }
        return url;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = SOURCE_ID;
        novel.name = doc.select("div.m-desc > h1").text().trim();
        novel.cover = getCover(doc.select("div.pic"));
        novel.summary = String.join("\n\n", doc.select("div.inner > p").eachText());

        for (Element element : doc.select("div.txt > div.item")) {
            String check = element.select("span").attr("title");
            if (check.contains("Status")) {
                novel.status = element.select("a").text().trim();
            } else if (check.contains("Author")) {
                novel.authors = element.select("a").eachText();
            } else if (check.contains("Genre")) {
                novel.genres = element.select("a").eachText();
            }
        }

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        // {"code":200,"html":"<li>...</li>","page":1,"pageSize":200,"totalPage":3,"totalChapters":404}
        List<Chapter> items = new ArrayList<>();
        int page = 1;
        int totalPage = 1;

        do {
            String web = novel.url + "?ajax=chapters&page=" + page + "&pageSize=" + CHAPTER_PAGE_SIZE;
            JSONObject json = new JSONObject(HttpClient.GET(web, new HashMap<>()));
            totalPage = json.optInt("totalPage", 1);

            Element doc = Jsoup.parseBodyFragment(json.optString("html", ""));
            for (Element element : doc.select("li > a")) {
                Chapter item = new Chapter(novel.url);

                item.url = resolveUrl(element.attr("href").trim());
                item.name = element.text().trim();
                item.id = NumberUtils.toFloat(item.name);
                items.add(item);
            }
            page++;
        } while (page <= totalPage);

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));
        chapter.content = String.join("\n\n", doc.select("div#article > p").eachText());
        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (filters.hashKeyword() && page == 1) {
            String keyword = filters.getKeyword();
            String web = BASE_URL + "/search/";
            HashMap<String, String> form = new HashMap<>();
            form.put("searchkey", keyword);
            String response = HttpClient.POST(web, new HashMap<>(), form);
            return parseNovel(response);
        }
        return new ArrayList<>();
    }
}
