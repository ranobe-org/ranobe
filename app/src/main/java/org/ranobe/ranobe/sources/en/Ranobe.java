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

public class Ranobe implements Source {
    private final int sourceId = 6;

    @Override
    public DataSource metadata() {
        DataSource source = new DataSource();
        source.sourceId = sourceId;
        source.url = "https://ranobe-org.github.io/";
        source.name = "Ranobe Originals";
        source.lang = Lang.eng;
        source.dev = "ap-atul";
        source.logo = "https://ranobe-org.github.io/.github/tiny.png";
        return source;
    }

    @Override
    public List<NovelItem> novels(int page) throws Exception {
        List<NovelItem> items = new ArrayList<>();
        if (page > 1) {
            throw new Exception("well");
        }

        Novel level = new Novel("https://ranobe-org.github.io/level-unknown");
        level.name = "What is your level?";
        level.cover = "https://github.com/ranobe-org/level-unknown/raw/main/cover.jpg";
        items.add(level);

        Novel elevator = new Novel("https://ranobe-org.github.io/elevator");
        elevator.name = "Elevator";
        elevator.cover = "https://github.com/ranobe-org/elevator/raw/main/cover.jpg";
        items.add(elevator);

        return items;
    }

    @Override
    public Novel details(String url) throws IOException {
        Novel novel = new Novel(url);
        Element doc = Jsoup.parse(HttpClient.GET(url, new HashMap<>()));

        novel.sourceId = sourceId;
        novel.name = doc.select("h1.menu-title").text().trim();
        novel.cover = doc.select("img").attr("src").trim();
        novel.summary = doc.select("h2#summary").nextAll().select("p").text();
        novel.rating = 0;

        for (Element element : doc.select("main > ul > li")) {
            String value = element.text().trim();

            if (value.contains("Alternative")) {
                novel.alternateNames = Arrays.asList(value.replace("Alternative Names:", "").trim().split(","));
            } else if (value.contains("Author")) {
                novel.authors = Arrays.asList(value.replace("Author:", "").trim().split(","));
            } else if (value.contains("Genre")) {
                novel.genres = Arrays.asList(value.replace("Genre:", "").trim().split(","));
            } else if (value.contains("Year")) {
                novel.year = NumberUtils.toInt(value.replace("Year:", "").trim());
            } else if (value.contains("Status")) {
                novel.status = value.replace("Status:", "").trim();
            }
        }

        return novel;
    }

    private String substringFromC(String name) {
        int index = name.indexOf("C");
        return name.substring(index);
    }

    @Override
    public List<ChapterItem> chapters(String url) throws IOException {
        List<ChapterItem> items = new ArrayList<>();
        Element doc = Jsoup.parse(HttpClient.GET(url, new HashMap<>()));

        for (Element element : doc.select("li.chapter-item")) {
            if (element.select("a").hasClass("active")) {
                continue;
            }

            ChapterItem item = new ChapterItem(url);

            item.url = url.concat("/").concat(element.select("a").attr("href").trim());
            item.name = substringFromC(element.select("a").text());
            item.id = NumberUtils.toFloat(item.name);
            items.add(item);
        }

        return items;
    }

    @Override
    public Chapter chapter(String novelUrl, String chapterUrl) throws IOException {
        Chapter chapter = new Chapter(novelUrl);
        Element doc = Jsoup.parse(HttpClient.GET(chapterUrl, new HashMap<>()));
        Element main = doc.select("main").first();

        if (main == null) {
            return null;
        }

        chapter.url = chapterUrl;
        chapter.content = "";
        main.select("p").append("::");
        main.select("h1").append("::");
        chapter.content = SourceUtils.cleanContent(main.text().replaceAll("::", "\n\n").trim());

        return chapter;
    }

    @Override
    public List<NovelItem> search(Filter filters, int page) throws Exception {
        throw new Exception("Not Implemented. Has no results!");
    }
}
