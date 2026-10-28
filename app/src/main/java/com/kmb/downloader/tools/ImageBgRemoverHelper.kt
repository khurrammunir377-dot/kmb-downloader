package com.kmb.downloader.tools

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class BgMode {
    TRANSPARENT,
    WHITE,
    DARK_STUDIO,
    BLUE,
    PURPLE,
    BLUR
}

object ImageBgRemoverHelper {
    private const val TAG = "ImageBgRemoverHelper"

    /**
     * Removes the background from a bitmap using on-device ML Kit selfie/subject segmentation.
     */
    suspend fun removeBackground(
        context: Context,
        inputBitmap: Bitmap,
        mode: BgMode = BgMode.TRANSPARENT
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .enableRawSizeMask()
                .build()

            val segmenter = Segmentation.getClient(options)
            val image = InputImage.fromBitmap(inputBitmap, 0)

            val mask = suspendCancellableCoroutine { continuation ->
                segmenter.process(image)
                    .addOnSuccessListener { segmentationMask ->
                        continuation.resume(segmentationMask)
                    }
                    .addOnFailureListener { e ->
                        continuation.resumeWithException(e)
                    }
            }

            val maskBuffer: ByteBuffer = mask.buffer
            val maskWidth = mask.width
            val maskHeight = mask.height

            val width = inputBitmap.width
            val height = inputBitmap.height

            val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val inputPixels = IntArray(width * height)
            inputBitmap.getPixels(inputPixels, 0, width, 0, 0, width, height)

            val outputPixels = IntArray(width * height)
            maskBuffer.rewind()

            // Pre-calculate background color if solid
            val bgColor = when (mode) {
                BgMode.WHITE -> Color.WHITE
                BgMode.DARK_STUDIO -> Color.rgb(30, 30, 35)
                BgMode.BLUE -> Color.rgb(37, 99, 235)
                BgMode.PURPLE -> Color.rgb(124, 58, 237)
                else -> Color.TRANSPARENT
            }

            val scaleX = maskWidth.toFloat() / width.toFloat()
            val scaleY = maskHeight.toFloat() / height.toFloat()

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val index = y * width + x
                    val origColor = inputPixels[index]

                    val mx = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                    val my = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
                    val maskIndex = (my * maskWidth + mx) * 4 // 4 bytes per float
                    val confidence = if (maskIndex + 3 < maskBuffer.capacity()) {
                        maskBuffer.getFloat(maskIndex)
                    } else 0f

                    if (confidence > 0.65f) {
                        // High confidence subject
                        outputPixels[index] = origColor
                    } else if (confidence > 0.25f) {
                        // Soft boundary antialiasing
                        val alpha = ((confidence - 0.25f) / 0.40f * 255).toInt().coerceIn(0, 255)
                        if (mode == BgMode.TRANSPARENT) {
                            val r = Color.red(origColor)
                            val g = Color.green(origColor)
                            val b = Color.blue(origColor)
                            outputPixels[index] = Color.argb(alpha, r, g, b)
                        } else {
                            val factor = alpha / 255f
                            val r = (Color.red(origColor) * factor + Color.red(bgColor) * (1 - factor)).toInt()
                            val g = (Color.green(origColor) * factor + Color.green(bgColor) * (1 - factor)).toInt()
                            val b = (Color.blue(origColor) * factor + Color.blue(bgColor) * (1 - factor)).toInt()
                            outputPixels[index] = Color.rgb(r, g, b)
                        }
                    } else {
                        // Background
                        outputPixels[index] = if (mode == BgMode.TRANSPARENT) Color.TRANSPARENT else bgColor
                    }
                }
            }

            outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

            if (mode == BgMode.BLUR) {
                val blurred = blurBitmap(inputBitmap, 15)
                val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(combined)
                canvas.drawBitmap(blurred, 0f, 0f, null)
                canvas.drawBitmap(outputBitmap, 0f, 0f, null)
                Result.success(combined)
            } else {
                Result.success(outputBitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Background removal error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun blurBitmap(source: Bitmap, radius: Int): Bitmap {
        val small = Bitmap.createScaledBitmap(source, (source.width / 4).coerceAtLeast(1), (source.height / 4).coerceAtLeast(1), true)
        return Bitmap.createScaledBitmap(small, source.width, source.height, true)
    }

    /**
     * Saves processed image into Android MediaStore Pictures/KMB AI Studio.
     */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap, title: String): Result<Uri> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = "KMB_AI_${System.currentTimeMillis()}_$title.png"

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/KMB AI Studio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values)
            ?: return@withContext Result.failure(Exception("Failed to create MediaStore entry"))

        try {
            resolver.openOutputStream(uri)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            Result.success(uri)
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            Result.failure(e)
        }
    }
}
