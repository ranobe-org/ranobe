package org.ranobe.ranobe.ui.details.viewmodel;

import android.os.Parcel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.database.mapper.NovelMapper;
import org.ranobe.ranobe.models.Novel;
import org.ranobe.ranobe.models.NovelMetadata;
import org.ranobe.ranobe.network.repository.Repository;

public class DetailsViewModel extends ViewModel {
    private static final long CACHE_EXPIRY_HOURS = 24;
    private MutableLiveData<String> error = new MutableLiveData<>();

    private static Novel copy(Novel novel) {
        Parcel parcel = Parcel.obtain();
        try {
            novel.writeToParcel(parcel, 0);
            parcel.setDataPosition(0);
            return Novel.CREATOR.createFromParcel(parcel);
        } finally {
            parcel.recycle();
        }
    }

    public MutableLiveData<String> getError() {
        return error = new MutableLiveData<>();
    }

    public LiveData<Novel> getDetails(Novel novel) {
        MutableLiveData<Novel> details = new MutableLiveData<>();

        RanobeDatabase.databaseExecutor.execute(() -> {
            NovelMetadata cachedMetadata = RanobeDatabase.database()
                    .novelMetadata()
                    .get(novel.url);

            if (cachedMetadata == null || isCacheExpired(cachedMetadata.cachedDate)) {
                fetchFromNetwork(novel, details, error, cachedMetadata);
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
                                  NovelMetadata staleCache) {
        // sources fill in the novel they're given on a worker thread; don't hand them the
        // instance the details page is showing
        new Repository(novel.sourceId).details(copy(novel), new Repository.Callback<Novel>() {
            @Override
            public void onComplete(Novel result) {
                // a block / error page parses into a nameless novel; don't let it replace or pin a good copy
                if (result == null || result.name == null || result.name.trim().isEmpty()) {
                    if (staleCache != null) details.postValue(NovelMapper.ToNovel(staleCache));
                    else if (result != null) details.postValue(result);
                    else error.postValue("Couldn't load novel details");
                    return;
                }
                NovelMetadata newMetadata = NovelMapper.ToNovelMetadata(result);
                details.postValue(result);

                // Update cache
                newMetadata.cachedDate = System.currentTimeMillis();
                RanobeDatabase.database().runInTransaction(() -> {
                    if (staleCache != null) {
                        RanobeDatabase.database().novelMetadata().delete(novel.url);
                    }
                    RanobeDatabase.database().novelMetadata().save(newMetadata);
                });
            }

            @Override
            public void onError(Exception e) {
                // an expired copy is better than nothing when the source is unreachable
                if (staleCache != null) {
                    details.postValue(NovelMapper.ToNovel(staleCache));
                } else {
                    error.postValue(e.getLocalizedMessage());
                }
            }
        });
    }
}
