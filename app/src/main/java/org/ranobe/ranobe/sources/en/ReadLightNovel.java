package org.ranobe.ranobe.sources.en;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ReadLightNovel implements Source {
    public final String baseUrl = "https://www.readlightnovel.me";
    public final HashMap<String, String> HEADERS = new HashMap<String, String>() {{
        put("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 6.1; rv:2.2) Gecko/20110201");
        put("Cache-Control", "public max-age=604800");
        put("x-requested-with", "XMLHttpRequest");
    }};

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = 1;
        source.url = baseUrl;
        source.name = "Read Light Novel";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://www.readlightnovel.me/assets/images/logo-new-day.png";
        source.isActive = false;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws IOException {
        String url = baseUrl + "/top-novels/new/" + page;
        String body = HttpClient.GET(url, HEADERS);
        return parse(body);
    }

    public List<Novel> parse(String body) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(body);

        for (Element element : doc.select("div.top-novel-block")) {
            String url = element.select("div.top-novel-header > h2 > a").attr("href").trim();
            if (url.length() > 0) {
                Novel item = new Novel(url);
                item.sourceId = 1;
                item.name = element.select("div.top-novel-header > h2 > a").text().trim();
                item.url = element.select("div.top-novel-header > h2 > a").attr("href").trim();
                item.cover = element.select("div.top-novel-cover > a > img").attr("src").trim();
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws IOException {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, HEADERS));

        novel.sourceId = 1;
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
                novel.genres = Arrays.asList(value.split(" "));
            } else if (header.toLowerCase().contains("rating")) {
                novel.rating = NumberUtils.toFloat(value);
            } else if (header.toLowerCase().contains("description")) {
                String summary = "";
                for (Element ele : element.select("div.novel-detail-body > p")) {
                    summary = summary.concat(ele.text().trim()).concat("\n\n");
                }
                novel.summary = summary;
            } else if (header.toLowerCase().contains("status")) {
                novel.status = value;
            } else if (header.toLowerCase().contains("year")) {
                novel.year = NumberUtils.toInt(value);
            }
        }

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws IOException {
        List<Chapter> items = new ArrayList<>();
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, HEADERS));

        Elements main = doc.select("div.tab-content");
        for (Element element : main.select("a")) {
            Chapter item = new Chapter(novel.url);

            item.name = element.text().trim();
            item.id = NumberUtils.toFloat(item.name);
            item.url = element.attr("href").trim();
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws IOException {
        chapter.content = "";
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, HEADERS));

        for (Element element : doc.select("div#chapterhidden > p")) {
            String text = element.text().trim();
            chapter.content = chapter.content.concat("\n\n").concat(text);
        }
        return chapter;
    }

    @Override
    // returns only 50 results
    public List<Novel> search(Filter filters, int page) throws IOException {
        String url = "https://www.readlightnovel.me/detailed-search-210922";
        if (page > 1) return new ArrayList<>();
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            HashMap<String, String> form = new HashMap<>();
            form.put("keyword", keyword);
            form.put("search", "1");
            String body = HttpClient.POST(url, HEADERS, form);
            return parse(body);
        }
        return new ArrayList<>();
    }
}
