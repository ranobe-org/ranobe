package org.ranobe.core.sources.en;

import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MyDramaNovel implements Source {
    private static final String BASE_URL = "https://mydramanovel.com";
    private static final int SOURCE_ID = 19;

    // internal logic: not related to the site
    private static final int NOVELS_PER_PAGE = 15;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = SOURCE_ID;
        source.url = BASE_URL;
        source.name = "My Drama Novel";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://mydramanovel.com/wp-content/uploads/2025/01/Icon-300x300.webp";
        source.isActive = true;
        return source;
    }

    @Override
    public List<Novel> novels(int page) throws Exception {
        List<Novel> items = new ArrayList<>();
        String web = BASE_URL + "/novels/";
        Element doc = Jsoup.parse(HttpClient.GET(web, new HashMap<>()));

        List<String> allNovels = new ArrayList<>();

        for (Element element : doc.select("div.td-ct-wrap > a")) {
            String url = element.attr("href").trim();
            allNovels.add(url);
        }

        int start = (page == 0 || page == 1) ? 0 : NOVELS_PER_PAGE * (page - 1);
        int end = start + NOVELS_PER_PAGE;
        List<String> sub = allNovels.subList(start, end);

        for (String u : sub) {
            Element itemPage = Jsoup.parse(HttpClient.GET(u, new HashMap<>()));

            Novel item = new Novel(u);
            item.sourceId = SOURCE_ID;
            item.name = itemPage.select("h1.tdb-title-text").text().trim();
            item.cover = itemPage.select("span.entry-thumb").attr("data-img-url").trim();
            items.add(item);
        }

        return items;
    }

    @Override
    public Novel details(Novel novel) throws Exception {
        Element itemPage = Jsoup.parse(HttpClient.GET(novel.url, new HashMap<>()));
        Element section = itemPage.select("div.tdb_category_description").first();

        List<String> summary = new ArrayList<>();
        boolean isSynopsis = false;
        assert section != null;
        for (Element element : section.select("p")) {
            String text = element.text();
            if (text.startsWith("Original Title:")) {
                novel.alternateNames = new ArrayList<>();
                novel.alternateNames.add(text.replace("Original Title:", ""));
            }
            if (text.startsWith("Author:")) {
                novel.authors = new ArrayList<>();
                novel.authors.add(text.replace("Author:", ""));
            }
            if (text.startsWith("Synopsis")) {
                isSynopsis = true;
                continue;
            }
            if (isSynopsis) {
                summary.add(text.trim());
            }
        }

        novel.summary = StringUtil.join(summary, "\n");

        return novel;
    }

    @Override
    public List<Chapter> chapters(Novel novel) throws Exception {
        List<Chapter> items = new ArrayList<>();

        String currentUrl = novel.url;
        while (currentUrl != null) {
            items.addAll(parseChapters(currentUrl));

            Element itemPage = Jsoup.parse(HttpClient.GET(currentUrl, new HashMap<>()));
            Element pagination = itemPage.select("a[aria-label='next-page']").first();
            if (pagination != null) {
                currentUrl = pagination.attr("href").trim();
            } else {
                break;
            }
        }

        return items;
    }

    public List<Chapter> parseChapters(String pageUrl) throws Exception {
        List<Chapter> items = new ArrayList<>();
        Element itemPage = Jsoup.parse(HttpClient.GET(pageUrl, new HashMap<>()));
        Element main = itemPage.select("div.td-main-content-wrap").first();
        assert main != null;
        main.select("div#tdi_64").remove();

        for (Element element : main.select("a[rel='bookmark']")) {
            String title = element.text().trim();
            String url = element.attr("href").trim();

            if (title.isEmpty()) continue;

            Chapter item = new Chapter(url);
            item.url = url;
            item.name = title;
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(Chapter chapter) throws Exception {
        Element doc = Jsoup.parse(HttpClient.GET(chapter.url, new HashMap<>()));
        chapter.content = String.join("\n\n", doc.select("div.tdb_single_content").first().select("p").eachText());
        return chapter;
    }

    @Override
    public List<Novel> search(Filter filters, int page) throws Exception {
        return Collections.emptyList();
    }
}
