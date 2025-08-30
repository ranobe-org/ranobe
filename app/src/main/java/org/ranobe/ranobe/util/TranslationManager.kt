package org.ranobe.ranobe.utils

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslationManager {
    private var translator: Translator? = null

    suspend fun initTranslator(from: String, to: String): Boolean {
        try {
            translator?.close()
            
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(from)
                .setTargetLanguage(to)
                .build()
            
            translator = Translation.getClient(options)
            
            Tasks.await(translator?.downloadModelIfNeeded())
            return true
        } catch (e: Exception) {
            translator?.close()
            translator = null
            return false
        }
    }

    suspend fun translate(text: String): String? {
        return try {
            Tasks.await(translator?.translate(text))
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        translator?.close()
        translator = null
    }
}