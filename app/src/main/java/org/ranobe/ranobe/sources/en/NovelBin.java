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

public class NovelBin implements Source {

    private final String baseUrl = "https://novelbin.me";
    private final int sourceId = 18;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "Novel Bin";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://novelbin.me/img/logo.png";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = page == 1
                ? baseUrl + "/sort/novelbin-popular"
                : baseUrl + "/sort/novelbin-popular?page=" + page;
        return parse(HttpClient.GET(web, new HashMap<>()), false);
    }

    private List<Novel> parse(String body, boolean fromSearch) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select("div.list-novel").first();

        if (doc == null) return items;

        for (Element element : doc.select("div.row")) {
            String url = element.select("h3.novel-title > a").attr("href").trim();

            if (!url.isEmpty()) {
                Novel item = new Novel(url);
                item.sourceId = sourceId;
                item.name = element.select("h3.novel-title > a").text().trim();

                String imageFieldLookup = fromSearch ? "src" : "data-src";
                item.cover = element.select("img").attr(imageFieldLookup).replace("_200_89", "");
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = sourceId;
        novel.name = doc.select("h3.title").first().text().trim();
        novel.cover = doc.select("div.book").select("img").attr("data-src").trim();

        doc.select("div.desc-text").select("p").append("::");
        novel.summary = doc.select("div.desc-text").text().replaceAll("::", "\n\n").trim();
        novel.rating = NumberUtils.toFloat(doc.select("span[itemprop=ratingValue]").text()) / 2;

        for (Element element : doc.select("ul.info-meta > li")) {
            String header = element.select("h3").text();

            if (header.equalsIgnoreCase("Author:")) {
                novel.authors = Arrays.asList(element.select("a").text().split(","));
            } else if (header.equalsIgnoreCase("Genre:")) {
                List<String> genres = new ArrayList<>();
                for (Element a : element.select("a")) genres.add(a.text());
                novel.genres = genres;
            } else if (header.equalsIgnoreCase("Alternative names:")) {
                novel.alternateNames = Arrays.asList(element.select("a").text().split(","));
            } else if (header.equalsIgnoreCase("Status:")) {
                novel.status = element.select("a").text().trim();
            }
        }

        return novel;
    }

    private String getNovelId(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        String base = baseUrl.concat("/ajax/chapter-archive?novelId=").concat(getNovelId(novel.url));
        Element doc = Jsoup.parse(HttpClient.GET(base, new HashMap<>()));

        for (Element element : doc.select("a")) {
            Chapter item = new Chapter(novel.url);

            item.url = element.attr("href").trim();
            item.name = element.attr("title").trim();
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));

        chapter.content = "";
        doc.select("div.chr-c").select("p").append("::");
        doc.select("div.unlock-buttons").remove();
        chapter.content = SourceUtils.cleanContent(
                doc.select("div.chr-c").text().replaceAll("::", "\n\n\n").trim()
        );

        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws IOException {
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/search?keyword=", keyword, "&page=", String.valueOf(page));
            return parse(HttpClient.GET(web, new HashMap<>()), true);
        }
        return new ArrayList<>();
    }
}
