package org.ranobe.ranobe.ui.settings.viewmodel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.network.repository.GithubRepo;

public class SettingsViewModel extends ViewModel {
    private MutableLiveData<GithubRepo.GithubRelease> update;

    public MutableLiveData<GithubRepo.GithubRelease> getUpdate() {
        if (update == null) {
            update = new MutableLiveData<>();
        }
        return update;
    }

    public void checkForUpdate() {
        new GithubRepo().getLatestRelease(new GithubRepo.Callback<GithubRepo.GithubRelease>() {
            @Override
            public void onComplete(GithubRepo.GithubRelease result) {
                update.postValue(result);
            }

            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }
}
