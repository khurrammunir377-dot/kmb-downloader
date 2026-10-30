package com.kmb.downloader.tools

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

data class SupportedLanguage(
    val code: String,
    val displayName: String
)

object SpeechTranscriberHelper {
    private const val TAG = "SpeechTranscriberHelper"

    val AVAILABLE_LANGUAGES = listOf(
        SupportedLanguage("auto", "Auto Detect (Device Default)"),
        SupportedLanguage("en-US", "English (US)"),
        SupportedLanguage("en-GB", "English (UK)"),
        SupportedLanguage("ur-PK", "Urdu (اردو)"),
        SupportedLanguage("hi-IN", "Hindi (हिन्दी)"),
        SupportedLanguage("ar-SA", "Arabic (العربية)"),
        SupportedLanguage("es-ES", "Spanish (Español)"),
        SupportedLanguage("fr-FR", "French (Français)"),
        SupportedLanguage("de-DE", "German (Deutsch)"),
        SupportedLanguage("tr-TR", "Turkish (Türkçe)"),
        SupportedLanguage("ru-RU", "Russian (Русский)")
    )

    fun isRecognitionAvailable(context: Context): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Creates an intent configured for speech-to-text with the target language.
     */
    fun createRecognizerIntent(languageCode: String = "auto"): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            if (languageCode != "auto") {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            }
        }
    }
}
