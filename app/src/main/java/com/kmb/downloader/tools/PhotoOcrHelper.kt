package com.kmb.downloader.tools

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(
    val fullText: String,
    val blockCount: Int,
    val lineCount: Int
)

data class TranslationLanguage(
    val code: String,
    val displayName: String
)

object PhotoOcrHelper {
    private const val TAG = "PhotoOcrHelper"

    val TRANSLATION_LANGUAGES = listOf(
        TranslationLanguage(TranslateLanguage.ENGLISH, "English"),
        TranslationLanguage(TranslateLanguage.URDU, "Urdu (اردو)"),
        TranslationLanguage(TranslateLanguage.HINDI, "Hindi (हिन्दी)"),
        TranslationLanguage(TranslateLanguage.ARABIC, "Arabic (العربية)"),
        TranslationLanguage(TranslateLanguage.SPANISH, "Spanish (Español)"),
        TranslationLanguage(TranslateLanguage.FRENCH, "French (Français)"),
        TranslationLanguage(TranslateLanguage.GERMAN, "German (Deutsch)"),
        TranslationLanguage(TranslateLanguage.CHINESE, "Chinese (中文)"),
        TranslationLanguage(TranslateLanguage.JAPANESE, "Japanese (日本語)"),
        TranslationLanguage(TranslateLanguage.KOREAN, "Korean (한국어)"),
        TranslationLanguage(TranslateLanguage.RUSSIAN, "Russian (Русский)"),
        TranslationLanguage(TranslateLanguage.TURKISH, "Turkish (Türkçe)"),
        TranslationLanguage(TranslateLanguage.ITALIAN, "Italian (Italiano)"),
        TranslationLanguage(TranslateLanguage.PORTUGUESE, "Portuguese (Português)")
    )

    /**
     * Extracts all text (Latin, Devanagari, Chinese, Japanese, Korean) from a bitmap.
     */
    suspend fun recognizeText(
        context: Context,
        bitmap: Bitmap
    ): Result<OcrResult> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val combinedLines = linkedSetOf<String>()
            var blockCount = 0

            suspend fun runRecognizer(recognizer: com.google.mlkit.vision.text.TextRecognizer) {
                try {
                    val result: Text? = suspendCancellableCoroutine { continuation ->
                        recognizer.process(image)
                            .addOnSuccessListener { continuation.resume(it) }
                            .addOnFailureListener { continuation.resume(null) }
                    }
                    result?.let { res ->
                        blockCount += res.textBlocks.size
                        for (block in res.textBlocks) {
                            for (line in block.lines) {
                                val trimmed = line.text.trim()
                                if (trimmed.isNotBlank()) combinedLines.add(trimmed)
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1. Devanagari (Hindi, Nepali, Marathi, Sanskrit)
            val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            runRecognizer(devanagariRecognizer)

            // 2. Latin (English, Urdu transliterated, numbers, usernames)
            val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            runRecognizer(latinRecognizer)

            // 3. Chinese
            try {
                val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                runRecognizer(chineseRecognizer)
            } catch (_: Exception) {}

            // 4. Japanese
            try {
                val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                runRecognizer(japaneseRecognizer)
            } catch (_: Exception) {}

            // 5. Korean
            try {
                val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                runRecognizer(koreanRecognizer)
            } catch (_: Exception) {}

            val fullText = combinedLines.joinToString("\n")

            Result.success(
                OcrResult(
                    fullText = fullText,
                    blockCount = blockCount,
                    lineCount = combinedLines.size
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognition error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Translates text to the selected target language using on-device ML Kit Translation.
     */
    suspend fun translateText(
        text: String,
        targetLanguageCode: String,
        sourceLanguageCode: String = TranslateLanguage.ENGLISH
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")
        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build()

            val translator = Translation.getClient(options)
            val conditions = DownloadConditions.Builder().build()
            suspendCancellableCoroutine { continuation ->
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }

            val translated: String = suspendCancellableCoroutine { continuation ->
                translator.translate(text)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
            Result.success(translated)
        } catch (e: Exception) {
            Log.w(TAG, "Translation error: ${e.message}")
            Result.failure(e)
        }
    }
}
