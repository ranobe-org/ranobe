package org.ranobe.core.sources.en;

import android.util.Base64;

import org.json.JSONArray;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Neovel implements Source {
    private static final String BASE_URL = "https://neovel.io";
    private static final int SOURCE_ID = 14;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = BASE_URL;
        source.name = "Neovel";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://neovel.io/favicon-32x32.png";
        source.isActive = false;
        return source;
    }

    private String getCoverImage(String bookId) {
        return String.format(Locale.getDefault(), "https://neovel.io/V2/book/image?bookId=%s&oldApp=false&imageExtension=2", bookId);
    }

    private String getStatus(int completion) {
        switch (completion) {
            case 1:
                return "Ongoing";
            case 3:
                return "Completed";
            default:
                return "N.A.";
        }
    }

    private int getYear(String date) {
        Pattern pattern = Pattern.compile("/\\d{4}/gm");
        Matcher m = pattern.matcher(date);
        if (m.find()) {
            return Integer.parseInt(m.group());
        }
        return 0;
    }

    private List<String> jsonArrayToString(JSONArray arr) throws JSONException {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            values.add(arr.getString(0));
        }
        return values;
    }

    private String encodeKeyword(String keyword) {
        byte[] inputData = keyword.getBytes(); // Default encoding is UTF-8
        return Base64.encodeToString(inputData, Base64.DEFAULT);
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = BASE_URL + "/V2/books/search?language=EN&filter=0&name=&sort=6&page=".concat(String.valueOf(page)) + "&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=9999&completion=5&onlyPremium=false&blacklistedTagIds=&onlyMature=false";
        return parseNovels(web);
    }

    private List<Novel> parseNovels(String web) throws Exception {
        List<Novel> items = new ArrayList<>();
        JSONArray data = new JSONArray(HttpClient.GET(web, new HashMap<>()));

        for (int i = 0; i < data.length(); i++) {
            JSONObject d = data.getJSONObject(i);
            String url = BASE_URL + "/" + d.getString("id");

            Novel item = new Novel(url);
            item.sourceId = SOURCE_ID;
            item.name = d.getString("name");
            item.cover = getCoverImage(d.getString("id"));
            items.add(item);
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        String bookId = novel.url.replace(BASE_URL, "").replace("/", "").trim();
        String url = String.format("https://neovel.io/V1/page/book?bookId=%s&language=EN", bookId);
        JSONObject data = new JSONObject(HttpClient.GET(url, new HashMap<>()));
        JSONObject bookDetails = data.getJSONObject("bookDto");

        novel.sourceId = SOURCE_ID;
        novel.name = bookDetails.getString("name");
        novel.cover = getCoverImage(bookId);
        novel.summary = bookDetails.getString("bookDescription");
        novel.rating = Float.parseFloat(String.valueOf(bookDetails.getDouble("rating")));
        novel.authors = jsonArrayToString(bookDetails.getJSONArray("authors"));
        novel.status = getStatus(bookDetails.getInt("completion"));
        novel.year = getYear(bookDetails.getString("postDate"));

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        String bookId = novel.url.replace(BASE_URL, "").replace("/", "").trim();
        String url = String.format("https://neovel.io/V5/chapters?bookId=%s&language=EN", bookId);
        JSONArray data = new JSONArray(HttpClient.GET(url, new HashMap<>()));

        for (int i = 0; i < data.length(); i++) {
            JSONObject o = data.getJSONObject(i);
            String u = BASE_URL + "/chapter/" + o.getString("chapterId");

            Chapter item = new Chapter(novel.url);
            item.url = u;
            item.name = o.getString("chapterName");
            item.id = Float.parseFloat(o.getString("chapterNumber"));
            item.updated = o.getString("postDate");
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        String chapterId = chapter.url.replace(BASE_URL, "").replace("/chapter/", "").trim();
        String url = String.format("https://neovel.io/V2/chapter/content?chapterId=%s", chapterId);
        JSONObject data = new JSONObject(HttpClient.GET(url, new HashMap<>()));
        Element doc = Jsoup.parse(data.getString("chapterContent"));
        List<String> paras = new ArrayList<>();

        for (String bits : doc.wholeText().split("\n")) {
            String text = bits.trim();
            if (text.length() > 0) {
                paras.add(text);
            }
        }

        chapter.content = String.join("\n\n", paras);
        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        List<Novel> items = new ArrayList<>();

        if (filters.hashKeyword()) {
            String encodedKeyword = encodeKeyword(filters.getKeyword()).trim();
            String web = BASE_URL + "/V2/books/search?language=EN&filter=0&name=" + encodedKeyword + "&sort=6&page=".concat(String.valueOf(page - 1)) + "&onlyOffline=true&genreIds=0&genreCombining=0&tagIds=0&tagCombining=0&minChapterCount=0&maxChapterCount=9999&completion=5&onlyPremium=false&blacklistedTagIds=&onlyMature=false";
            return parseNovels(web);
        }

        return items;
    }
}
