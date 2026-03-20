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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AllNovel implements Source {

    private final String baseUrl = "https://allnovel.org";
    private final int sourceId = 7;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "All Novel";
        source.lang = Lang.eng;
        source.dev = "punpun";
        source.logo = "https://allnovel.org/uploads/thumbs/logo23232-21abb9ad59-98b3a84b69aa4c92e8b001282e110775.png";
        source.isActive = true;
        return source;
    }


    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = baseUrl + "/latest-release-novel?page=" + page;
        return parse(HttpClient.GET(web, new HashMap<>()));
    }


    private List<Novel> parse(String body) throws IOException {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select("div.col-truyen-main.archive").first();

        if (doc == null) return items;

        for (Element element : doc.select("div.row")) {
            String url = element.select("h3.truyen-title > a").attr("href").trim();

            if (!url.isEmpty()) {
                Novel item = new Novel(baseUrl+url);
                item.sourceId = sourceId;
                item.name = element.select("h3.truyen-title > a").text().trim();
                Element img = Jsoup.parse(HttpClient.GET(baseUrl + url, new HashMap<>()));
                item.cover = baseUrl + img.select("div.books img").attr("src");

                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));
        novel.sourceId = sourceId;
        novel.name = doc.select("div.books h3.title").text().trim();
        novel.cover = baseUrl + doc.select("div.books img").attr("src").trim();
        novel.summary = doc.select("div.desc-text > p").text().trim();
        novel.rating = NumberUtils.toFloat(doc.select("input#rateVal").attr("value")) / 2;


        for (Element element : doc.select("div.info")) {

            novel.authors = Arrays.asList(element.select("div:eq(0) > a").text().split(","));
            List<String> genres = new ArrayList<>();
            for (Element a : element.select("div:eq(2) > a")) genres.add(a.text());
            novel.genres = genres;
            novel.status = element.select("div:eq(4) > a").text();

        }

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        Element novelId = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>())); // getNovelId
        String id = novelId.select("div#rating").attr("data-novel-id");

        String base = baseUrl.concat("/ajax-chapter-option?novelId=").concat(id);
        Element doc = Jsoup.parse(HttpClient.GET(base, new HashMap<>()));

        for (Element element : doc.select("select option")) {
            Chapter item = new Chapter(novel.url);

            item.url = baseUrl+element.attr("value").trim();
            item.name = element.text().trim();
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }
        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));

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
}
