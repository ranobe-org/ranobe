package org.ranobe.core.network.repository;

import org.ranobe.core.models.Chapter;
import org.ranobe.core.models.Filter;
import org.ranobe.core.models.Novel;
import org.ranobe.core.sources.Source;
import org.ranobe.core.sources.SourceManager;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Repository {
    private final Executor executor;
    private final Source source;

    public Repository(int sourceId) {
        this.executor = Executors.newCachedThreadPool();
        this.source = SourceManager.getSource(sourceId);
    }

    public Repository(Source source) {
        this.executor = Executors.newCachedThreadPool();
        this.source = source;
    }

    public void novels(int page, Callback<List<Novel>> callback) {
        executor.execute(() -> {
            try {
                List<Novel> result = source.novels(page);
                callback.onComplete(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void details(Novel novel, Callback<Novel> callback) {
        executor.execute(() -> {
            try {
                Novel result = source.details(novel);
                callback.onComplete(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void chapters(Novel novel, Callback<List<Chapter>> callback) {
        executor.execute(() -> {
            try {
                List<Chapter> items = source.chapters(novel);
                callback.onComplete(items);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void chapter(Chapter chapter, Callback<Chapter> callback) {
        executor.execute(() -> {
            try {
                Chapter item = source.chapter(chapter);
                callback.onComplete(item);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }

    public void search(Filter filter, int page, Callback<List<Novel>> callback) {
        executor.execute(() -> {
            try {
                List<Novel> items = source.search(filter, page);
                callback.onComplete(items);
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
