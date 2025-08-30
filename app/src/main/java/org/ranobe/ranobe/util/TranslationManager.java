package org.ranobe.ranobe.utils;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class TranslationManager {
    private Translator translator = null;

    public boolean initTranslator(String from, String to) {
        try {
            if (translator != null) {
                translator.close();
            }

            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(from)
                    .setTargetLanguage(to)
                    .build();

            translator = Translation.getClient(options);

            Tasks.await(translator.downloadModelIfNeeded());
            return true;
        } catch (Exception e) {
            if (translator != null) {
                translator.close();
            }
            translator = null;
            return false;
        }
    }

    public String translate(String text) {
        try {
            return Tasks.await(translator.translate(text));
        } catch (Exception e) {
            return null;
        }
    }

    public void close() {
        if (translator != null) {
            translator.close();
            translator = null;
        }
    }
}
