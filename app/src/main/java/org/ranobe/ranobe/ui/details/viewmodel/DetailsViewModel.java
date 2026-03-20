package org.ranobe.ranobe.ui.details.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.database.mapper.NovelMapper;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelMetadata;
import org.ranobe.ranobe.network.repository.Repository;

public class DetailsViewModel extends ViewModel {
    private MutableLiveData<String> error = new MutableLiveData<>();

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    private static final long CACHE_EXPIRY_HOURS = 24;

    public LiveData<Novel> getDetails(Novel novel) {
        MutableLiveData<Novel> details = new MutableLiveData<>();

        RanobeDatabase.databaseExecutor.execute(() -> {
            NovelMetadata cachedMetadata = RanobeDatabase.database()
                    .novelMetadata()
                    .get(novel.url);

            if (cachedMetadata == null || isCacheExpired(cachedMetadata.cachedDate)) {
                fetchFromNetwork(novel, details, error, cachedMetadata != null);
            } else {
                Novel cachedNovel = NovelMapper.ToNovel(cachedMetadata);
                details.postValue(cachedNovel);
            }
        });

        return details;
    }

    private boolean isCacheExpired(long cachedDate) {
        long expiryTime = cachedDate + (CACHE_EXPIRY_HOURS * 60 * 60 * 1000);
        return System.currentTimeMillis() > expiryTime;
    }

    private void fetchFromNetwork(Novel novel,
                                  MutableLiveData<Novel> details,
                                  MutableLiveData<String> error,
                                  boolean shouldDeleteOldCache) {
        new Repository().details(novel, new Repository.Callback<Novel>() {
            @Override
            public void onComplete(Novel result) {
                // Update cache
                if (shouldDeleteOldCache) {
                    RanobeDatabase.database().novelMetadata().delete(novel.url);
                }
                NovelMetadata newMetadata = NovelMapper.ToNovelMetadata(result);
                newMetadata.cachedDate = System.currentTimeMillis();
                RanobeDatabase.database().novelMetadata().save(newMetadata);

                details.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                error.postValue(e.getLocalizedMessage());
            }
        });
    }
}
