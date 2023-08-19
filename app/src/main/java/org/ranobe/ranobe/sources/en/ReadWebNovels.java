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

public class ReadWebNovels implements Source {
    private static final String baseUrl = "https://readwebnovels.net/";
    private static final int sourceId = 11;

    private String cleanImg(String cover) {
        return cover.replaceAll("/-\\d+x\\d+.\\w{3}/gm", ".jpg");
    }

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = baseUrl;
        source.name = "Read Web Novels";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://readwebnovels.net/wp-content/uploads/2020/01/cropped-boo1k-180x180.png";
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws IOException {
        List<Novel> items = new ArrayList<>();
        String web = baseUrl.concat("/page/").concat(String.valueOf(page));
        Element doc = Jsoup.parse(HttpClient.GET(web, new HashMap<>()));

        for (Element element : doc.select(".page-item-detail")) {
            String url = element.select(".h5 > a").attr("href").trim();

            if (url.length() > 0) {
                Novel item = new Novel(url);
                item.sourceId = sourceId;
                item.name = element.select(".h5 > a").text().trim();
                item.cover = cleanImg(element.select("img").attr("src").trim());
                items.add(item);
            }
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws IOException {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = sourceId;
        novel.name = doc.select(".post-title > h1").text().trim();
        novel.cover = cleanImg(doc.select(".summary_image > a > img").attr("Src").trim());
        novel.summary = doc.select("div.summary__content").text().replaceAll("\n", "\n\n").trim();
        novel.rating = NumberUtils.toFloat(doc.select(".total_votes").text().trim());
        novel.authors = Arrays.asList(doc.select(".author-content > a").text().split(","));

        List<String> genres = new ArrayList<>();
        for (Element element : doc.select(".genres-content > a")) {
            genres.add(element.text().trim());
        }
        novel.genres = genres;

        for (Element element : doc.select(".post-content_item")) {
            String header = element.select(".summary-heading > h5").text().trim();
            String content = element.select(".summary-content").text().trim();

            if (header.equalsIgnoreCase("Status")) {
                novel.status = content;
            } else if (header.equalsIgnoreCase("Alternative")) {
                novel.alternateNames = Arrays.asList(content.split(","));
            } else if (header.equalsIgnoreCase("Release")) {
                novel.year = NumberUtils.toInt(content);
            }
        }

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws IOException {
        List<Chapter> items = new ArrayList<>();
        String web = novel.url.concat("ajax/chapters");
        Element doc = Jsoup.parse(HttpClient.POST(web, new HashMap<>(), new HashMap<>()));

        for (Element element : doc.select(".wp-manga-chapter")) {
            Chapter item = new Chapter(novel.url);

            item.url = element.select("a").attr("href").trim();
            item.name = element.select("a").text().trim();
            item.id = NumberUtils.toFloat(item.name);
            item.updated = element.select("span.chapter-release-date").text().trim();
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws IOException {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));
        Element main = doc.select(".reading-content").first();

        if (main == null) {
            return null;
        }

        chapter.content = "";
        chapter.content = String.join("\n\n", main.select("p").eachText());
        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws IOException {
        List<Novel> items = new ArrayList<>();

        if (filters.hashKeyword()) {
            String web = SourceUtils.buildUrl(baseUrl, "/page/", String.valueOf(page), "/?s=", filters.getKeyword(), "&post_type=wp-manga");
            Element doc = Jsoup.parse(HttpClient.GET(web, new HashMap<>()));
            for (Element element : doc.select(".c-tabs-item__content")) {
                String url = element.select(".tab-thumb  > a").attr("href").trim();

                if (url.length() > 0) {
                    Novel item = new Novel(url);
                    item.sourceId = sourceId;
                    item.url = url;
                    item.name = element.select(".post-title > h3 > a").text().trim();
                    item.cover = cleanImg(element.select("img.img-responsive").attr("src").trim());

                    items.add(item);
                }
            }
        }

        return items;
    }
}
