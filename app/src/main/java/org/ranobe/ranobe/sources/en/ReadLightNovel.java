package org.ranobe.ranobe.sources.en;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.sources.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ReadLightNovel implements Source {
    public final HashMap<String, String> HEADERS = new HashMap<String, String> () {{
        put("Cache-Control", "public max-age=604800");
        put("x-requested-with", "XMLHttpRequest");
    }};
    // user agent
    public final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201";
    public final String baseUrl = "https://www.readlightnovel.me";

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = 1;
        source.url = baseUrl;
        return source;
    }

    @Override
    public List<NovelItem> novels(int page) throws Exception {
        List<NovelItem> items = new ArrayList<>();
        String url = baseUrl + "/top-novels/most-viewed/" + page;
        Element doc = Jsoup.connect(url).headers(HEADERS).userAgent(USER_AGENT).get().body();

        for (Element element: doc.select("div.top-novel-block")) {
            NovelItem item = new NovelItem();
            item.sourceId = 1;
            item.name = element.select("div.top-novel-header > h2 > a").text().trim();
            item.url = element.select("div.top-novel-header > h2 > a").attr("href").trim();
            item.cover = element.select("div.top-novel-cover > a > img").attr("src").trim();
            items.add(item);
        }

        return items;
    }

    @Override
    public Novel details(String url) {
        return null;
    }

    @Override
    public List<ChapterItem> chapters(String url) {
        return null;
    }

    @Override
    public Chapter chapter(String url) {
        return null;
    }
}
