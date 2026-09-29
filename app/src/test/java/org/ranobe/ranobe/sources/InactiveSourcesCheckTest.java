package org.ranobe.ranobe.sources;

import org.junit.Test;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.sources.en.*;

import java.util.List;

public class InactiveSourcesCheckTest {

    private void check(Source source) {
        String name = source.getClass().getSimpleName();
        String stage = "novels";
        try {
            List<Novel> novels = source.novels(1);
            System.out.println(name + " novels: " + novels.size() + (novels.isEmpty() ? "" : " first=" + novels.get(0).name + " " + novels.get(0).url + " cover=" + novels.get(0).cover));
            if (novels.isEmpty()) return;
            stage = "details";
            Novel novel = source.details(novels.get(0));
            System.out.println(name + " details: " + novel.name + " | authors=" + novel.authors + " | genres=" + novel.genres + " | status=" + novel.status + " | summaryLen=" + (novel.summary == null ? -1 : novel.summary.length()));
            stage = "chapters";
            List<Chapter> chapters = source.chapters(novel);
            System.out.println(name + " chapters: " + chapters.size() + (chapters.isEmpty() ? "" : " first=" + chapters.get(0).name + " " + chapters.get(0).url));
            if (chapters.isEmpty()) return;
            stage = "chapter";
            Chapter chapter = source.chapter(chapters.get(0));
            String content = chapter == null ? null : chapter.content;
            System.out.println(name + " chapter: len=" + (content == null ? -1 : content.length()) + (content == null ? "" : " " + content.substring(0, Math.min(100, content.length())).replace("\n", " / ")));
            stage = "search";
            Filter filter = new Filter();
            filter.addFilter(Filter.FILTER_KEYWORD, "love");
            System.out.println(name + " search: " + source.search(filter, 1).size());
        } catch (Throwable e) {
            System.out.println(name + " FAILED at " + stage + ": " + e);
        }
    }

    @Test public void wordRain69() { check(new WordRain69()); }
    @Test public void myDramaNovel() { check(new MyDramaNovel()); }
    @Test public void neovel() { check(new Neovel()); }
    @Test public void novelBin() { check(new NovelBin()); }
    @Test public void ranobe() { check(new Ranobe()); }
    @Test public void vipNovel() { check(new VipNovel()); }
    @Test public void newNovel() { check(new NewNovel()); }
    @Test public void lightNovelPub() { check(new LightNovelPub()); }
}
