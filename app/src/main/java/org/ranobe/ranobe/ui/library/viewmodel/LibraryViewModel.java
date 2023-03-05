package org.ranobe.ranobe.ui.library.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.RanobeDatabase;
import org.ranobe.ranobe.models.Novel;

import java.util.List;

public class LibraryViewModel extends ViewModel {
    public LiveData<List<Novel>> list() {
        return RanobeDatabase.database().novels().list();
    }

    public MutableLiveData<String> deleteNovel(long novelId) {
        MutableLiveData<String> message = new MutableLiveData<>();
        RanobeDatabase.databaseExecutor.execute(() -> {
            int rows = RanobeDatabase.database().novels().delete(novelId);
            String msg = rows > 0
                    ? "Novel removed from your library successfully"
                    : "Failed to remove your novel";
            message.postValue(msg);
        });
        return message;
    }
}
