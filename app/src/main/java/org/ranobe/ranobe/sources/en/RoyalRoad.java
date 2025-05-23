package org.ranobe.ranobe.sources.en;

import android.util.Log;

import org.json.JSONArray;
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
import org.ranobe.ranobe.util.SourceUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoyalRoad implements Source {

    private final String baseUrl = "https://royalroad.com";
    private final int sourceId = 20;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "Royal Road";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://www.royalroad.com/icons/apple-icon-180x180.png";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = baseUrl + "/fictions/active-popular?page=" + page;
        return parse(HttpClient.GET(web, new HashMap<>()), false);
    }

    private List<Novel> parse(String body, boolean fromSearch) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select(fromSearch ? "div.search-container" : "div#result").first();

        if (doc == null) return items;

        for (Element element : doc.select("div.fiction-list-item")) {
            String url = element.select("h2.fiction-title > a").attr("href").trim();

            if (!url.isEmpty()) {
                Novel item = new Novel(baseUrl + url);
                item.sourceId = sourceId;
                item.name = element.select("h2.fiction-title > a").text().trim();
                item.cover = element.select("img[data-type=\"cover\"]").attr("src");
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = sourceId;
        novel.name = doc.select("h1").first().text().trim();
        novel.cover = doc.select("img.thumbnail").attr("src").trim();

        doc.select("div.description").select("p").append("::");
        novel.summary = doc.select("div.description").text().replaceAll("::", "\n\n").trim();
        novel.rating = NumberUtils.toFloat(doc.select("span[data-original-title='Overall Score']").attr("data-content").trim().split("/")[0].trim());

        String author = doc.select("h4").first().text().trim().replace("by", "").trim();
        novel.authors = Collections.singletonList(author);

        List<String> genres = new ArrayList<>();
        for (Element e : doc.select("span.tags > a")) {
            String text = e.text().trim();
            if (text.equals("ONGOING")) {
                novel.status = "Ongoing";
                continue;
            } else if (text.equals("COMPLETED")) {
                novel.status = "Completed";
                continue;
            }
            genres.add(e.text().trim());
        }

        novel.genres = genres;

        return novel;
    }

    private String getNovelId(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));
        Elements scriptElements = doc.getElementsByTag("script");

        String array = null;
        for (Element element : scriptElements) {
            if (element.data().contains("window.chapters")) {
                // find the line which contains 'window.chapters = <...>;'
                Pattern pattern = Pattern.compile(".*window\\.chapters = ([^;]*);");
                Matcher matcher = pattern.matcher(element.data());
                // we only expect a single match here so there's no need to loop through the matcher's groups
                if (matcher.find()) {
                    array = matcher.group(1);
                    break;
                }
                break;
            }
        }

        JSONArray chapts = new JSONArray(array);
        for (int i = 0; i < chapts.length(); i++) {
            JSONObject o = chapts.getJSONObject(i);

            Chapter item = new Chapter();
            item.name = o.getString("title");
            item.url = baseUrl + o.getString("url");
            item.id = i + 1;
            item.novelUrl = novel.url;
            item.updated = o.getString("date").split("T")[0];
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));

        chapter.content = "";
        doc.select("div.chapter-content").select("p").append("::");
        chapter.content = SourceUtils.cleanContent(
                doc.select("div.chapter-content").text().replaceAll("::", "\n\n\n").trim()
        );

        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws IOException {
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/fictions/search?title=", keyword, "&page=", String.valueOf(page));
            return parse(HttpClient.GET(web, new HashMap<>()), true);
        }
        return new ArrayList<>();
    }
}
