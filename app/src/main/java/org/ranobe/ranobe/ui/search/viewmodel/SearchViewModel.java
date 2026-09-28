package org.ranobe.ranobe.ui.search.viewmodel;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.config.Ranobe;
import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;
import org.ranobe.ranobe.sources.SourceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchViewModel extends ViewModel {
    // repository callbacks arrive on worker threads; results are only touched on the main thread
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MutableLiveData<Map<DataSource, List<Novel>>> results = new MutableLiveData<>(Collections.emptyMap());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final LinkedHashMap<DataSource, List<Novel>> collected = new LinkedHashMap<>();
    private Filter filter = new Filter();
    // bumped on every new search so late answers from an older one are dropped
    private int generation = 0;
    private int pending = 0;

    private static List<DataSource> enabledSources() {
        List<DataSource> sources = new ArrayList<>();
        for (Integer id : SourceManager.getSources().keySet()) {
            if (!Ranobe.isSourceEnabled(id)) continue;
            DataSource dataSource = SourceManager.getSource(id).metadata();
            if (dataSource.isActive) sources.add(dataSource);
        }
        return sources;
    }

    public LiveData<Map<DataSource, List<Novel>>> getResults() {
        return results;
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public String getKeyword() {
        return filter.getKeyword();
    }

    public void search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        Filter next = new Filter();
        next.addFilter(Filter.FILTER_KEYWORD, keyword.trim());
        // same query again (e.g. back from details): keep the results / the search in flight
        if (next.equals(filter) && (!collected.isEmpty() || Boolean.TRUE.equals(loading.getValue())))
            return;

        filter = next;
        int current = ++generation;
        collected.clear();
        results.setValue(Collections.emptyMap());

        List<DataSource> sources = enabledSources();
        pending = sources.size();
        loading.setValue(pending > 0);
        for (DataSource source : sources) {
            new Repository(source.sourceId).search(next, 1, new Repository.Callback<List<Novel>>() {
                @Override
                public void onComplete(List<Novel> result) {
                    main.post(() -> onSourceDone(current, source, result));
                }

                @Override
                public void onError(Exception e) {
                    // one broken source shouldn't hide the others' results
                    main.post(() -> onSourceDone(current, source, null));
                }
            });
        }
    }

    private void onSourceDone(int searchGeneration, DataSource source, List<Novel> result) {
        if (searchGeneration != generation) return;
        pending--;
        if (result != null && !result.isEmpty()) {
            collected.put(source, result);
            results.setValue(new LinkedHashMap<>(collected));
        }
        if (pending <= 0) loading.setValue(false);
    }
}
