package org.ranobe.ranobe.repository;

import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.sources.Source;

import java.util.List;
import java.util.concurrent.Executor;

public class Repository {
    private final Executor executor;
    private final Source source;

    public Repository(Executor executor, Source source) {
        this.executor = executor;
        this.source = source;
    }

    public void novels(int page, Callback<List<NovelItem>> callback) {
        executor.execute(() -> {
            try {
                List<NovelItem> result = source.novels(page);
                callback.onComplete(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void details(String novelUrl, Callback<Novel> callback) {
        executor.execute(() -> {
            try {
                Novel result = source.details(novelUrl);
                callback.onComplete(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void chapters(String novelUrl, Callback<List<ChapterItem>> callback) {
        executor.execute(() -> {
            try {
                List<ChapterItem> items = source.chapters(novelUrl);
                callback.onComplete(items);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void chapter(String chapterUrl, Callback<Chapter> callback) {
        executor.execute(() -> {
            try {
                Chapter item = source.chapter(chapterUrl);
                callback.onComplete(item);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public interface Callback<T> {
        void onComplete(T result);

        void onError(Exception e);
    }
}
