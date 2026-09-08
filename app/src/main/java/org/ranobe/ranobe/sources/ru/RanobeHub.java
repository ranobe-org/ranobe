package org.ranobe.ranobe.sources.ru;

import android.util.Log;

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
    private final String baseUrl = "https://ranobehub.org";
    private final int sourceId = 5;

    public final HashMap<String, String> HEADERS = new HashMap<String, String>() {{
        put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/151.0.0.0 Safari/537.36");
        put("Cache-Control", "public max-age=604800");
        put("host", "ranobehub.org");
        put("referer", "https://ranobehub.org/");
    }};

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "Ranobehub — ранобэ на русском онлайн";
        source.lang = Lang.ru;
        source.dev = "ap-atul";
        source.logo = "https://ranobehub.org/icon.svg";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        List<Novel> items = new ArrayList<>();
        String web = baseUrl.concat("/popular?page=").concat(String.valueOf(page));
        Element doc = Jsoup.parse(HttpClient.GET(web, HEADERS));

        for (Element element : doc.select("ol.popular-list > li")) {
            Log.d("DEBUG", element.select("span.popular-rank").text());
            String url = element.select("a.popular-list-cover").attr("href");
            String full = baseUrl.concat(url);

            if (!full.isEmpty()) {
                Novel item = new Novel(full);
                item.sourceId = sourceId;
                item.name = element.select("h3").text().trim();
                item.cover = baseUrl.concat(element.select("img").attr("src").trim());
                item.status = element.select("span.popular-list-status").text().trim();
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws IOException {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, HEADERS));

        novel.sourceId = sourceId;
        novel.name = doc.select("h1.book-title-cyrillic").text().trim();
        novel.alternateNames = Arrays.asList(doc.select("p.book-original-title").text().trim().split(","));
        novel.cover = baseUrl.concat(doc.select("img.book-cover-image").attr("src").trim());
        novel.summary = doc.select("p.book-hero-summary").text().trim();

        List<String> authors = new ArrayList<>();
        for (Element element : doc.select("div.book-author-byline > div > a")) {
            authors.add(element.select("strong").text().trim());
        }
        novel.authors = authors;
        novel.year = NumberUtils.toInt(doc.select("a.book-kicker-year").text().trim());

        List<String> genres = new ArrayList<>();
        for (Element element : doc.select("nav[aria-label=Жанры произведения] > a")) {
            genres.add(element.text().trim());
        }
        novel.genres = genres;
        novel.status = doc.select("a.book-kicker-status").text().trim();
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
        String baseApiUrl = baseUrl.concat("/api/books/").concat(getNovelId(novel.url)).concat("/chapters");

        // Start with offset 0 or no offset query param
        Integer nextOffset = 0;

        while (nextOffset != null) {
            // Construct URL with offset pagination query param
            String web = baseApiUrl.concat("?offset=").concat(String.valueOf(nextOffset));
            String json = HttpClient.GET(web, HEADERS);

            JSONObject response = new JSONObject(json);
            JSONArray chaps = response.optJSONArray("items");

            if (chaps == null || chaps.length() == 0) {
                break;
            }

            for (int i = 0; i < chaps.length(); i++) {
                JSONObject chapter = chaps.getJSONObject(i);
                Chapter item = new Chapter(novel.url);

                // Appends "chapter" path properly (e.g., ensuring a separating slash if needed)
                item.url = novel.url.concat("/chapter/").concat(chapter.getString("id"));
                item.name = chapter.getString("title");
                item.id = items.size() + 1;
                item.updated = SourceUtils.parseIsoDate(chapter.getString("publishedAt"));

                items.add(item);
            }

            // Extract nextOffset to continue loop, returns null if missing or explicitly null in JSON
            if (response.has("nextOffset") && !response.isNull("nextOffset")) {
                nextOffset = response.getInt("nextOffset");
            } else {
                nextOffset = null;
            }
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws IOException {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, HEADERS));
        chapter.content = "";

        for (Element element : doc.select("div.reader-content")) {
            element.select("p").append("::");
            chapter.content = SourceUtils.cleanContent(
                    element.text().replace("::", "\n\n\n").trim()
            );
        }

        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (page > 1) return new ArrayList<>();

        List<Novel> items = new ArrayList<>();
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/api/search?q=", keyword, "&rh_client=modern");
            String json = HttpClient.GET(web, HEADERS);
            JSONObject response = new JSONObject(json);
            JSONArray books = response.getJSONArray("books");

            for (int i = 0; i < books.length(); i++) {
                JSONObject novel = books.getJSONObject(i);

                String id = novel.getString("id").concat("-").concat(novel.getString("slug"));
                String url = baseUrl.concat("/ranobe/").concat(id);

                Novel item = new Novel(url);
                item.sourceId = sourceId;
                item.name = novel.getString("title");
                item.cover = baseUrl.concat(novel.getString("posterUrl"));
                items.add(item);
            }
        }
        return items;
    }
}
