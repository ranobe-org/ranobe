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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FreeWebNovel implements Source {
    private static final String BASE_URL = "https://freewebnovel.com";
    private static final int SOURCE_ID = 16;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = BASE_URL;
        source.name = "Free Web Novel";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://freewebnovel.com/static/freewebnovel/favicon.ico";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        String web = BASE_URL.concat("/latest-release-novels/" + (page + 1) + "/");
        return parseNovel(HttpClient.GET(web, new HashMap<>()));
    }

    private List<Novel> parseNovel(String response) {
        List<Novel> items = new ArrayList<>();
        Element doc = Jsoup.parse(response);

        for (Element element : doc.select("div.li-row")) {
            String url = element.select("div.pic > a").attr("href").trim();

            if (url.length() > 0) {
                Novel item = new Novel(BASE_URL + url);
                item.sourceId = SOURCE_ID;
                item.name = element.select("div.txt > h3.tit > a").text().trim();
                item.cover = BASE_URL.concat(element.select("div.pic > a > img").attr("src"));
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        novel.sourceId = SOURCE_ID;
        novel.name = doc.select("div.m-desc > h1").text().trim();
        novel.cover = BASE_URL.concat(doc.select("div.pic > img").attr("src").trim());
        novel.summary = String.join("\n\n", doc.select("div.inner > p").eachText());

        for (Element element : doc.select("div.txt > div.item")) {
            String check = element.select("span").attr("title");
            if (check.contains("Status")) {
                novel.status = element.select("a").text().trim();
            } else if (check.contains("Author")) {
                novel.authors = element.select("a").eachText();
            } else if (check.contains("Genre")) {
                novel.genres = element.select("a").eachText();
            }
        }

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();
        Element doc = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));

        while (true) {
            Element next = doc.select("div.page > a:contains(Next)").first();

            for (Element element : doc.select("div.m-newest2 > ul.ul-list5 > li")) {
                Chapter item = new Chapter(novel.url);

                item.url = BASE_URL + element.select("a").attr("href").trim();
                item.name = element.select("a").text().trim();
                item.id = NumberUtils.toFloat(item.name);
                items.add(item);
            }

            if (next != null) {
                String nextUrl = next.attr("href");
                doc = Jsoup.parse(HttpClient.GET(BASE_URL + nextUrl, new HashMap<>()));
            } else {
                break;
            }
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));
        chapter.content = String.join("\n\n", doc.select("div#article > p").eachText());
        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        if (filters.hashKeyword() && page == 1) {
            String keyword = filters.getKeyword();
            String web = BASE_URL + "/search/";
            HashMap<String, String> form = new HashMap<>();
            form.put("searchkey", keyword);
            String response = HttpClient.POST(web, new HashMap<>(), form);
            return parseNovel(response);
        }
        return new ArrayList<>();
    }
}
