package org.ranobe.ranobe.sources.ru;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
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
import java.util.HashMap;
import java.util.List;

public class RanobeHub implements Source {
    private final String baseUrl = "https://ranobehub.org/";
    private final int sourceId = 5;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "Ranobehub — ранобэ на русском онлайн";
        source.lang = Lang.ru;
        source.dev = "ap-atul";
        source.logo = "https://ranobehub.org/favicon.png";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = baseUrl.concat("api/search?page=").concat(String.valueOf(page)).concat("&take=40");
        List<Novel> items = new ArrayList<>();
        String json = HttpClient.GET(web, new HashMap<>());

        JSONArray novels = new JSONObject(json).getJSONArray("resource");
        for (int i = 0; i < novels.length(); i++) {
            JSONObject novel = novels.getJSONObject(i);

            String url = novel.getString("url");
            Novel item = new Novel(url);
            item.sourceId = sourceId;
            item.name = novel.getJSONObject("names").getString("rus");
            item.cover = novel.getJSONObject("poster").getString("medium")
                    .replace("medium", "big");
            items.add(item);
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws IOException {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = sourceId;
        novel.name = doc.select("h1.ui.huge.header").text().trim();
        novel.alternateNames = Arrays.asList(doc.select("h2.ui.header.medium").text().trim().split(","));
        novel.cover = doc.select("img.__posterbox").attr("data-src").replace("medium", "big").trim();
        doc.select("div.book-description").select("p").append("::");
        novel.summary = doc.select("div.book-description").text().replaceAll("::", "\n\n").trim();

        List<String> authors = new ArrayList<>();
        for (Element element : doc.select("book-author")) {
            authors.add(element.select("a").text().trim());
        }
        novel.authors = authors;
        novel.year = NumberUtils.toInt(doc.select("div.book-meta-value").select("a").text().trim());

        List<String> genres = new ArrayList<>();
        for (Element element : doc.select("div.book-meta-value.book-tags > a")) {
            genres.add(element.text().trim());
        }
        novel.genres = genres;

        for (Element element : doc.select("div.book-meta-row")) {
            String header = element.select("div.book-meta-key").text().trim();
            if (header.contains("перевода")) {
                novel.status = element.select("div.book-meta-value").select("a").text().trim();
            }
        }
        return novel;
    }

    private String getNovelId(String url) {
        String[] parts = url.split("/");
        String last = parts[parts.length - 1];
        return String.valueOf(NumberUtils.toInt(last));
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        String web = baseUrl.concat("api/ranobe/").concat(getNovelId(novel.url)).concat("/contents");
        String json = HttpClient.GET(web, new HashMap<>());

        JSONArray vols = new JSONObject(json).getJSONArray("volumes");

        for (int i = 0; i < vols.length(); i++) {
            JSONArray chaps = vols.getJSONObject(i).getJSONArray("chapters");

            for (int j = 0; j < chaps.length(); j++) {
                JSONObject chapter = chaps.getJSONObject(j);

                Chapter item = new Chapter(novel.url);
                item.url = chapter.getString("url");
                item.name = chapter.getString("name");
                item.id = items.size() + 1;
                item.updated = SourceUtils.getDate(chapter.getInt("changed_at"));
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws IOException {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));
        chapter.content = "";

        for (Element element : doc.select("div.ui.text.container")) {
            if (element.hasAttr("data-container")) {
                element.select("p").append("::");
                chapter.content = SourceUtils.cleanContent(
                        element.text().replaceAll("::", "\n\n").trim()
                );
            }
        }

        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (page > 1) return new ArrayList<>();

        List<Novel> items = new ArrayList<>();
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "api/fulltext/global?query=", keyword, "&take=100");
            String json = HttpClient.GET(web, new HashMap<>());
            JSONArray response = new JSONArray(json);

            for (int i = 0; i < response.length(); i++) {
                if (response.getJSONObject(i).getJSONObject("meta").getString("key").equals("ranobe")) {
                    JSONArray novels = response.getJSONObject(i).getJSONArray("data");
                    for (int j = 0; j < novels.length(); j++) {
                        JSONObject novel = novels.getJSONObject(j);

                        Novel item = new Novel(novel.getString("url"));
                        item.sourceId = sourceId;
                        item.name = novel.getJSONObject("names").getString("rus");
                        item.cover = novel.getString("image").replace("small", "big");
                        items.add(item);
                    }
                    break;
                }
            }
        }
        return items;
    }
}
