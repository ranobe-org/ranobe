package org.ranobe.ranobe.ui.chapters.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.database.mapper.ChapterMapper;
import org.ranobe.ranobe.models.Chapter;
import org.ranobe.ranobe.models.ChapterMetadata;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.network.repository.Repository;

import java.util.List;

public class ChaptersViewModel extends ViewModel {
    private MutableLiveData<String> error = new MutableLiveData<>();

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    private static final long CACHE_EXPIRY_HOURS = 24;

    public MutableLiveData<List<Chapter>> getChapters(Novel novel) {

        MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();

        RanobeDatabase.databaseExecutor.execute(()->{

            List<ChapterMetadata> cachedMetadata = RanobeDatabase.database().chapterMetadata().listByUrl(novel.url);
            boolean hasCache = cachedMetadata != null && !cachedMetadata.isEmpty();
            boolean cacheExpired = hasCache && isCacheExpired(cachedMetadata.get(0).cachedDate);

            if (!hasCache || cacheExpired) {
                // Cache miss or expired → fetch from network
                fetchFromNetwork(novel, chapters, error, hasCache);
            } else {
                chapters.postValue(ChapterMapper.ToChapterList(cachedMetadata));
            }
        });
        return chapters;
    }

    private boolean isCacheExpired(long cachedDate) {
        long expiryTime = cachedDate + (CACHE_EXPIRY_HOURS * 60 * 60 * 1000);
        return System.currentTimeMillis() > expiryTime;
    }

    private void fetchFromNetwork(Novel novel,
                                  MutableLiveData<List<Chapter>> chapters,
                                  MutableLiveData<String> error,
                                  boolean shouldDeleteOldCache) {
        RanobeDatabase.databaseExecutor.execute(() -> {
            new Repository().chapters(novel, new Repository.Callback<List<Chapter>>() {
                @Override
                public void onComplete(List<Chapter> result) {
                    if (shouldDeleteOldCache) {
                        RanobeDatabase.database().chapterMetadata().deleteByNovel(novel.url);
                    }
                    List<ChapterMetadata> list = ChapterMapper.ToChapterMetadataList(result);
                    for(int i = 0;i<list.size();i++) list.get(i).cachedDate = System.currentTimeMillis();
                    RanobeDatabase.database().chapterMetadata().saveAll(list);
                    chapters.postValue(result);
                }

                @Override
                public void onError(Exception e) {
                    error.postValue(e.getLocalizedMessage());
                }
            });

        });
    }

    public MutableLiveData<Chapter> chapter(Chapter chap) {
        MutableLiveData<Chapter> chapter = new MutableLiveData<>();
        new Repository().chapter(chap, new Repository.Callback<Chapter>() {
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
