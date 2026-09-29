package org.ranobe.ranobe.ui.browse.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BrowseViewModel extends ViewModel {
    // repository callbacks arrive on a worker thread; paging state is only touched on the main thread
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<List<Novel>> items = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<LoadError> error = new MutableLiveData<>();
    private final Set<String> seenUrls = new HashSet<>();
    private int currentSourceId = -1;
    private int page = 0;
    private boolean endReached = false;

    public LiveData<List<Novel>> getNovels() {
        return items;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<LoadError> getError() {
        return error;
    }

    public boolean isEndReached() {
        return endReached;
    }

    // keeps loaded pages when returning to the same source (e.g. back from details)
    public void open(int sourceId) {
        if (sourceId == currentSourceId) {
            // coming back after a failed first page: try again instead of showing nothing
            List<Novel> current = items.getValue();
            if (current == null || current.isEmpty()) loadMore();
            return;
        }
        currentSourceId = sourceId;
        page = 0;
        endReached = false;
        seenUrls.clear();
        items.setValue(new ArrayList<>());
        error.setValue(null);
        loading.setValue(false);
        loadMore();
    }

    public void loadMore() {
        if (Boolean.TRUE.equals(loading.getValue()) || endReached || currentSourceId == -1) return;
        loading.setValue(true);
        int sourceId = currentSourceId;
        int nextPage = page + 1;

        new Repository(sourceId).novels(nextPage, new Repository.Callback<List<Novel>>() {
            @Override
            public void onComplete(List<Novel> result) {
                main.post(() -> onPageLoaded(sourceId, nextPage, result));
            }

            @Override
            public void onError(Exception e) {
                String message = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getSimpleName();
                main.post(() -> onPageFailed(sourceId, nextPage, message));
            }
        });
    }

    private void onPageLoaded(int sourceId, int nextPage, List<Novel> result) {
        if (sourceId != currentSourceId) return;
        List<Novel> current = items.getValue() == null ? new ArrayList<>() : new ArrayList<>(items.getValue());
        int added = 0;
        for (Novel novel : result) {
            if (seenUrls.add(novel.url)) {
                current.add(novel);
                added++;
            }
        }
        page = nextPage;
        // an empty page, or one that only repeats earlier novels, means the source has no more
        endReached = added == 0;
        loading.setValue(false);
        items.setValue(current);
    }

    private void onPageFailed(int sourceId, int nextPage, String message) {
        if (sourceId != currentSourceId) return;
        loading.setValue(false);
        error.setValue(new LoadError(message, nextPage == 1));
    }

    public void clearError() {
        error.setValue(null);
    }

    public static class LoadError {
        public final String message;
        public final boolean firstPage;

        LoadError(String message, boolean firstPage) {
            this.message = message;
            this.firstPage = firstPage;
        }
    }
}
