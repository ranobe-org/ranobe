package org.ranobe.ranobe.ui.browse.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.NovelItem;
import org.ranobe.ranobe.repository.Repository;
import org.ranobe.ranobe.sources.Source;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrowseViewModel extends ViewModel {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private MutableLiveData<List<NovelItem>> items;
    private MutableLiveData<String> error;

    public MutableLiveData<List<NovelItem>> getNovels() {
        if (items == null) {
            items = new MutableLiveData<>();
        }
        return items;
    }

    public void novels(Source source, int page) {
        Repository repository = new Repository(executor, source);
        repository.novels(page, new Repository.Callback<List<NovelItem>>() {
            @Override
            public void onComplete(List<NovelItem> result) {
                items.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
    }
}
