package com.kmb.downloader.tools

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.random.Random

data class DetectedTextBox(
    val text: String,
    val rect: Rect
)

object ImageInpaintHelper {
    private const val TAG = "ImageInpaintHelper"

    /**
     * Detects all text boxes in an image using on-device ML Kit Text Recognition.
     */
    suspend fun detectText(inputBitmap: Bitmap): Result<List<DetectedTextBox>> = withContext(Dispatchers.Default) {
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(inputBitmap, 0)

            val visionText = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { text -> continuation.resume(text) }
                    .addOnFailureListener { err -> continuation.resumeWithException(err) }
            }

            val list = mutableListOf<DetectedTextBox>()
            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    val box = line.boundingBox
                    if (box != null && box.width() > 5 && box.height() > 5) {
                        list.add(DetectedTextBox(text = line.text, rect = box))
                    }
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Detect text failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Erases given areas (watermarks, text, stickers) using boundary-interpolated inpainting.
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
                val pad = 4
                val left = (box.left - pad).coerceIn(0, width - 1)
                val top = (box.top - pad).coerceIn(0, height - 1)
                val right = (box.right + pad).coerceIn(0, width - 1)
                val bottom = (box.bottom + pad).coerceIn(0, height - 1)

                val boxW = right - left
                val boxH = bottom - top
                if (boxW <= 0 || boxH <= 0) continue

                // Sample boundary pixels
                val topColors = IntArray(boxW) { x -> output.getPixel(left + x, top) }
                val bottomColors = IntArray(boxW) { x -> output.getPixel(left + x, bottom) }
                val leftColors = IntArray(boxH) { y -> output.getPixel(left, top + y) }
                val rightColors = IntArray(boxH) { y -> output.getPixel(right, top + y) }

                for (y in 0 until boxH) {
                    val vFactor = y.toFloat() / boxH.toFloat()
                    for (x in 0 until boxW) {
                        val hFactor = x.toFloat() / boxW.toFloat()

                        val cTop = topColors[x]
                        val cBottom = bottomColors[x]
                        val cLeft = leftColors[y]
                        val cRight = rightColors[y]

                        // 4-way bilinear blend
                        val rH = Color.red(cLeft) * (1 - hFactor) + Color.red(cRight) * hFactor
                        val gH = Color.green(cLeft) * (1 - hFactor) + Color.green(cRight) * hFactor
                        val bH = Color.blue(cLeft) * (1 - hFactor) + Color.blue(cRight) * hFactor

                        val rV = Color.red(cTop) * (1 - vFactor) + Color.red(cBottom) * vFactor
                        val gV = Color.green(cTop) * (1 - vFactor) + Color.green(cBottom) * vFactor
                        val bV = Color.blue(cTop) * (1 - vFactor) + Color.blue(cBottom) * vFactor

                        // Subtle natural noise to prevent flat unnatural appearance
                        val noise = (Random.nextFloat() - 0.5f) * 6f

                        val finalR = ((rH + rV) / 2f + noise).toInt().coerceIn(0, 255)
                        val finalG = ((gH + gV) / 2f + noise).toInt().coerceIn(0, 255)
                        val finalB = ((bH + bV) / 2f + noise).toInt().coerceIn(0, 255)

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
