package org.ranobe.ranobe.ui.reader.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.List;

public class ReaderViewModel extends ViewModel {
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public MutableLiveData<String> getError() {
        return error;
    }

    public MutableLiveData<Chapter> getChapter(Chapter chap) {
        MutableLiveData<Chapter> chapter = new MutableLiveData<>();
        RanobeDatabase.databaseExecutor.execute(() -> {
            Chapter saved = RanobeDatabase.database().chapters().getSync(chap.url);
            if (saved != null && saved.content != null && !saved.content.isEmpty()) {
                chapter.postValue(saved);
            } else {
                new Repository().chapter(chap, new Repository.Callback<Chapter>() {
                    @Override
                    public void onComplete(Chapter result) {
                        chapter.postValue(result);
                    }

                    @Override
                    public void onError(Exception e) {
                        error.postValue(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    }
                });
            }
        });
        return chapter;
    }

    public MutableLiveData<List<Chapter>> getChapters(Novel novel) {
        MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();
        new Repository().chapters(novel, new Repository.Callback<List<Chapter>>() {
            @Override
            public void onComplete(List<Chapter> result) {
                chapters.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            }
        });
        return chapters;
    }
}
