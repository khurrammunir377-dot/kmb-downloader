package com.kmb.downloader.tools

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
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
import kotlin.math.min
import kotlin.random.Random

data class DetectedTextBox(
    val text: String,
    val rect: Rect
)

object ImageInpaintHelper {
    private const val TAG = "ImageInpaintHelper"

    /**
     * Detects all text boxes in an image across multiple scripts (Latin, Devanagari, Chinese, Japanese, Korean).
     */
    suspend fun detectText(inputBitmap: Bitmap): Result<List<DetectedTextBox>> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(inputBitmap, 0)
            val list = mutableListOf<DetectedTextBox>()

            suspend fun runRecognizer(recognizer: com.google.mlkit.vision.text.TextRecognizer) {
                try {
                    val result: Text? = suspendCancellableCoroutine { continuation ->
                        recognizer.process(image)
                            .addOnSuccessListener { continuation.resume(it) }
                            .addOnFailureListener { continuation.resume(null) }
                    }
                    result?.let { res ->
                        for (block in res.textBlocks) {
                            for (line in block.lines) {
                                val box = line.boundingBox
                                if (box != null && box.width() > 6 && box.height() > 6) {
                                    list.add(DetectedTextBox(text = line.text, rect = box))
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 1. Devanagari (Hindi, Nepali, Marathi)
            val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            runRecognizer(devanagariRecognizer)

            // 2. Latin (English, numbers, handles)
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

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Detect text failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Erases text and logos using seamless boundary texture diffusion.
     * Reconstructs pixels from immediate adjacent background borders without covering or blurring.
     */
    suspend fun eraseAreas(
        inputBitmap: Bitmap,
        boxesToErase: List<Rect>
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val width = inputBitmap.width
            val height = inputBitmap.height
            val output = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)

            for (box in boxesToErase) {
                // Tight padding to only encompass text stroke and outline
                val padX = 2
                val padY = 2
                val left = (box.left - padX).coerceIn(0, width - 1)
                val top = (box.top - padY).coerceIn(0, height - 1)
                val right = (box.right + padX).coerceIn(0, width - 1)
                val bottom = (box.bottom + padY).coerceIn(0, height - 1)

                val boxW = right - left
                val boxH = bottom - top
                if (boxW <= 0 || boxH <= 0) continue

                val topBoundaryY = (top - 1).coerceAtLeast(0)
                val bottomBoundaryY = (bottom + 1).coerceAtMost(height - 1)
                val leftBoundaryX = (left - 1).coerceAtLeast(0)
                val rightBoundaryX = (right + 1).coerceAtMost(width - 1)

                // Cache boundary pixels
                val topColors = IntArray(boxW) { x -> output.getPixel(left + x, topBoundaryY) }
                val bottomColors = IntArray(boxW) { x -> output.getPixel(left + x, bottomBoundaryY) }
                val leftColors = IntArray(boxH) { y -> output.getPixel(leftBoundaryX, top + y) }
                val rightColors = IntArray(boxH) { y -> output.getPixel(rightBoundaryX, top + y) }

                for (y in 0 until boxH) {
                    val vFactor = (y + 1).toFloat() / (boxH + 1).toFloat()
                    val cLeft = leftColors[y]
                    val cRight = rightColors[y]

                    for (x in 0 until boxW) {
                        val hFactor = (x + 1).toFloat() / (boxW + 1).toFloat()
                        val cTop = topColors[x]
                        val cBottom = bottomColors[x]

                        // Text lines are predominantly horizontal, so vertical texture continuation is primary
                        val rV = Color.red(cTop) * (1f - vFactor) + Color.red(cBottom) * vFactor
                        val gV = Color.green(cTop) * (1f - vFactor) + Color.green(cBottom) * vFactor
                        val bV = Color.blue(cTop) * (1f - vFactor) + Color.blue(cBottom) * vFactor

                        val rH = Color.red(cLeft) * (1f - hFactor) + Color.red(cRight) * hFactor
                        val gH = Color.green(cLeft) * (1f - hFactor) + Color.green(cRight) * hFactor
                        val bH = Color.blue(cLeft) * (1f - hFactor) + Color.blue(cRight) * hFactor

                        // Distance to borders weighting
                        val distV = min(y, boxH - y).toFloat() + 1f
                        val distH = min(x, boxW - x).toFloat() + 1f
                        val weightV = (1f / distV) * 2.5f
                        val weightH = (1f / distH)
                        val totalW = weightV + weightH

                        val r = (rV * weightV + rH * weightH) / totalW
                        val g = (gV * weightV + gH * weightH) / totalW
                        val b = (bV * weightV + bH * weightH) / totalW

                        // Natural subtle noise perturbation
                        val noise = (Random.nextFloat() - 0.5f) * 2f

                        val finalR = (r + noise).toInt().coerceIn(0, 255)
                        val finalG = (g + noise).toInt().coerceIn(0, 255)
                        val finalB = (b + noise).toInt().coerceIn(0, 255)

                        output.setPixel(left + x, top + y, Color.rgb(finalR, finalG, finalB))
                    }
                }
            }

            Result.success(output)
        } catch (e: Exception) {
            Log.e(TAG, "Inpaint erase failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}
