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
    val lineCount: Int,
    val detectedLanguage: String = "en"
)

data class TranslationLanguage(
    val code: String,
    val displayName: String
)

data class OcrScriptOption(
    val id: String,
    val displayName: String,
    val description: String
)

object PhotoOcrHelper {
    private const val TAG = "PhotoOcrHelper"

    val SCRIPT_OPTIONS = listOf(
        OcrScriptOption("auto", "Auto (All Scripts)", "Scans Hindi, English, Urdu, Chinese, Japanese, Korean"),
        OcrScriptOption("devanagari", "Hindi / Devanagari (हिन्दी)", "Devanagari: Hindi, Nepali, Marathi"),
        OcrScriptOption("latin", "English / Latin", "English, Spanish, French, German, Roman Urdu"),
        OcrScriptOption("chinese", "Chinese (中文)", "Simplified and Traditional Chinese characters"),
        OcrScriptOption("japanese", "Japanese (日本語)", "Kanji, Hiragana, Katakana"),
        OcrScriptOption("korean", "Korean (한국어)", "Hangul characters")
    )

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
     * Inspects Unicode character blocks to detect the primary script / language
     * of the extracted text.
     */
    fun detectSourceLanguage(text: String): String {
        var devanagariCount = 0
        var arabicCount = 0
        var cjkCount = 0
        var hangulCount = 0
        var latinCount = 0

        for (ch in text) {
            val code = ch.code
            when {
                code in 0x0900..0x097F -> devanagariCount++
                code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF -> arabicCount++
                code in 0x4E00..0x9FFF || code in 0x3400..0x4DBF -> cjkCount++
                code in 0xAC00..0xD7AF || code in 0x1100..0x11FF -> hangulCount++
                code in 0x0041..0x005A || code in 0x0061..0x007A -> latinCount++
            }
        }

        val max = maxOf(devanagariCount, arabicCount, cjkCount, hangulCount, latinCount)
        return when {
            max == 0 -> TranslateLanguage.ENGLISH
            max == devanagariCount -> TranslateLanguage.HINDI
            max == arabicCount -> TranslateLanguage.URDU
            max == cjkCount -> TranslateLanguage.CHINESE
            max == hangulCount -> TranslateLanguage.KOREAN
            else -> TranslateLanguage.ENGLISH
        }
    }

    /**
     * Extracts text from bitmap with support for all major scripts:
     * Devanagari (Hindi, Nepali, Marathi), Latin (English, Urdu), Chinese, Japanese, Korean.
     */
    suspend fun recognizeText(
        context: Context,
        bitmap: Bitmap,
        scriptId: String = "auto"
    ): Result<OcrResult> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val combinedLines = linkedSetOf<String>()
            var blockCount = 0
            var downloadPendingError: String? = null

            suspend fun runRecognizer(recognizer: com.google.mlkit.vision.text.TextRecognizer) {
                try {
                    val result: Text? = suspendCancellableCoroutine { continuation ->
                        recognizer.process(image)
                            .addOnSuccessListener { continuation.resume(it) }
                            .addOnFailureListener { e ->
                                val msg = e.message ?: ""
                                if (msg.contains("Waiting for the", ignoreCase = true) || msg.contains("download", ignoreCase = true)) {
                                    downloadPendingError = "Google Play Services is downloading OCR model. Please ensure Wi-Fi/data is active and retry in 10s."
                                }
                                Log.w(TAG, "Recognizer error: $msg")
                                continuation.resume(null)
                            }
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
                } catch (e: Exception) {
                    Log.w(TAG, "runRecognizer exception: ${e.message}")
                }
            }

            when (scriptId) {
                "devanagari" -> {
                    val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                    runRecognizer(devanagariRecognizer)
                    // Fallback to latin for numbers and brand names
                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    runRecognizer(latinRecognizer)
                }
                "latin" -> {
                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    runRecognizer(latinRecognizer)
                }
                "chinese" -> {
                    val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    runRecognizer(chineseRecognizer)
                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    runRecognizer(latinRecognizer)
                }
                "japanese" -> {
                    val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                    runRecognizer(japaneseRecognizer)
                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    runRecognizer(latinRecognizer)
                }
                "korean" -> {
                    val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                    runRecognizer(koreanRecognizer)
                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    runRecognizer(latinRecognizer)
                }
                else -> {
                    // Auto: Run Devanagari first (Hindi/Nepali), then Latin, then East Asian
                    val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                    runRecognizer(devanagariRecognizer)

                    val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    runRecognizer(latinRecognizer)

                    try {
                        val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                        runRecognizer(chineseRecognizer)
                    } catch (_: Exception) {}

                    try {
                        val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                        runRecognizer(japaneseRecognizer)
                    } catch (_: Exception) {}

                    try {
                        val koreanRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                        runRecognizer(koreanRecognizer)
                    } catch (_: Exception) {}
                }
            }

            val fullText = combinedLines.joinToString("\n")
            if (fullText.isBlank() && downloadPendingError != null) {
                return@withContext Result.failure(Exception(downloadPendingError))
            }

            val detectedLang = detectSourceLanguage(fullText)

            Result.success(
                OcrResult(
                    fullText = fullText,
                    blockCount = blockCount,
                    lineCount = combinedLines.size,
                    detectedLanguage = detectedLang
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognition error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Translates text to the selected target language using on-device ML Kit Translation.
     * Automatically handles source language detection if not specified.
     */
    suspend fun translateText(
        text: String,
        targetLanguageCode: String,
        sourceLanguageCode: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.success("")

        val resolvedSource = sourceLanguageCode ?: detectSourceLanguage(text)

        // If source and target are identical, return the original text directly
        if (resolvedSource.equals(targetLanguageCode, ignoreCase = true)) {
            return@withContext Result.success(text)
        }

        try {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(resolvedSource)
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
            Log.w(TAG, "Translation error from $resolvedSource to $targetLanguageCode: ${e.message}")
            Result.failure(e)
        }
    }
}
