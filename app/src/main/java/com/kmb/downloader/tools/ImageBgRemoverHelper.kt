package com.kmb.downloader.tools

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

enum class BgMode {
    TRANSPARENT,
    WHITE,
    DARK_STUDIO,
    BLUE,
    PURPLE,
    BLUR
}

data class SegmentationResult(
    val processedBitmap: Bitmap,
    val alphaMask: FloatArray,
    val maskWidth: Int,
    val maskHeight: Int
)

object ImageBgRemoverHelper {
    private const val TAG = "ImageBgRemoverHelper"

    /**
     * Scales bitmap to safe max dimension for high quality & smooth ML inference.
     */
    fun prepareBitmap(source: Bitmap, maxDim: Int = 1280): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val scale = maxDim.toFloat() / max(w, h).toFloat()
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, nw, nh, true)
    }

    /**
     * Extracts a high-definition segmentation mask using on-device Google ML Kit.
     */
    suspend fun extractSegmentationMask(
        inputBitmap: Bitmap
    ): Result<SegmentationResult> = withContext(Dispatchers.Default) {
        try {
            val scaledBitmap = prepareBitmap(inputBitmap, 1280)
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                // Note: Not enabling raw size mask allows ML Kit to return full-sized mask
                .build()

            val segmenter = Segmentation.getClient(options)
            val image = InputImage.fromBitmap(scaledBitmap, 0)

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
            maskBuffer.rewind()

            val totalPixels = maskWidth * maskHeight
            val floatMask = FloatArray(totalPixels)
            for (i in 0 until totalPixels) {
                if (maskBuffer.hasRemaining()) {
                    floatMask[i] = maskBuffer.getFloat()
                } else {
                    floatMask[i] = 0f
                }
            }

            Result.success(SegmentationResult(scaledBitmap, floatMask, maskWidth, maskHeight))
        } catch (e: Exception) {
            Log.e(TAG, "extractSegmentationMask error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Composes the segmented foreground with the chosen background mode.
     */
    fun composeBackground(
        baseBitmap: Bitmap,
        alphaMask: FloatArray,
        maskWidth: Int,
        maskHeight: Int,
        mode: BgMode
    ): Bitmap {
        val width = baseBitmap.width
        val height = baseBitmap.height
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val inputPixels = IntArray(width * height)
        baseBitmap.getPixels(inputPixels, 0, width, 0, 0, width, height)
        val outputPixels = IntArray(width * height)

        val bgColor = when (mode) {
            BgMode.WHITE -> Color.WHITE
            BgMode.DARK_STUDIO -> Color.rgb(24, 28, 40)
            BgMode.BLUE -> Color.rgb(37, 99, 235)
            BgMode.PURPLE -> Color.rgb(124, 58, 237)
            else -> Color.TRANSPARENT
        }

        val scaleX = maskWidth.toFloat() / width.toFloat()
        val scaleY = maskHeight.toFloat() / height.toFloat()

        for (y in 0 until height) {
            val my = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
            val myOffset = my * maskWidth
            for (x in 0 until width) {
                val index = y * width + x
                val origColor = inputPixels[index]

                val mx = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                val rawConf = alphaMask[myOffset + mx]

                // Bilinear feathering / smoothstep for silky edges:
                // smoothstep(edge0, edge1, x): hermite interpolation between 0 and 1
                val edge0 = 0.30f
                val edge1 = 0.75f
                val t = ((rawConf - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                val alphaFloat = t * t * (3f - 2f * t)
                val alpha = (alphaFloat * 255f).toInt().coerceIn(0, 255)

                if (alpha == 255) {
                    outputPixels[index] = origColor
                } else if (alpha == 0) {
                    outputPixels[index] = if (mode == BgMode.TRANSPARENT) Color.TRANSPARENT else bgColor
                } else {
                    if (mode == BgMode.TRANSPARENT) {
                        val r = Color.red(origColor)
                        val g = Color.green(origColor)
                        val b = Color.blue(origColor)
                        outputPixels[index] = Color.argb(alpha, r, g, b)
                    } else {
                        val factor = alphaFloat
                        val r = (Color.red(origColor) * factor + Color.red(bgColor) * (1f - factor)).toInt()
                        val g = (Color.green(origColor) * factor + Color.green(bgColor) * (1f - factor)).toInt()
                        val b = (Color.blue(origColor) * factor + Color.blue(bgColor) * (1f - factor)).toInt()
                        outputPixels[index] = Color.rgb(r, g, b)
                    }
                }
            }
        }

        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)

        if (mode == BgMode.BLUR) {
            val blurred = blurBitmap(baseBitmap, 18)
            val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(combined)
            canvas.drawBitmap(blurred, 0f, 0f, null)
            canvas.drawBitmap(outputBitmap, 0f, 0f, null)
            return combined
        }

        return outputBitmap
    }

    /**
     * Standalone background removal entry point.
     */
    suspend fun removeBackground(
        context: Context,
        inputBitmap: Bitmap,
        mode: BgMode = BgMode.TRANSPARENT
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        val maskRes = extractSegmentationMask(inputBitmap)
        maskRes.map { seg ->
            composeBackground(seg.processedBitmap, seg.alphaMask, seg.maskWidth, seg.maskHeight, mode)
        }
    }

    private fun blurBitmap(source: Bitmap, radius: Int): Bitmap {
        val smallW = (source.width / 5).coerceAtLeast(1)
        val smallH = (source.height / 5).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(source, smallW, smallH, true)
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
