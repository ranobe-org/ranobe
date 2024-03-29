package org.ranobe.ranobe.ui.details.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

public class DetailsViewModel extends ViewModel {
    private MutableLiveData<String> error = new MutableLiveData<>();

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public MutableLiveData<Novel> details(Novel novel) {
        MutableLiveData<Novel> details = new MutableLiveData<>();
        new Repository().details(novel, new Repository.Callback<Novel>() {
            @Override
            public void onComplete(Novel result) {
                details.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
        return details;
    }
}
