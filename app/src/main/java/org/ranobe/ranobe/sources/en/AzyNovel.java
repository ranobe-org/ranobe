package org.ranobe.ranobe.sources.en;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Lang;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.sources.Source;
import org.ranobe.ranobe.util.NumberUtils;
import org.ranobe.ranobe.util.SourceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class AzyNovel implements Source {

    private final String baseUrl = "https://www.azynovel.com";
    private final int sourceId = 8;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "AzyNovel";
        source.lang = Lang.eng;
        source.dev = "punpun";
        source.logo = "https://www.azynovel.com/img/azynovel_icon_64.png";
        return source;
    }

    @Override
    public List<NovelItem> novels(int page) throws Exception {
        String web = baseUrl + "/popular-novels?page=" + page;
        return parse(HttpClient.GET(web, new HashMap<>()));
    }

    private List<NovelItem> parse(String body) {
        List<NovelItem> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select("div.columns.is-multiline").first();
        if (doc == null) return items;

        for (Element element : doc.select("a.box.is-shadowless")) {
            String url = element.attr("href").trim();

            if (url.length() > 0) {
                NovelItem item = new NovelItem(url);
                item.sourceId = sourceId;
                item.name = element.select("div.content > p.gtitle").attr("title").trim();
                item.cover = element.select("img.athumbnail").attr("data-src");


                items.add(item);
            }

        }

        return items;
    }

    @Override
    public Novel details(String url) throws Exception {
        Novel novel = new Novel(url);
        Element doc = Jsoup.parse(HttpClient.GET(baseUrl + url, new HashMap<>()));
        novel.sourceId = sourceId;
        novel.name = doc.select("article.media div.media-content h1:eq(0)").text().trim();
        novel.cover = doc.select("div.media-left img").attr("data-src").trim();
        novel.summary = doc.select("div.content > div:eq(1)").text().trim();
        novel.rating = NumberUtils.toFloat("9") / 2;


        novel.authors = Arrays.asList(doc.select("article.media div.media-content p:eq(1) a").text().split(","));
        List<String> genres = new ArrayList<>();
        for (Element a : doc.select("article.media div.media-content p:eq(3) a"))
            genres.add(a.text());
        novel.genres = genres;
        novel.status = "unknown";


        return novel;
    }

    @Override
    public List<ChapterItem> chapters(String url) throws Exception {
        List<ChapterItem> items = new ArrayList<>();
        String base = baseUrl.concat(url);
        Element doc = Jsoup.parse(HttpClient.GET(base, new HashMap<>()));

        for (Element element : doc.select("div.chapter-list a")) {
            ChapterItem item = new ChapterItem(url);

            item.url = element.attr("href").trim();
            item.name = element.text().trim();
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }
        return items;
    }

    @Override
    public Chapter chapter(String novelUrl, String chapterUrl) throws Exception {
        Chapter chapter = new Chapter(novelUrl);
        Element doc = Jsoup.parse(HttpClient.GET(baseUrl + chapterUrl, new HashMap<>()));

        chapter.url = baseUrl + chapterUrl;
        chapter.content = "";

        doc.select("div.columns div div:eq(4)").select("p").append("::");
        chapter.content = SourceUtils.cleanContent(
                doc.select("div.columns div div:eq(4)").text().replaceAll("::", "\n\n").trim()
        );

        return chapter;
    }

    @Override
    public List<NovelItem> search(Filter filters, int page) throws Exception {
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/search?q=", keyword, "&page=", String.valueOf(page));
            return parse(HttpClient.GET(web, new HashMap<>()));
        }
        return new ArrayList<>();
    }
}
