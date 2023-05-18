package org.ranobe.ranobe.ui.search.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.ArrayList;
import java.util.List;

public class SearchViewModel extends ViewModel {
    private MutableLiveData<String> error = new MutableLiveData<>();
    private Filter oldFilter = new Filter();

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public MutableLiveData<List<Novel>> search(int sourceId, Filter filter, int page) {
        MutableLiveData<List<Novel>> items = new MutableLiveData<>();
        new Repository(sourceId).search(filter, page, new Repository.Callback<List<Novel>>() {
            @Override
            public void onComplete(List<Novel> result) {
                List<Novel> old = items.getValue();
                // add more items without losing old ones
                if (old == null) {
                    old = new ArrayList<>();
                }

                // if filter changes in this iteration, clear old results
                if (!oldFilter.equals(filter)) {
                    old.clear();
                    oldFilter = filter;
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
