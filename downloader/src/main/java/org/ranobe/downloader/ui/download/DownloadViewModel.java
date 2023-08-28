package org.ranobe.downloader.ui.download;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.core.models.Novel;
import org.ranobe.core.network.repository.Repository;
import org.ranobe.core.sources.Source;

public class DownloadViewModel extends ViewModel {
    private MutableLiveData<Exception> error;

    public DownloadViewModel() {
        error = new MutableLiveData<>();
    }

    public MutableLiveData<Exception> getError() {
        return error;
    }

    public MutableLiveData<Novel> getNovel(Source source, Novel novel) {
        MutableLiveData<Novel> result = new MutableLiveData<>();
        new Repository(source).details(novel, new Repository.Callback<Novel>() {
            @Override
            public void onComplete(Novel n) {
                result.postValue(n);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                error.postValue(e);
            }
        });
        return result;
    }
}
