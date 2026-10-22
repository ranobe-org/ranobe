package org.ranobe.ranobe.sources.en;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Lang;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.sources.Genres;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.util.SourceUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * FableSpace is an open platform for original web fiction, with images inside chapters.
 * Everything comes from its JSON api, the site itself is a client-rendered app.
 */
public class FableSpace implements Source {

    private final String baseUrl = "https://fablespace.space";
    private final String api = "https://api.fablespace.space/api/v1";
    private final String storyPath = "/story/";
    private final int sourceId = 23;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "FableSpace";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = baseUrl + "/favicon.ico";
        source.isActive = true;
        source.isNew = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        return parse(get(api + "/stories?sortBy=recentlyUpdated&limit=20&page=" + page));
    }

    private List<Novel> parse(String body) throws Exception {
        List<Novel> items = new ArrayList<>();
        JSONArray array = new JSONObject(body).optJSONArray("data");
        if (array == null) return items;

        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            String slug = o.optString("slug", "");
            if (slug.isEmpty()) continue;

            Novel item = new Novel(baseUrl + storyPath + slug, sourceId);
            item.name = o.optString("title", "").trim();
            item.cover = o.isNull("coverImage") ? "" : o.optString("coverImage", "");
            items.add(item);
        }
        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        JSONObject o = new JSONObject(get(api + "/stories/slug/" + slug(novel.url))).getJSONObject("data");

        novel.sourceId = sourceId;
        novel.name = o.optString("title", "").trim();
        novel.cover = o.isNull("coverImage") ? "" : o.optString("coverImage", "");
        novel.summary = o.optString("description", "").trim();
        String status = o.optString("status", "");
        novel.status = status.isEmpty() ? "" : Character.toUpperCase(status.charAt(0)) + status.substring(1);
        novel.lastKnownChapterCount = o.optInt("chapterCount", 0);

        JSONObject author = o.optJSONObject("author");
        if (author != null) {
            String name = author.optString("name", "").trim();
            if (name.isEmpty()) name = author.optString("username", "").trim();
            if (!name.isEmpty()) novel.authors = Collections.singletonList(name);
        }

        // FableSpace's own "genre" is a single category, what readers browse by are the tags,
        // so those are the genres here and the category only stands in for a story with none
        List<String> genres = new ArrayList<>();
        JSONArray tags = o.optJSONArray("tags");
        for (int i = 0; tags != null && i < tags.length(); i++) {
            JSONObject tag = tags.getJSONObject(i);
            String name = tag.optString("name", "").trim();
            if (name.isEmpty() || tag.optString("slug", "").isEmpty()) continue;
            genres.add(name);
            novel.genreLinks.put(name, api + "/stories?tags=" + tag.optString("slug"));
        }
        JSONObject genre = o.optJSONObject("genre");
        if (genres.isEmpty() && genre != null && !genre.optString("slug", "").isEmpty()) {
            genres.add(genre.optString("name"));
            novel.genreLinks.put(genre.optString("name"), api + "/stories?genre=" + genre.optString("slug"));
        }
        novel.genres = genres;
        novel.recommendations = similar(slug(novel.url));

        return novel;
    }

    // same shape as the story list; failing here (e.g. rate limit) shouldn't fail the details
    private List<Novel> similar(String slug) {
        try {
            return parse(get(api + "/recommendations/slug/" + slug + "?limit=10"));
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        JSONArray array = new JSONObject(get(api + "/stories/slug/" + slug(novel.url) + "/chapters")).getJSONArray("data");

        List<Chapter> items = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            if (!o.optString("status", "published").equals("published")) continue;

            Chapter item = new Chapter();
            item.name = o.optString("title", "").trim();
            // the id after '#' is what the content endpoint needs, the rest is the page a browser opens
            item.url = novel.url + "/chapter/" + o.optInt("number", i + 1) + "#" + o.getString("id");
            item.id = o.optInt("number", i + 1);
            item.novelUrl = novel.url;
            item.updated = o.optString("createdAt", "").split("T")[0];
            items.add(item);
        }
        Collections.sort(items, (a, b) -> Float.compare(a.id, b.id));
        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        String id = chapter.url.substring(chapter.url.indexOf('#') + 1);
        JSONObject o = new JSONObject(get(api + "/chapters/" + id)).getJSONObject("data");

        Document doc = Jsoup.parse(o.optString("content", ""), baseUrl);
        // authors can paste html with 1x1 tracking pixels, they'd show up as broken images
        doc.select("img[width=1], img[height=1]").remove();
        String content = SourceUtils.contentFromHtml(doc.body());
        String notes = o.optString("authorNotes", "").trim();
        if (!notes.isEmpty()) content = content + "\n\n" + Jsoup.parse(notes).text();
        chapter.content = content;

        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws IOException {
        try {
            if (filters.hasGenre()) {
                // links saved from an earlier version used ?tag=, which the api ignores
                String link = filters.getGenre().replace("/stories?tag=", "/stories?tags=");
                return parse(get(Genres.queryPage(link + "&limit=20", "page", page)));
            }
            if (filters.hashKeyword()) {
                String keyword = URLEncoder.encode(filters.getKeyword(), "UTF-8");
                return parse(get(api + "/stories?limit=20&search=" + keyword + "&page=" + page));
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
        return new ArrayList<>();
    }

    // the api answers 200 with success=false when it refuses, it only allows ~30 requests per
    // 15 minutes per ip, so say so instead of showing an empty list
    private String get(String url) throws IOException {
        String body = HttpClient.GET(url, new HashMap<>());
        try {
            JSONObject json = new JSONObject(body);
            if (!json.optBoolean("success", true)) {
                JSONObject error = json.optJSONObject("error");
                throw new IOException(error != null ? error.optString("message", "Request failed") : "Request failed");
            }
        } catch (org.json.JSONException e) {
            throw new IOException("Unexpected response from FableSpace");
        }
        return body;
    }

    // ".../story/some-slug" -> "some-slug"
    private String slug(String url) {
        String path = url.substring(url.indexOf(storyPath) + storyPath.length());
        int end = path.indexOf('/');
        return end < 0 ? path : path.substring(0, end);
    }
}
