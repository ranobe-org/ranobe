package org.ranobe.core.sources.en;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.ranobe.core.models.Chapter;
import org.ranobe.core.models.DataSource;
import org.ranobe.core.models.Filter;
import org.ranobe.core.models.Lang;
import org.ranobe.core.models.Novel;
import org.ranobe.core.network.HttpClient;
import org.ranobe.core.sources.Source;
import org.ranobe.core.util.NumberUtils;
import org.ranobe.core.util.SourceUtils;

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
        source.isActive = false;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = baseUrl + "/popular-novels?page=" + page;
        return parse(HttpClient.GET(web, new HashMap<>()));
    }

    private List<Novel> parse(String body) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select("div.columns.is-multiline").first();
        if (doc == null) return items;

        for (Element element : doc.select("a.box.is-shadowless")) {
            String url = element.attr("href").trim();

            if (url.length() > 0) {
                Novel item = new Novel(url);
                item.sourceId = sourceId;
                item.name = element.select("div.content > p.gtitle").attr("title").trim();
                item.cover = element.select("img.athumbnail").attr("data-src");


                items.add(item);
            }

        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(baseUrl + novel.url, new HashMap<>()));
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
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        String base = baseUrl.concat(novel.url);
        Element doc = Jsoup.parse(HttpClient.GET(base, new HashMap<>()));

        for (Element element : doc.select("div.chapter-list a")) {
            Chapter item = new Chapter(novel.url);

            item.url = element.attr("href").trim();
            item.name = element.text().trim();
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }
        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(baseUrl + chapter.url, new HashMap<>()));

        chapter.url = baseUrl + chapter.url;
        chapter.content = "";

        doc.select("div.columns div div:eq(4)").select("p").append("::");
        chapter.content = SourceUtils.cleanContent(
                doc.select("div.columns div div:eq(4)").text().replaceAll("::", "\n\n").trim()
        );

        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/search?q=", keyword, "&page=", String.valueOf(page));
            return parse(HttpClient.GET(web, new HashMap<>()));
        }
        return new ArrayList<>();
    }
}
