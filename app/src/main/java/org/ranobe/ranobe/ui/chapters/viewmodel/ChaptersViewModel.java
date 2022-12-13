package org.ranobe.ranobe.ui.chapters.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.repository.Repository;
import org.ranobe.ranobe.sources.Source;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChaptersViewModel extends ViewModel {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<List<ChapterItem>> chapters;
    private String oldUrl = "";

    public MutableLiveData<List<ChapterItem>> getChapters(String novelUrl) {
        if (chapters == null || !oldUrl.equals(novelUrl)) {
            oldUrl = novelUrl;
            chapters = new MutableLiveData<>();
        }
        return chapters;
    }

    public void chapters(Source source, String novelUrl) {
        Repository repository = new Repository(executor, source);
        repository.chapters(novelUrl, new Repository.Callback<List<ChapterItem>>() {
            @Override
            public void onComplete(List<ChapterItem> result) {
                chapters.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
    }
}
