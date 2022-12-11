package org.ranobe.ranobe.sources.en;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.util.NumberUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ReadLightNovel implements Source {
    public final String baseUrl = "https://www.readlightnovel.me";
    public final String USER_AGENT = "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201";
    public final HashMap<String, String> HEADERS = new HashMap<String, String>() {{
        put("Cache-Control", "public max-age=604800");
        put("x-requested-with", "XMLHttpRequest");
    }};

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

        for (Element element : doc.select("div.top-novel-block")) {
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
    public Novel details(String url) throws IOException {
        Novel novel = new Novel();
        Element doc = Jsoup.connect(url).headers(HEADERS).userAgent(USER_AGENT).get().body();

        novel.sourceId = 1;
        novel.url = url;
        novel.name = doc.select("div.novel-cover > a > img").attr("alt").trim();
        novel.cover = doc.select("div.novel-cover > a > img").attr("src").trim();

        for (Element element : doc.select("div.novel-detail-item")) {
            String header = element.select("div.novel-detail-header").text().trim();
            String value = element.select("div.novel-detail-body").text().trim();

            if (header.toLowerCase().contains("alternative")) {
                novel.alternateNames = Arrays.asList(value.split("\n"));
            } else if (header.toLowerCase().contains("author")) {
                novel.authors = Arrays.asList(value.split("\n"));
            } else if (header.toLowerCase().contains("genre")) {
                novel.genres = Arrays.asList(value.split("\n"));
            } else if (header.toLowerCase().contains("rating")) {
                novel.rating = NumberUtils.toFloat(value);
            } else if (header.toLowerCase().contains("description")) {
                novel.summary = value;
            } else if (header.toLowerCase().contains("status")) {
                novel.status = value;
            } else if (header.toLowerCase().contains("year")) {
                novel.year = NumberUtils.toInt(value);
            }
        }

        return novel;
    }

    @Override
    public List<ChapterItem> chapters(String url) throws IOException {
        List<ChapterItem> items = new ArrayList<>();
        Element doc = Jsoup.connect(url).headers(HEADERS).userAgent(USER_AGENT).get().body();

        Elements main = doc.select("div.tab-content");
        for (Element element : main.select("a")) {
            ChapterItem item = new ChapterItem();

            item.name = element.text().trim();
            item.id = NumberUtils.toFloat(item.name);
            item.url = element.attr("href").trim();
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(String url) throws IOException {
        Chapter chapter = new Chapter();
        chapter.content = "";
        Element doc = Jsoup.connect(url).headers(HEADERS).userAgent(USER_AGENT).get().body();

        for (Element element : doc.select("div#chapterhidden > p")) {
            String text = element.text().trim();
            chapter.content = chapter.content.concat("\n\n").concat(text);
        }
        return chapter;
    }
}
