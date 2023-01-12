package org.ranobe.ranobe.ui.reader.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.repository.Repository;

import java.util.List;

public class ReaderViewModel extends ViewModel {
    private MutableLiveData<Chapter> chapter;
    private MutableLiveData<List<ChapterItem>> chapters;
    private MutableLiveData<String> error;

    public MutableLiveData<Chapter> getChapter() {
        if (chapter == null) {
            chapter = new MutableLiveData<>();
        }
        return chapter;
    }

    public MutableLiveData<List<ChapterItem>> getChapters() {
        if (chapters == null) {
            chapters = new MutableLiveData<>();
        }
        return chapters;
    }

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public void chapter(String chapterUrl) {
        new Repository().chapter(chapterUrl, new Repository.Callback<Chapter>() {
            @Override
            public void onComplete(Chapter result) {
                chapter.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
    }

    public void chapters(String novelUrl) {
        new Repository().chapters(novelUrl, new Repository.Callback<List<ChapterItem>>() {
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
