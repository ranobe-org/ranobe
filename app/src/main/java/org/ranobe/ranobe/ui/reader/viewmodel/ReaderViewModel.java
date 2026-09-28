package org.ranobe.ranobe.ui.reader.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReaderViewModel extends ViewModel {
    private static final int MAX_PREFETCHED = 3;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    // chapters fetched ahead of the reader, keyed by url; consumed (removed) once shown
    private final Map<String, Chapter> prefetched = new ConcurrentHashMap<>();
    private final Set<String> prefetching = Collections.synchronizedSet(new HashSet<>());

    public MutableLiveData<String> getError() {
        return error;
    }

    public MutableLiveData<Chapter> getChapter(Chapter chap) {
        MutableLiveData<Chapter> chapter = new MutableLiveData<>();
        Chapter ready = prefetched.remove(chap.url);
        if (ready != null) {
            chapter.setValue(ready);
            return chapter;
        }
        RanobeDatabase.databaseExecutor.execute(() -> {
            Chapter saved = RanobeDatabase.database().chapters().getSync(chap.url);
            if (saved != null && saved.content != null && !saved.content.isEmpty()) {
                chapter.postValue(saved);
            } else {
                new Repository().chapter(chap, new Repository.Callback<Chapter>() {
                    @Override
                    public void onComplete(Chapter result) {
                        chapter.postValue(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        error.postValue(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    }
                });
            }
        });
        return chapter;
    }

    // fetch a chapter in the background so it's instant when the reader reaches it
    public void prefetch(Chapter chap) {
        if (chap == null || prefetched.containsKey(chap.url) || prefetched.size() >= MAX_PREFETCHED) return;
        if (!prefetching.add(chap.url)) return;

        RanobeDatabase.databaseExecutor.execute(() -> {
            Chapter saved = RanobeDatabase.database().chapters().getSync(chap.url);
            if (saved != null && saved.content != null && !saved.content.isEmpty()) {
                prefetching.remove(chap.url);
                return;
            }
            // sources fill in the chapter they're given, so hand them a copy
            Chapter copy = new Chapter(chap.novelUrl);
            copy.url = chap.url;
            copy.name = chap.name;
            copy.updated = chap.updated;
            copy.id = chap.id;
            new Repository().chapter(copy, new Repository.Callback<Chapter>() {
                @Override
                public void onComplete(Chapter result) {
                    if (result != null && result.content != null && !result.content.isEmpty()) {
                        prefetched.put(chap.url, result);
                    }
                    prefetching.remove(chap.url);
                }

                @Override
                public void onError(Exception e) {
                    // the regular load will retry and surface the error
                    prefetching.remove(chap.url);
                }
            });
        });
    }

    public MutableLiveData<List<Chapter>> getChapters(Novel novel) {
        MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();
        new Repository().chapters(novel, new Repository.Callback<List<Chapter>>() {
            @Override
            public void onComplete(List<Chapter> result) {
                chapters.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        });
        return chapters;
    }
}
