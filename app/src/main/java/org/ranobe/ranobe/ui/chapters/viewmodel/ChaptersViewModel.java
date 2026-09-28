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
    private static final long CACHE_EXPIRY_HOURS = 24;
    private MutableLiveData<String> error = new MutableLiveData<>();

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public MutableLiveData<List<Chapter>> getChapters(Novel novel) {

        MutableLiveData<List<Chapter>> chapters = new MutableLiveData<>();

        RanobeDatabase.databaseExecutor.execute(() -> {

            List<ChapterMetadata> cachedMetadata = RanobeDatabase.database().chapterMetadata().listByUrl(novel.url);
            boolean hasCache = cachedMetadata != null && !cachedMetadata.isEmpty();
            boolean cacheExpired = hasCache && isCacheExpired(cachedMetadata.get(0).cachedDate);

            if (!hasCache || cacheExpired) {
                // Cache miss or expired → fetch from network
                fetchFromNetwork(novel, chapters, error, hasCache ? cachedMetadata : null);
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
                                  List<ChapterMetadata> staleCache) {
        new Repository(novel.sourceId).chapters(novel, new Repository.Callback<List<Chapter>>() {
            @Override
            public void onComplete(List<Chapter> result) {
                // an empty list is usually a block / error page; keep the cached list instead of wiping it
                if ((result == null || result.isEmpty()) && staleCache != null) {
                    chapters.postValue(ChapterMapper.ToChapterList(staleCache));
                    return;
                }
                List<ChapterMetadata> list = ChapterMapper.ToChapterMetadataList(result);
                // show the list first, persisting a few thousand rows shouldn't delay the UI
                chapters.postValue(result);

                long now = System.currentTimeMillis();
                for (ChapterMetadata item : list) item.cachedDate = now;
                RanobeDatabase.database().runInTransaction(() -> {
                    if (staleCache != null) {
                        RanobeDatabase.database().chapterMetadata().deleteByNovel(novel.url);
                    }
                    RanobeDatabase.database().chapterMetadata().saveAll(list);
                });
            }

            @Override
            public void onError(Exception e) {
                // an expired list is better than nothing when the source is unreachable
                if (staleCache != null) {
                    chapters.postValue(ChapterMapper.ToChapterList(staleCache));
                } else {
                    error.postValue(e.getLocalizedMessage());
                }
            }
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
