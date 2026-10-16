package org.ranobe.ranobe.sources.en;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Lang;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.network.WebViewCookieJar;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.util.NumberUtils;
import org.ranobe.ranobe.util.SourceUtils;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Webnovel (Qidian International). The whole site sits behind Cloudflare, which the app's
 * {@link org.ranobe.ranobe.network.Cloudflare} interceptor gets through. Only free chapters are
 * listed, locked (paid) ones can't be read without an account.
 */
public class Webnovel implements Source {
    public static final int SOURCE_ID = 22;
    private static final String SITE = "https://www.webnovel.com";

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = SITE;
        source.name = "Webnovel";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        // the site itself is behind Cloudflare, its CDN is not
        source.logo = "https://www.yueimg.com/en/favicon/favicon.d3f6a.ico";
        source.isActive = true;
        source.isNew = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        Document doc = get(SITE + "/stories/novel?orderBy=1&pageIndex=" + page);
        return parseList(doc);
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (!filters.hashKeyword()) return Collections.emptyList();
        // the same form the site's own search box sends: lower case, words joined by +
        String keyword = filters.getKeyword().trim().toLowerCase(Locale.ROOT);
        String query = URLEncoder.encode(keyword, "UTF-8");
        String searchPage = SITE + "/search?keywords=" + query;
        if (page <= 1) {
            // only the result list, the page also carries popular and recommended books
            Element results = get(searchPage).selectFirst(".j_list_container ul.j_result_wrap");
            return results == null ? new ArrayList<>() : parseList(results);
        }
        return moreResults(searchPage, query, keyword, page);
    }

    // further pages come from the api the search page calls while scrolling
    // the api never runs dry, past the real matches it pads with unrelated popular books, so only
    // books that actually match are kept and a page without any ends the search
    private List<Novel> moreResults(String searchPage, String query, String keyword, int page) throws Exception {
        String token = WebViewCookieJar.get(SITE, "_csrfToken");
        if (token == null) {
            // the search page sets the token cookie
            get(searchPage);
            token = WebViewCookieJar.get(SITE, "_csrfToken");
        }
        if (token == null) return new ArrayList<>();

        HashMap<String, String> headers = headers();
        headers.put("Referer", searchPage);
        headers.put("X-Requested-With", "XMLHttpRequest");
        String url = SITE + "/go/pcm/search/result?_csrfToken=" + URLEncoder.encode(token, "UTF-8")
                + "&keywords=" + query + "&pageIndex=" + page + "&type=novel";
        JSONObject response = new JSONObject(HttpClient.GET(url, headers));
        JSONObject data = response.optJSONObject("data");
        JSONObject books = data == null ? null : data.optJSONObject("bookInfo");
        JSONArray items = books == null ? null : books.optJSONArray("bookItems");
        List<Novel> novels = new ArrayList<>();
        if (items == null) return novels;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            String id = item.optString("bookId", "");
            if (id.isEmpty()) continue;
            Novel novel = new Novel(SITE + "/book/" + id, SOURCE_ID);
            novel.name = item.optString("bookName", "").trim();
            if (!matches(keyword, novel.name, item.optString("description", ""))) continue;
            String imageId = firstNonEmpty(item.optString("coverUpdateTime"), item.optString("bookCoverId"));
            novel.cover = "https://book-pic.webnovel.com/bookcover/" + id + "?imageMogr2/thumbnail/300x"
                    + (imageId.isEmpty() ? "" : "&imageId=" + imageId);
            novels.add(novel);
        }
        return novels;
    }

    // book tiles look the same on the category, ranking and search pages
    private List<Novel> parseList(Element doc) {
        List<Novel> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Element link : doc.select("a.g_thumb[href*=/book/], a[href*=/book/][title]:has(img)")) {
            String url = bookUrl(link.attr("href"));
            if (url == null || !seen.add(url)) continue;
            Novel novel = new Novel(url, SOURCE_ID);
            Element img = link.selectFirst("img");
            novel.name = firstNonEmpty(link.attr("title"), img != null ? img.attr("alt") : null, link.text());
            novel.cover = image(img);
            if (novel.name.isEmpty()) continue;
            items.add(novel);
        }
        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Document doc = get(novel.url);
        novel.sourceId = SOURCE_ID;

        Element info = firstOf(doc, ".det-info", ".g_col_8", "body");
        String name = text(info.selectFirst("h1"));
        if (!name.isEmpty()) novel.name = name.replaceAll("\\s+(WN|Novel)$", "");
        String cover = image(doc.selectFirst(".det-info .g_thumb img, .g_thumb img"));
        if (!cover.isEmpty()) novel.cover = cover;

        Element synopsis = firstOf(doc, ".j_synopsis", ".det-abt .g_txt_over", ".det-abt");
        if (synopsis != null) {
            List<String> paragraphs = synopsis.select("p").eachText();
            novel.summary = paragraphs.isEmpty() ? synopsis.text().trim() : String.join("\n\n", paragraphs);
        }

        List<String> authors = new ArrayList<>();
        Element og = doc.selectFirst("meta[property=og:author]");
        String author = og == null ? "" : og.attr("content").trim();
        if (!author.isEmpty()) authors.add(author);
        novel.authors = authors;

        List<String> genres = new ArrayList<>();
        for (Element a : doc.select(".det-hd-detail a[href*=/stories/], .m-tags a, .det-tag a")) {
            String genre = a.text().replace("#", "").trim();
            if (!genre.isEmpty() && !genres.contains(genre)) genres.add(genre);
        }
        novel.genres = genres;

        String header = doc.select(".det-hd-detail").text();
        if (header.contains("Completed")) novel.status = "Completed";
        else if (header.contains("Ongoing")) novel.status = "Ongoing";
        novel.rating = NumberUtils.toFloat(text(doc.selectFirst(".g_star_num small, ._score strong, .score")));

        novel.recommendations = alsoLike(doc, novel.url);
        return novel;
    }

    // letters from any script but latin mark another language's edition
    private static final java.util.regex.Pattern LATIN_ONLY =
            java.util.regex.Pattern.compile("[\\p{IsLatin}\\p{IsCommon}\\p{IsInherited}]*\\p{IsLatin}[\\p{IsLatin}\\p{IsCommon}\\p{IsInherited}]*");

    // the "You May Also Like" row, it mixes in other languages' editions, only latin titles are kept
    private List<Novel> alsoLike(Document doc, String selfUrl) {
        List<Novel> items = new ArrayList<>();
        Element row = doc.selectFirst("ul.j_books_you_also_like");
        if (row == null) return items;
        for (Novel novel : parseList(row)) {
            if (!novel.url.equals(selfUrl) && LATIN_ONLY.matcher(novel.name).matches()) items.add(novel);
        }
        return items;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        Document doc = get(novel.url.replaceAll("/$", "") + "/catalog");
        List<Chapter> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Element li : doc.select(".volume-item li, .j_catalog_list li, .content-list li")) {
            Element a = li.selectFirst("a[href]");
            if (a == null) continue;
            // a lock icon (<use xlink:href="#i-lock">) marks a paid chapter
            if (li.html().contains("#i-lock")) continue;
            String url = absolute(a.attr("href"));
            if (!seen.add(url)) continue;
            String title = firstNonEmpty(a.attr("title"), text(a.selectFirst("strong")), a.text());
            String number = text(li.selectFirst("i._num"));
            Chapter chapter = new Chapter(novel.url);
            chapter.url = url;
            if (title.toLowerCase().startsWith("chapter")) {
                chapter.name = title;
            } else {
                chapter.name = number.isEmpty() ? title : "Chapter " + number + ": " + title;
            }
            chapter.updated = text(li.selectFirst("small"));
            chapter.id = number.isEmpty() ? items.size() + 1 : NumberUtils.toFloat(number);
            items.add(chapter);
        }
        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Document doc = get(chapter.url);
        Element body = firstOf(doc, ".cha-words", ".cha-content", ".chapter_content");
        if (body == null) throw new IllegalStateException("This chapter is locked or couldn't be read");
        body.select(".para-comment, .j_comment, script, style, pirate").remove();
        chapter.content = SourceUtils.contentFromHtml(body);
        return chapter;
    }

    private static Document get(String url) throws Exception {
        String html = HttpClient.GET(url, headers());
        return Jsoup.parse(html, SITE);
    }

    // any query word in the title, or every query word in the description
    static boolean matches(String keyword, String title, String description) {
        String name = title.toLowerCase(Locale.ROOT);
        String about = description == null ? "" : description.toLowerCase(Locale.ROOT);
        boolean all = true;
        boolean any = false;
        for (String word : keyword.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            if (word.length() < 2) continue;
            if (name.contains(word)) any = true;
            if (!about.contains(word) && !name.contains(word)) all = false;
        }
        return any || all;
    }

    // the mobile site fills its lists in with javascript, the desktop one renders them
    private static HashMap<String, String> headers() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", HttpClient.desktopUserAgent());
        headers.put("Accept-Language", "en-US,en;q=0.9");
        return headers;
    }

    // book urls are /book/{slug}_{id} or /book/{id}, chapters add another segment
    private static String bookUrl(String href) {
        if (href == null || href.isEmpty()) return null;
        String url = absolute(href).split("[?#]")[0];
        int at = url.indexOf("/book/");
        if (at < 0) return null;
        String rest = url.substring(at + "/book/".length());
        int slash = rest.indexOf('/');
        if (slash >= 0) rest = rest.substring(0, slash);
        return rest.isEmpty() ? null : SITE + "/book/" + rest;
    }

    private static String absolute(String href) {
        String url = href.trim();
        if (url.startsWith("//")) return "https:" + url;
        if (url.startsWith("/")) return SITE + url;
        return url;
    }

    private static String image(Element img) {
        if (img == null) return "";
        String src = firstNonEmpty(img.attr("data-original"), img.attr("data-src"), img.attr("src"));
        if (src.startsWith("data:")) return "";
        return src.startsWith("//") ? "https:" + src : src;
    }

    private static Element firstOf(Element root, String... selectors) {
        for (String selector : selectors) {
            Element found = root.selectFirst(selector);
            if (found != null) return found;
        }
        return null;
    }

    private static String text(Element element) {
        return element == null ? "" : element.text().trim();
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }
}
