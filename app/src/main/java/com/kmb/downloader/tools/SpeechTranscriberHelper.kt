package com.kmb.downloader.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

data class SupportedLanguage(
    val code: String,
    val displayName: String
)

object SpeechTranscriberHelper {
    private const val TAG = "SpeechTranscriberHelper"

    val AVAILABLE_LANGUAGES = listOf(
        SupportedLanguage("auto", "Auto Detect (Device Default)"),
        SupportedLanguage("ur-PK", "Urdu (اردو)"),
        SupportedLanguage("hi-IN", "Hindi (हिन्दी)"),
        SupportedLanguage("en-US", "English (US)"),
        SupportedLanguage("en-GB", "English (UK)"),
        SupportedLanguage("ar-SA", "Arabic (العربية)"),
        SupportedLanguage("es-ES", "Spanish (Español)"),
        SupportedLanguage("fr-FR", "French (Français)"),
        SupportedLanguage("de-DE", "German (Deutsch)"),
        SupportedLanguage("tr-TR", "Turkish (Türkçe)"),
        SupportedLanguage("ru-RU", "Russian (Русский)"),
        SupportedLanguage("zh-CN", "Chinese (中文)"),
        SupportedLanguage("ja-JP", "Japanese (日本語)")
    )

    fun isRecognitionAvailable(context: Context): Boolean {
        if (SpeechRecognizer.isRecognitionAvailable(context)) return true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val list = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.isNotEmpty()
    }

    /**
     * Creates an intent configured for speech-to-text with the target language.
     */
    fun createRecognizerIntent(languageCode: String = "auto", prompt: String = "Listening... Speak now"): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            if (languageCode != "auto") {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
            }
        }
    }
}
