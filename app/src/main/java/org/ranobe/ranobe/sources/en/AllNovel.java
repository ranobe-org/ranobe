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
import org.ranobe.ranobe.util.SourceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AllNovel implements Source {

    // allnovel.org now redirects every path to the novelfull.com homepage
    private static final String LEGACY_BASE_URL = "https://allnovel.org";
    private final String baseUrl = "https://novelfull.com";
    private final int sourceId = 7;

    private static String splitDialoguesFromNarration(String text) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("\"[^\"]*\"|[^\"']+");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String part = matcher.group().trim();
            if (part.startsWith("\"") && part.endsWith("\"")) {
                // Dialogue: put on its own line
                result.append(part).append("\n");
            } else {
                // Narration: optionally add spacing every 5 sentences
                result.append(addSpacingEveryNSentences(part, 5, 1)).append("\n");
            }
        }

        return result.toString().trim();
    }

    private static String addSpacingEveryNSentences(String text, int n, int lineBreaks) {
        String[] sentences = text.split("(?<=[.!?])\\s+"); // Split on sentence-ending punctuation
        StringBuilder result = new StringBuilder();
        int count = 0;

        for (String sentence : sentences) {
            result.append(sentence.trim()).append(" ");
            count++;
            if (count % n == 0) {
                for (int i = 0; i < lineBreaks; i++) {
                    result.append("\n");
                }
            }
        }

        return result.toString().trim();
    }

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "All Novel";
        source.lang = Lang.eng;
        source.dev = "punpun";
        source.logo = baseUrl + "/web/images/favicon.ico";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = baseUrl + "/latest-release-novel?page=" + page;
        return parse(HttpClient.GET(web, new HashMap<>()));
    }

    private List<Novel> parse(String body) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select("div.col-truyen-main.archive").first();

        if (doc == null) return items;

        for (Element element : doc.select("div.row")) {
            String url = element.select("h3.truyen-title > a").attr("href").trim();

            if (!url.isEmpty()) {
                Novel item = new Novel(baseUrl + url);
                item.sourceId = sourceId;
                item.name = element.select("h3.truyen-title > a").text().trim();
                item.cover = resolveUrl(element.select("img.cover").attr("src").trim());
                items.add(item);
            }
        }

        return items;
    }

    private String resolveUrl(String url) {
        if (url.isEmpty() || url.startsWith("http")) return url;
        return baseUrl + url;
    }

    private String migrateUrl(String url) {
        return url.startsWith(LEGACY_BASE_URL) ? baseUrl + url.substring(LEGACY_BASE_URL.length()) : url;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(migrateUrl(novel.url), new HashMap<>()));
        novel.sourceId = sourceId;
        novel.name = doc.select("div.books h3.title").text().trim();
        novel.cover = resolveUrl(doc.select("div.books img").attr("src").trim());
        novel.summary = doc.select("div.desc-text > p").text().trim();
        novel.rating = NumberUtils.toFloat(doc.select("input#rateVal").attr("value")) / 2;


        // rows are matched by label since optional rows (e.g. "Source:") shift positions
        for (Element element : doc.select("div.info > div")) {
            String label = element.select("h3").text();
            if (label.startsWith("Author")) {
                novel.authors = element.select("a").eachText();
            } else if (label.startsWith("Genre")) {
                novel.genres = element.select("a").eachText();
            } else if (label.startsWith("Status")) {
                novel.status = element.select("a").text().trim();
            }
        }

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        Element novelId = Jsoup.parse(HttpClient.GET(migrateUrl(novel.url), new HashMap<>())); // getNovelId
        String id = novelId.select("div#rating").attr("data-novel-id");

        String base = baseUrl.concat("/ajax-chapter-option?novelId=").concat(id);
        Element doc = Jsoup.parse(HttpClient.GET(base, new HashMap<>()));

        for (Element element : doc.select("select option")) {
            Chapter item = new Chapter(novel.url);

            item.url = baseUrl + element.attr("value").trim();
            item.name = element.text().trim();
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }
        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(migrateUrl(chapter.url), new HashMap<>()));

        Elements paragraphs = doc.select("div.chapter-c").select("p");
        StringBuilder contentBuilder = new StringBuilder();

        for (Element p : paragraphs) {
            String paragraph = p.text().trim();
            if (!paragraph.isEmpty()) {
                // Split paragraph into dialogues and narration
                String formattedParagraph = splitDialoguesFromNarration(paragraph);
                contentBuilder.append(formattedParagraph).append("\n\n"); // Keep paragraph spacing
            }
        }

        chapter.content = contentBuilder.toString().trim();
        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/search?keyword=", keyword, "&page=", String.valueOf(page));
            return parse(HttpClient.GET(web, new HashMap<>()));
        }
        return new ArrayList<>();
    }
}
