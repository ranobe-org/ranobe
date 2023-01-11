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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class LightNovelPub implements Source {
    private final String baseUrl = "https://light-novelpub.com";
    private final int sourceId = 4;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "Light Novel Pub";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://light-novelpub.com/img/favicon.ico";
        return source;
    }

    @Override
    public List<NovelItem> novels(int page) throws IOException {
        String web = baseUrl.concat("/sort/hot-lightnovelpub-update/?page=").concat(String.valueOf(page));
        return parse(HttpClient.GET(web, new HashMap<>()));
    }

    private List<NovelItem> parse(String body) {
        List<NovelItem> items = new ArrayList<>();
        Element doc = Jsoup.parse(body).select("div.list-novel").first();

        if(doc == null) return items;

        for(Element element: doc.select("div.row")) {
            String url = element.select("h3.novel-title > a").attr("href").trim();

            if (url.length() > 0) {
                NovelItem item = new NovelItem(url);
                item.sourceId = sourceId;
                item.name = element.select("h3.novel-title > a").text().trim();
                item.cover = element.select("img").attr("src").replace("_200_89", "");
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(String url) throws IOException {
        Novel novel = new Novel(url);
        Element doc = Jsoup.parse(HttpClient.GET(url, new HashMap<>()));

        novel.sourceId = sourceId;
        novel.name = doc.select("h3.title").text().trim();
        novel.cover = doc.select("div.book").select("img").attr("src").trim();
        novel.summary = doc.select("div.desc-text").text().trim();
        novel.rating = NumberUtils.toFloat(doc.select("span[itemprop=ratingValue]").text()) / 2;

        for (Element element: doc.select("ul.info-meta > li")) {
            String header = element.select("h3").text();

            if (header.equalsIgnoreCase("Author:")) {
                novel.authors = Arrays.asList(element.select("a").text().split(","));
            }
            else if (header.equalsIgnoreCase("Genre:")) {
                List<String> genres = new ArrayList<>();
                for(Element a: element.select("a")) genres.add(a.text());
                novel.genres = genres;
            }
            else if (header.equalsIgnoreCase("Alternative names:")){
                novel.alternateNames = Arrays.asList(element.select("a").text().split(","));
            }
            else if (header.equalsIgnoreCase("Status:")){
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
    public List<ChapterItem> chapters(String url) throws IOException {
        List<ChapterItem> items = new ArrayList<>();
        String base = baseUrl.concat("/ajax/chapter-archive?novelId=").concat(getNovelId(url));
        Element doc = Jsoup.parse(HttpClient.GET(base, new HashMap<>()));

        for(Element element: doc.select("a")) {
            ChapterItem item = new ChapterItem(url);

            item.url = element.attr("href").trim();
            item.name = element.attr("title").trim();
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(String url) throws IOException {
        Chapter chapter = new Chapter(url);
        Element doc = Jsoup.parse(HttpClient.GET(url, new HashMap<>()));

        chapter.url = url;
        chapter.content = "";

        doc.select("div.chr-c").select("p").append("::");
        chapter.content = SourceUtils.cleanContent(
                doc.select("div.chr-c").text().replaceAll("::", "\n\n").trim()
        );

        return chapter;
    }

    @Override
    public List<NovelItem> search(Filter filters, int page) throws IOException {
        if (filters.hashKeyword()) {
            String keyword = filters.getKeyword();
            String web = SourceUtils.buildUrl(baseUrl, "/search?keyword=", keyword, "&page=", String .valueOf(page));
            return parse(HttpClient.GET(web, new HashMap<>()));
        }
        return new ArrayList<>();
    }
}
