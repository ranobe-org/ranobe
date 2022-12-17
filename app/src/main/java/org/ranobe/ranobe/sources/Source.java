package org.ranobe.ranobe.sources;

import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;

import java.util.List;

public interface Source {
    DataSource metadata();

    // get the list of novels based on page
    List<NovelItem> novels(int page) throws Exception;

    // get all the fields for a single novel
    Novel details(String url) throws Exception;

    // get all chapters for a novel
    List<ChapterItem> chapters(String url) throws Exception;

    // get content of the chapter from the url
    Chapter chapter(String url) throws Exception;

    // search novels
    List<NovelItem> search(Filter filters, int page) throws Exception;
}
