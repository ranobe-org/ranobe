package org.ranobe.ranobe.ui.browse.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.ArrayList;
import java.util.List;

public class BrowseViewModel extends ViewModel {
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<List<Novel>> items;
    private int currentSourceId = -1;
    private int page = 0;

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public MutableLiveData<List<Novel>> getNovels(int sourceId) {
        if (currentSourceId != sourceId) {
            items = new MutableLiveData<>();
            page = 0;
            currentSourceId = sourceId;
        } else {
            page += 1;
        }
        new Repository(sourceId).novels(page, new Repository.Callback<List<Novel>>() {
            @Override
            public void onComplete(List<Novel> result) {
                List<Novel> old = items.getValue();
                if (old == null) {
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
        return items;
    }
}
