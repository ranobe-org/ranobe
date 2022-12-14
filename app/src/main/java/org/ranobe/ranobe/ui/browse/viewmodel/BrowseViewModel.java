package org.ranobe.ranobe.ui.browse.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.repository.Repository;
import org.ranobe.ranobe.sources.Source;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrowseViewModel extends ViewModel {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<List<NovelItem>> items;
    private int page = 0;

    public MutableLiveData<List<NovelItem>> getNovels() {
        if (items == null) {
            items = new MutableLiveData<>();
        }
        return items;
    }

    public void novels(Source source) {
        page += 1;
        Repository repository = new Repository(executor, source);
        repository.novels(page, new Repository.Callback<List<NovelItem>>() {
            @Override
            public void onComplete(List<NovelItem> result) {
                List<NovelItem> old = items.getValue();
                if(old == null) {
                    old = new ArrayList<>();
                }
                old.addAll(result);
                items.postValue(old);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
    }
}
