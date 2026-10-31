package com.kmb.downloader.tools

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
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

object PhotoOcrHelper {
    private const val TAG = "PhotoOcrHelper"

    /**
     * Extracts all text (Latin + Devanagari / Nepali / Hindi) from a bitmap using on-device Google ML Kit.
     */
    suspend fun recognizeText(
        context: Context,
        bitmap: Bitmap
    ): Result<OcrResult> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)

            // 1. Latin Text Recognizer (English, numbers, symbols)
            val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val latinResult: Text? = try {
                suspendCancellableCoroutine { continuation ->
                    latinRecognizer.process(image)
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resume(null) }
                }
            } catch (_: Exception) { null }

            // 2. Devanagari Text Recognizer (Nepali, Hindi, Marathi, Sanskrit)
            val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            val devanagariResult: Text? = try {
                suspendCancellableCoroutine { continuation ->
                    devanagariRecognizer.process(image)
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resume(null) }
                }
            } catch (_: Exception) { null }

            val combinedLines = linkedSetOf<String>()
            var blockCount = 0

            devanagariResult?.let { res ->
                blockCount += res.textBlocks.size
                for (block in res.textBlocks) {
                    for (line in block.lines) {
                        val trimmed = line.text.trim()
                        if (trimmed.isNotBlank()) combinedLines.add(trimmed)
                    }
                }
            }

            latinResult?.let { res ->
                blockCount += res.textBlocks.size
                for (block in res.textBlocks) {
                    for (line in block.lines) {
                        val trimmed = line.text.trim()
                        if (trimmed.isNotBlank()) combinedLines.add(trimmed)
                    }
                }
            }

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
}
