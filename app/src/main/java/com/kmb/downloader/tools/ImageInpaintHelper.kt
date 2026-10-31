package com.kmb.downloader.tools

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
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
import kotlin.random.Random

data class DetectedTextBox(
    val text: String,
    val rect: Rect
)

object ImageInpaintHelper {
    private const val TAG = "ImageInpaintHelper"

    /**
     * Detects all text boxes in an image using Latin + Devanagari (Nepali/Hindi) recognition.
     */
    suspend fun detectText(inputBitmap: Bitmap): Result<List<DetectedTextBox>> = withContext(Dispatchers.Default) {
        try {
            val image = InputImage.fromBitmap(inputBitmap, 0)
            val list = mutableListOf<DetectedTextBox>()

            // 1. Devanagari Recognizer (Hindi, Nepali, Marathi, etc.)
            val devanagariRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            val devanagariResult: Text? = try {
                suspendCancellableCoroutine { continuation ->
                    devanagariRecognizer.process(image)
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resume(null) }
                }
            } catch (_: Exception) { null }

            // 2. Latin Recognizer (English, numbers, symbols, usernames)
            val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val latinResult: Text? = try {
                suspendCancellableCoroutine { continuation ->
                    latinRecognizer.process(image)
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resume(null) }
                }
            } catch (_: Exception) { null }

            fun addBoxes(text: Text?) {
                if (text == null) return
                for (block in text.textBlocks) {
                    for (line in block.lines) {
                        val box = line.boundingBox
                        if (box != null && box.width() > 6 && box.height() > 6) {
                            list.add(DetectedTextBox(text = line.text, rect = box))
                        }
                    }
                }
            }

            addBoxes(devanagariResult)
            addBoxes(latinResult)

            Result.success(list)
        } catch (e: Exception) {
            Log.e(TAG, "Detect text failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Generates predefined full-screen text overlay zones (Top captions, Middle handles/captions, Bottom subtitles).
     */
    fun getFullScreenTextZones(width: Int, height: Int): List<Rect> {
        return listOf(
            // Top Captions / Title overlay
            Rect((width * 0.03).toInt(), (height * 0.04).toInt(), (width * 0.97).toInt(), (height * 0.22).toInt()),
            // Middle TikTok logo, handle, and center captions
            Rect((width * 0.02).toInt(), (height * 0.44).toInt(), (width * 0.98).toInt(), (height * 0.65).toInt()),
            // Bottom subtitles
            Rect((width * 0.05).toInt(), (height * 0.78).toInt(), (width * 0.95).toInt(), (height * 0.94).toInt())
        )
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
                val pad = 8 // Generous padding to completely remove letter shadows, outlines & emojis
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
                        val noise = (Random.nextFloat() - 0.5f) * 4f

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
