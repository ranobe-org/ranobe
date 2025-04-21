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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class WordRain69 implements Source {

    public static final String BASE_URL = "https://wordrain69.com";
    public static final int SOURCE_ID = 17;


    @Override
    public DataSource metadata() {

        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = BASE_URL;
        source.name = "WordRain69";
        source.lang = Lang.eng;
        source.dev = "ak-sohag";
        source.logo = "https://wordrain69.com/storage/2024/06/cropped-IMG_20240623_094303-270x270.png";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = BASE_URL.concat("/manga-genre/novel/page/" + page + "/");
        return parseNovel(HttpClient.GET(web, new HashMap<>()));
    }

    private List<Novel> parseNovel(String response) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(response);

        for (Element element : doc.select(".page-item-detail.text")) {
            String url = element.select("h3.h5 > a").attr("href").trim();

            if (url.length() > 0) {
                Novel item = new Novel(url);
                item.sourceId = SOURCE_ID;
                item.name = element.select("h3.h5 > a").text().trim();
                item.cover = element.select("div.item-thumb  img").attr("src");
                items.add(item);
            }
        }
        return items;
    }


    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = SOURCE_ID;
        novel.name = doc.select("div.post-title > h1").text().trim();
        novel.cover = doc.select("div.summary_image > a > img").attr("src").trim();
        novel.summary = String.join("\n\n", doc.select("div.summary__content > p").eachText());
        novel.rating = NumberUtils.toFloat(doc.select("div.post-total-rating span.score").text().trim());
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
    public List<Chapter> chapters(Novel novel) throws Exception {
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
    public Chapter chapter(Chapter chapter) throws Exception {
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
    public List<Novel> search(Filter filters, int page) throws Exception {

        //// Search function is not working in site

        List<Novel> items = new ArrayList<>();

//        if (filters.hashKeyword()) {
//            String web = SourceUtils.buildUrl(BASE_URL,"/?s=", filters.getKeyword());
//            Element doc = Jsoup.parse(HttpClient.GET(web, new HashMap<>()));
//            for (Element element : doc.select(".page-item-detail.text")) {
//                String url = element.select("h3.h5 > a").attr("href").trim();
//
//                if (url.length() > 0) {
//                    Novel item = new Novel(url);
//                    item.sourceId = SOURCE_ID;
//                    item.url = url;
//                    item.name = element.select("h3.h5 > a").text().trim();
//                    item.cover = element.select("div.item-thumb  img").attr("src");
//
//                    items.add(item);
//                }
//            }
//        }

        return items;
    }
}
