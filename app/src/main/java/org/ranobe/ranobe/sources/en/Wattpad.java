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
import org.ranobe.ranobe.util.NumberUtils;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wattpad through its public api (api.wattpad.com), which unlike the website isn't behind
 * Cloudflare. Novels are stored with their website url, www.wattpad.com/story/{id}.
 */
public class Wattpad implements Source {
    public static final int SOURCE_ID = 21;
    private static final String SITE = "https://www.wattpad.com";
    private static final String API = "https://api.wattpad.com";
    private static final int PAGE_SIZE = 20;
    private static final String LIST_FIELDS = "stories(id,title,cover,user(name)),nextUrl";
    private static final Pattern STORY_ID = Pattern.compile("/story/(\\d+)");
    private static final Pattern PART_ID = Pattern.compile("wattpad\\.com/(\\d+)");

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = SITE;
        source.name = "Wattpad";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://img.wattpad.com/useravatar/wattpad.128.jpg";
        source.isActive = true;
        source.isNew = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String url = API + "/v4/stories?filter=hot&limit=" + PAGE_SIZE + "&offset=" + offset(page) + "&fields=" + LIST_FIELDS;
        return parseStories(new JSONObject(get(url)).optJSONArray("stories"));
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        String text;
        if (filters.hasGenre()) {
            // a story tag, the api finds tags written as #tag
            String tag = filters.getGenre().substring(filters.getGenre().lastIndexOf('/') + 1);
            text = "#" + tag;
        } else if (filters.hashKeyword()) {
            text = filters.getKeyword();
        } else {
            return Collections.emptyList();
        }
        String query = URLEncoder.encode(text, "UTF-8");
        String url = API + "/v4/search/stories?query=" + query + "&limit=" + PAGE_SIZE + "&offset=" + offset(page) + "&fields=" + LIST_FIELDS;
        return parseStories(new JSONObject(get(url)).optJSONArray("stories"));
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        String id = storyId(novel.url);
        String fields = "id,title,description,cover,user(name),tags,completed,mature,voteCount,readCount,numParts,createDate";
        JSONObject story = new JSONObject(get(API + "/api/v3/stories/" + id + "?fields=" + fields));

        novel.sourceId = SOURCE_ID;
        novel.name = story.optString("title", novel.name).trim();
        novel.cover = story.optString("cover", novel.cover);
        novel.summary = story.optString("description", "").trim();
        JSONObject user = story.optJSONObject("user");
        if (user != null) novel.authors = Collections.singletonList(user.optString("name"));
        novel.genres = strings(story.optJSONArray("tags"));
        for (String tag : novel.genres) novel.genreLinks.put(tag, SITE + "/stories/" + tag);
        novel.status = story.optBoolean("completed") ? "Completed" : "Ongoing";
        String created = story.optString("createDate", "");
        if (created.length() >= 4) novel.year = NumberUtils.toInt(created.substring(0, 4));
        novel.recommendations = recommended(id);
        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        String id = storyId(novel.url);
        JSONObject story = new JSONObject(get(API + "/api/v3/stories/" + id + "?fields=parts(id,title,createDate,deleted)"));
        JSONArray parts = story.optJSONArray("parts");
        List<Chapter> items = new ArrayList<>();
        if (parts == null) return items;
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.getJSONObject(i);
            if (part.optBoolean("deleted")) continue;
            Chapter chapter = new Chapter(novel.url);
            chapter.url = SITE + "/" + part.getLong("id");
            chapter.name = part.optString("title", "Part " + (i + 1)).trim();
            chapter.updated = org.ranobe.ranobe.util.SourceUtils.parseIsoDay(part.optString("createDate", ""));
            chapter.id = items.size() + 1;
            items.add(chapter);
        }
        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Matcher m = PART_ID.matcher(chapter.url);
        if (!m.find()) throw new IllegalArgumentException("Not a Wattpad part: " + chapter.url);
        // without a page number the whole part comes back at once
        Document doc = Jsoup.parse(get(API + "/apiv2/storytext?id=" + m.group(1)), SITE);
        chapter.content = org.ranobe.ranobe.util.SourceUtils.contentFromHtml(doc.body());
        return chapter;
    }

    private List<Novel> recommended(String id) {
        try {
            JSONArray array = new JSONArray(get(API + "/api/v3/stories/" + id + "/recommended?limit=15"));
            return parseStories(array);
        } catch (Exception e) {
            // nice to have, never worth failing the details over
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Novel> parseStories(JSONArray stories) {
        List<Novel> items = new ArrayList<>();
        if (stories == null) return items;
        for (int i = 0; i < stories.length(); i++) {
            JSONObject story = stories.optJSONObject(i);
            if (story == null || story.optString("id").isEmpty()) continue;
            Novel novel = new Novel(SITE + "/story/" + story.optString("id"), SOURCE_ID);
            novel.name = story.optString("title", "").trim();
            novel.cover = story.optString("cover", "");
            JSONObject user = story.optJSONObject("user");
            if (user != null) novel.authors = Collections.singletonList(user.optString("name"));
            items.add(novel);
        }
        return items;
    }

    private static String storyId(String url) {
        Matcher m = STORY_ID.matcher(url);
        if (!m.find()) throw new IllegalArgumentException("Not a Wattpad story: " + url);
        return m.group(1);
    }

    private static int offset(int page) {
        return Math.max(0, page - 1) * PAGE_SIZE;
    }

    private static List<String> strings(JSONArray array) {
        List<String> list = new ArrayList<>();
        if (array == null) return list;
        for (int i = 0; i < array.length(); i++) list.add(array.optString(i));
        return list;
    }

    private static String get(String url) throws Exception {
        return HttpClient.GET(url, new HashMap<>());
    }
}
