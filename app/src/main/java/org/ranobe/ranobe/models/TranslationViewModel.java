package org.ranobe.ranobe.models;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.ranobe.ranobe.database.MLSettings;
import org.ranobe.ranobe.utils.TranslationManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TranslationViewModel extends ViewModel {
    private final TranslationManager translationManager = new TranslationManager();

    private final MutableLiveData<String> translatedText = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LiveData<String> getTranslatedText() { return translatedText; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }

    public void initTranslation(final String fromLang, final String toLang) {
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                MLSettings settings = new MLSettings(fromLang, toLang);
                if (settings.isValid() && translationManager.initTranslator(fromLang, toLang)) {
                    error.postValue(null);
                } else {
                    error.postValue("Error initializing translator");
                }
            } catch (Exception e) {
                error.postValue(e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    public void translateText(final String text) {
        isLoading.postValue(true);
        executor.execute(() -> {
            try {
                String result = translationManager.translate(text);
                if (result != null) {
                    translatedText.postValue(result);
                    error.postValue(null);
                } else {
                    error.postValue("Translation failed");
                }
            } catch (Exception e) {
                error.postValue(e.getMessage());
            } finally {
                isLoading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        translationManager.close();
        executor.shutdown();
    }
}
