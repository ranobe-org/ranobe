package org.ranobe.ranobe.repository;

import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.sources.Source;

import java.util.List;
import java.util.concurrent.Executor;

public class Repository {
    public interface Callback<T> {
        void onComplete(T result);
        void onError(Exception e);
    }

    private final Executor executor;
    private final Source source;

    public Repository(Executor executor, Source source) {
        this.executor = executor;
        this.source = source;
    }

    public void novels(int page, Callback<List<NovelItem>> callback) {
        executor.execute(() -> {
            try {
                List<NovelItem>  result = source.novels(page);
                callback.onComplete(result);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }
}
