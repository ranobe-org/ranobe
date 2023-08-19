package org.ranobe.core.sources;


import org.ranobe.core.models.Chapter;
import org.ranobe.core.models.DataSource;
import org.ranobe.core.models.Filter;
import org.ranobe.core.models.Novel;

import java.util.List;

public interface Source {
    DataSource metadata();

    // get the list of novels based on page
    List<Novel> novels(int page) throws Exception;

    // get all the fields for a single novel
    Novel details(Novel novel) throws Exception;

    // get all chapters for a novel
    List<Chapter> chapters(Novel novel) throws Exception;

    // get content of the chapter from the url
    Chapter chapter(Chapter chapter) throws Exception;

    // search novels
    List<Novel> search(Filter filters, int page) throws Exception;
}
