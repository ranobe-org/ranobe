package org.ranobe.ranobe.ui.reader.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.repository.Repository;
import org.ranobe.ranobe.sources.Source;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReaderViewModel extends ViewModel {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private MutableLiveData<Chapter> chapter;
    private MutableLiveData<String> error;

    public MutableLiveData<Chapter> getChapter() {
        if (chapter == null) {
            chapter = new MutableLiveData<>();
        }
        return chapter;
    }

    public void chapter(Source source, String chapterUrl) {
        Repository repository = new Repository(executor, source);
        repository.chapter(chapterUrl, new Repository.Callback<Chapter>() {
            @Override
            public void onComplete(Chapter result) {
                chapter.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
                error = new MutableLiveData<>();
                error.postValue(e.getLocalizedMessage());
            }
        });
    }
}
