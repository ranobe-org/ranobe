package org.ranobe.ranobe.ui.search.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.DataSource;
import org.ranobe.ranobe.models.Filter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.LinkedHashMap;
import java.util.List;

public class SearchViewModel extends ViewModel {
    private final MutableLiveData<LinkedHashMap<DataSource, List<Novel>>> results = new MutableLiveData<>();
    private MutableLiveData<String> error = new MutableLiveData<>();
    private Filter filter = new Filter();

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public Filter getFilter() {
        return filter;
    }

    public MutableLiveData<LinkedHashMap<DataSource, List<Novel>>> search(List<DataSource> sources, Filter filter, int page) {
        if (this.filter.equals(filter) && results.getValue() != null) {
            return results;
        }

        this.filter = filter;
        for (DataSource source : sources) {
            new Repository(source.sourceId).search(filter, page, new Repository.Callback<List<Novel>>() {
                @Override
                public void onComplete(List<Novel> result) {
                    if (!result.isEmpty()) {
                        LinkedHashMap<DataSource, List<Novel>> old = results.getValue();
                        if (old == null)
                            old = new LinkedHashMap<>();
                        old.put(source, result);
                        results.postValue(old);
                    }
                }

                @Override
                public void onError(Exception e) {
                    error.postValue(e.getLocalizedMessage());
                }
            });
        }
        return results;
    }

}
