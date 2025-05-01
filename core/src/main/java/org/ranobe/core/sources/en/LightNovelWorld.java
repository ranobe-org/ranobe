package org.ranobe.core.sources.en;


import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.ranobe.core.models.Chapter;
import org.ranobe.core.models.DataSource;
import org.ranobe.core.models.Filter;
import org.ranobe.core.models.Lang;
import org.ranobe.core.models.Novel;
import org.ranobe.core.network.HttpClient;
import org.ranobe.core.sources.Source;
import org.ranobe.core.util.NumberUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LightNovelWorld implements Source {
    private static final String BASE_URL = "https://lightnovelworld.org";
    private static final int SOURCE_ID = 15;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = BASE_URL;
        source.name = "Light Novel World";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://lightnovelworld.org/logo.ico";
        source.isActive = false;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        List<Novel> items = new ArrayList<>();
        String web = String.format(Locale.getDefault(), "%s/genre-all/sort-new/status-all/all-novel?page=%d", BASE_URL, page);
        Element doc = Jsoup.parse(HttpClient.GET(web, new HashMap<>()));

        for (Element element : doc.select("li.novel-item")) {
            String url = element.select("a").attr("href").trim();

            if (!url.isEmpty()) {
                Novel item = new Novel(url);
                item.sourceId = SOURCE_ID;
                item.name = element.select("a > h4").text().trim();
                item.cover = element.select("a > figure > img").attr("data-src");
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = SOURCE_ID;
        novel.name = doc.select("h1[itemprop=name]").text().trim();
        novel.cover = doc.select("figure.cover > img").attr("data-src").trim();
        novel.summary = String.join("\n\n", doc.select("div.inner > p").eachText());
        novel.authors = Collections.singletonList(doc.select("span[itemprop=author]").text());
        novel.genres = doc.select("div.categories > ul > li").eachText();

        for (Element element : doc.select("div.header-stats > span")) {
            if (element.select("small").text().contains("Status")) {
                novel.status = element.select("strong").text().trim();
                break;
            }
        }

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        String base = novel.url.concat("/chapters");
        Element doc = Jsoup.parse(HttpClient.GET(base, new HashMap<>()));

        while (true) {
            Element next = doc.select("a[rel=next]").first();

            for (Element element : doc.select("ul.chapter-list > li")) {
                Chapter item = new Chapter(novel.url);

                item.url = element.select("a").attr("href").trim();
                item.name = element.select("a > strong").text().trim();
                item.id = NumberUtils.toFloat(item.name);
                items.add(item);
            }

            if (next != null) {
                String nextUrl = next.attr("href");
                doc = Jsoup.parse(HttpClient.GET(nextUrl, new HashMap<>()));
            } else {
                break;
            }
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));
        chapter.content = String.join("\n\n", doc.select("div#content > p").eachText());
        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (filters.hashKeyword() && page == 1) {
            String keyword = filters.getKeyword();
            String web = BASE_URL + "/ajax/searchLive?inputContent=" + keyword;
            return parseNovel(web);
        }
        return new ArrayList<>();
    }

    private List<Novel> parseNovel(String web) throws JSONException, IOException {
        List<Novel> items = new ArrayList<>();
        String response = HttpClient.GET(web, new HashMap<>());
        Element doc = Jsoup.parse(new JSONObject(response).getString("html"));

        for (Element element : doc.select("li.novel-item")) {
            String url = element.select("a").attr("href").trim();

            if (url.length() > 0) {
                Novel item = new Novel(url);
                item.sourceId = SOURCE_ID;
                item.name = element.select("a").attr("title").trim();
                item.cover = element.select("figure.novel-cover > img").attr("src");
                items.add(item);
            }
        }
        return items;
    }
}
