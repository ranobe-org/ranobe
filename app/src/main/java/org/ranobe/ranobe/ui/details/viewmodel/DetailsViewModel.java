package org.ranobe.ranobe.ui.details.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.repository.Repository;

public class DetailsViewModel extends ViewModel {
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<Novel> details;
    private String oldUrl = "";

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public MutableLiveData<Novel> getDetails(String novelUrl) {
        if (details == null || !oldUrl.equals(novelUrl)) {
            oldUrl = novelUrl;
            details = new MutableLiveData<>();
        }
        return details;
    }

    public void details(String novelUrl) {
        new Repository().details(novelUrl, new Repository.Callback<Novel>() {
            @Override
            public void onComplete(Novel result) {
                details.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
    }
}
