package org.ranobe.ranobe.ui.chapters.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterItem;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.List;

public class ChaptersViewModel extends ViewModel {
    private MutableLiveData<String> error = new MutableLiveData<>();
    private MutableLiveData<List<ChapterItem>> chapters;
    private String oldUrl = "";

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public MutableLiveData<List<ChapterItem>> getChapters(String novelUrl) {
        if (chapters == null || !oldUrl.equals(novelUrl)) {
            oldUrl = novelUrl;
            chapters = new MutableLiveData<>();
        }
        return chapters;
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

    public MutableLiveData<Chapter> chapter(String novelUrl, String chapterUrl) {
        MutableLiveData<Chapter> chapter = new MutableLiveData<>();
        new Repository().chapter(novelUrl, chapterUrl, new Repository.Callback<Chapter>() {
            @Override
            public void onComplete(Chapter result) {
                chapter.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
        return chapter;
    }
}
