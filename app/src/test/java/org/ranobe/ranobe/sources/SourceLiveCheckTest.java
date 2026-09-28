package org.ranobe.ranobe.sources;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.HttpClient;
import org.ranobe.ranobe.sources.en.AllNovel;
import org.ranobe.ranobe.sources.ru.RanobeHub;
import org.ranobe.ranobe.sources.en.FreeWebNovel;

import java.util.HashMap;
import java.util.List;

public class SourceLiveCheckTest {

    @Test
    public void testFreeWebNovelAjaxChapters() throws Exception {
        Source source = new FreeWebNovel();
        List<Novel> novels = source.novels(1);
        assertFalse(novels.isEmpty());
        Novel novel = novels.get(0);
        String ajaxUrl = novel.url + "?ajax=chapters&page=1&pageSize=200";
        String response = HttpClient.GET(ajaxUrl, new HashMap<>());
        System.out.println("RESPONSE TYPE: " + (response.trim().startsWith("{") || response.trim().startsWith("[") ? "JSON" : "HTML"));
        System.out.println("RESPONSE: " + response);
        assertNotNull(response);
    }

    @Test
    public void testFreeWebNovelChapters() throws Exception {
        Source source = new FreeWebNovel();
        Novel novel = source.novels(1).get(0);
        List<Chapter> chapters = source.chapters(novel);
        System.out.println("CHAPTERS: " + chapters.size() + " first=" + chapters.get(0).name + " " + chapters.get(0).url
                + " last=" + chapters.get(chapters.size() - 1).name);
        assertFalse(chapters.isEmpty());
    }

    @Test
    public void testAllNovel() throws Exception {
        Source source = new AllNovel();

        List<Novel> page1 = source.novels(1);
        List<Novel> page2 = source.novels(2);
        assertFalse(page1.isEmpty());
        assertFalse(page1.get(0).url.equals(page2.get(0).url));
        System.out.println("ALLNOVEL list: " + page1.size() + " " + page1.get(0).name + " cover=" + page1.get(0).cover);

        // a novel saved while the source still pointed at allnovel.org
        Novel novel = source.details(new Novel("https://allnovel.org/infinite-mana-in-the-apocalypse.html"));
        System.out.println("ALLNOVEL details: " + novel.name + " | " + novel.authors + " | " + novel.genres + " | " + novel.status + " | " + novel.rating + " | " + novel.cover);
        assertFalse(novel.name.isEmpty());
        assertFalse(novel.summary.isEmpty());

        List<Chapter> chapters = source.chapters(novel);
        assertFalse(chapters.isEmpty());
        System.out.println("ALLNOVEL chapters: " + chapters.size() + " " + chapters.get(1).url);

        Chapter chapter = source.chapter(chapters.get(1));
        assertFalse(chapter.content.isEmpty());
        System.out.println("ALLNOVEL chapter: " + chapter.content.length() + " chars: " + chapter.content.substring(0, 120).replace("\n", " / "));

        Filter filter = new Filter();
        filter.addFilter(Filter.FILTER_KEYWORD, "martial");
        List<Novel> search = source.search(filter, 1);
        assertFalse(search.isEmpty());
        System.out.println("ALLNOVEL search: " + search.size() + " " + search.get(0).name);
    }

    @Test
    public void testRanobeHub() throws Exception {
        RanobeHub source = new RanobeHub();
        System.out.println("RH logo: " + source.metadata().logo);

        List<Novel> page1 = source.novels(1);
        List<Novel> page2 = source.novels(2);
        assertFalse(page1.isEmpty());
        assertFalse(page2.isEmpty());
        assertFalse(page1.get(0).url.equals(page2.get(0).url));
        for (Novel n : page1) System.out.println("RH list: " + n.name + " | " + n.status + " | " + n.url + " | " + n.cover);
        System.out.println("RH page2: " + page2.size() + " " + page2.get(0).name);

        Novel novel = source.details(page1.get(0));
        System.out.println("RH details: " + novel.name + " | alt=" + novel.alternateNames + " | " + novel.authors + " | " + novel.year
                + " | " + novel.genres + " | " + novel.status + " | " + novel.cover + " | " + novel.summary.length());
        assertFalse(novel.name.isEmpty());

        List<Chapter> chapters = source.chapters(novel);
        assertFalse(chapters.isEmpty());
        System.out.println("RH chapters: " + chapters.size() + " first=" + chapters.get(0).name + " " + chapters.get(0).url
                + " last=" + chapters.get(chapters.size() - 1).name + " updated=" + chapters.get(0).updated);

        Chapter chapter = source.chapter(chapters.get(0));
        assertFalse(chapter.content.isEmpty());
        System.out.println("RH chapter: " + chapter.content.length() + " chars: " + chapter.content.substring(0, Math.min(200, chapter.content.length())).replace("\n", " / "));

        Chapter text = source.chapter(chapters.get(2));
        assertFalse(text.content.isEmpty());
        System.out.println("RH text: " + text.name + " " + text.content.length() + " chars: " + text.content.substring(0, Math.min(300, text.content.length())).replace("\n", " / "));

        Filter filter = new Filter();
        filter.addFilter(Filter.FILTER_KEYWORD, "маг");
        List<Novel> search = source.search(filter, 1);
        assertFalse(search.isEmpty());
        System.out.println("RH search: " + search.size() + " " + search.get(0).name + " | " + search.get(0).url + " | " + search.get(0).cover);
    }
}
