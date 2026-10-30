package com.kmb.downloader.tools

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
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
     * Extracts all text from a bitmap using on-device Google ML Kit Text Recognition.
     */
    suspend fun recognizeText(
        context: Context,
        bitmap: Bitmap
    ): Result<OcrResult> = withContext(Dispatchers.Default) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            val visionText = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        continuation.resume(result)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            val text = visionText.text.trim()
            var lines = 0
            for (b in visionText.textBlocks) {
                lines += b.lines.size
            }

            Result.success(
                OcrResult(
                    fullText = text,
                    blockCount = visionText.textBlocks.size,
                    lineCount = lines
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognition error: ${e.message}", e)
            Result.failure(e)
        }
    }
}
