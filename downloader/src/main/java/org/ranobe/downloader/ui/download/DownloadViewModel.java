package org.ranobe.downloader.ui.download;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.core.models.Chapter;
import org.ranobe.core.models.Novel;
import org.ranobe.core.network.repository.Repository;
import org.ranobe.core.sources.Source;

import java.util.List;

public class DownloadViewModel extends ViewModel {
    private final MutableLiveData<Exception> error;

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

    public MutableLiveData<List<Chapter>> getChapters(Source source, Novel novel) {
        MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();
        new Repository(source).chapters(novel, new Repository.Callback<List<Chapter>>() {
            @Override
            public void onComplete(List<Chapter> result) {
                chapters.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
        return chapters;
    }
}
