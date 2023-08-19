package org.ranobe.ranobe.sources.en;

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

public class LightNovelBtt implements Source {
    private final String baseUrl = "https://lightnovelbtt.com";
    private final int sourceId = 3;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "Light Novel Btt";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://lightnovelbtt.com/Content/images/favicon/android-icon-192x192.png";
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws IOException {
        String web = baseUrl.concat("/?page=").concat(String.valueOf(page)).concat("&typegroup=0");
        return parse(HttpClient.GET(web, new HashMap<>()));
    }

    private String httpsImage(String url) {
        return url.replace("http://", "https://");
    }

    private List<Novel> parse(String body) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select("div.items").first();

        if (doc == null) return items;

        for (Element element : doc.select("div.item")) {
            String url = element.select("div.box_img > a").attr("href").trim();

            if (url.length() > 0) {
                Novel item = new Novel(url);
                item.sourceId = sourceId;
                item.name = element.select("div.title").text().trim();
                item.cover = httpsImage(element.select("img").attr("data-src").trim());
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = sourceId;
        novel.name = doc.select("h1.title-detail").text().trim();
        novel.cover = httpsImage(
                doc.select("div.detail-info").select("img").attr("data-src").trim()
        );
        doc.select("p#summary").select("br").append("::");
        novel.summary = doc.select("p#summary").text().replaceAll("::", "\n\n").trim();
        novel.authors = Arrays.asList(doc.select("li.author > p.col-xs-10 > a").text().trim().split(","));
        doc.select("li.kind > p").select("a").append("::");
        novel.genres = Arrays.asList(doc.select("li.kind > p").text().split("::"));
        novel.status = doc.select("li.status > p.col-xs-10").text().trim();

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        for (Element element : doc.select("div.list-chapter").select("li.row")) {
            Chapter item = new Chapter(novel.url);

            if (element.hasClass("heading")) {
                continue;
            }

            item.url = element.select("a").attr("href").trim();
            item.name = element.select("a").text().trim();
            item.id = NumberUtils.toFloat(item.name);
            item.updated = element.select("div.col-xs-4").text().trim();
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));

        chapter.content = "";
        doc.select("div.reading-detail").select("p").append("::");
        chapter.content = SourceUtils.cleanContent(
                doc.select("div.reading-detail").text().replaceAll("::", "\n\n").trim()
        );

        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws IOException {
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/find-story?keyword=", keyword, "&page=", String.valueOf(page));
            return parse(HttpClient.GET(web, new HashMap<>()));
        }
        return new ArrayList<>();
    }
}
