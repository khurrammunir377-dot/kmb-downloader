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
    BLUR,
    WHITE,
    DARK_STUDIO,
    BLUE,
    PURPLE
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
     * Raw size mask is enabled so the mask output matches exact bitmap resolution.
     */
    suspend fun extractSegmentationMask(
        inputBitmap: Bitmap
    ): Result<SegmentationResult> = withContext(Dispatchers.Default) {
        try {
            val scaledBitmap = prepareBitmap(inputBitmap, 1280)
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .enableRawSizeMask()
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
     * Features smooth bilateral feathering and genuine portrait bokeh blur.
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

        val blurredBitmap = if (mode == BgMode.BLUR) createBokehBlur(baseBitmap, 16) else null
        val blurPixels = if (blurredBitmap != null) {
            val bp = IntArray(width * height)
            blurredBitmap.getPixels(bp, 0, width, 0, 0, width, height)
            bp
        } else null

        val bgColor = when (mode) {
            BgMode.WHITE -> Color.WHITE
            BgMode.DARK_STUDIO -> Color.rgb(20, 24, 34)
            BgMode.BLUE -> Color.rgb(37, 99, 235)
            BgMode.PURPLE -> Color.rgb(124, 58, 237)
            else -> Color.TRANSPARENT
        }

        val scaleX = maskWidth.toFloat() / width.toFloat()
        val scaleY = maskHeight.toFloat() / height.toFloat()

        for (y in 0 until height) {
            val my = (y * scaleY).toInt().coerceIn(0, maskHeight - 1)
            val myOffset = my * maskWidth
            val rowOffset = y * width

            for (x in 0 until width) {
                val index = rowOffset + x
                val origColor = inputPixels[index]

                val mx = (x * scaleX).toInt().coerceIn(0, maskWidth - 1)
                val rawConf = alphaMask[myOffset + mx]

                // Ultra-smooth sigmoidal feathering for professional edges without harsh cutouts:
                val edge0 = 0.25f
                val edge1 = 0.75f
                val t = ((rawConf - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
                val alphaFloat = t * t * (3f - 2f * t)

                when (mode) {
                    BgMode.TRANSPARENT -> {
                        val alphaInt = (alphaFloat * 255f).toInt().coerceIn(0, 255)
                        if (alphaInt == 0) {
                            outputPixels[index] = Color.TRANSPARENT
                        } else {
                            val r = Color.red(origColor)
                            val g = Color.green(origColor)
                            val b = Color.blue(origColor)
                            outputPixels[index] = Color.argb(alphaInt, r, g, b)
                        }
                    }
                    BgMode.BLUR -> {
                        val blurColor = blurPixels?.get(index) ?: origColor
                        val r = (Color.red(origColor) * alphaFloat + Color.red(blurColor) * (1f - alphaFloat)).toInt().coerceIn(0, 255)
                        val g = (Color.green(origColor) * alphaFloat + Color.green(blurColor) * (1f - alphaFloat)).toInt().coerceIn(0, 255)
                        val b = (Color.blue(origColor) * alphaFloat + Color.blue(blurColor) * (1f - alphaFloat)).toInt().coerceIn(0, 255)
                        outputPixels[index] = Color.rgb(r, g, b)
                    }
                    else -> {
                        // Solid Studio Backgrounds
                        val r = (Color.red(origColor) * alphaFloat + Color.red(bgColor) * (1f - alphaFloat)).toInt().coerceIn(0, 255)
                        val g = (Color.green(origColor) * alphaFloat + Color.green(bgColor) * (1f - alphaFloat)).toInt().coerceIn(0, 255)
                        val b = (Color.blue(origColor) * alphaFloat + Color.blue(bgColor) * (1f - alphaFloat)).toInt().coerceIn(0, 255)
                        outputPixels[index] = Color.rgb(r, g, b)
                    }
                }
            }
        }

        outputBitmap.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return outputBitmap
    }

    /**
     * Creates a high-fidelity camera bokeh blur for portrait background.
     */
    private fun createBokehBlur(source: Bitmap, radius: Int = 16): Bitmap {
        val width = source.width
        val height = source.height

        // Downsample slightly for soft optical blur
        val downW = (width / 4).coerceAtLeast(1)
        val downH = (height / 4).coerceAtLeast(1)
        val small = Bitmap.createScaledBitmap(source, downW, downH, true)

        val pixels = IntArray(downW * downH)
        small.getPixels(pixels, 0, downW, 0, 0, downW, downH)

        val r = radius.coerceIn(4, 25)
        val blurredPixels = boxBlurPass(pixels, downW, downH, r)
        small.setPixels(blurredPixels, 0, downW, 0, 0, downW, downH)

        return Bitmap.createScaledBitmap(small, width, height, true)
    }

    private fun boxBlurPass(src: IntArray, w: Int, h: Int, r: Int): IntArray {
        val dst = IntArray(w * h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = r + r + 1

        val rAcc = IntArray(wh)
        val gAcc = IntArray(wh)
        val bAcc = IntArray(wh)

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var p: Int
        var yi = 0
        var yw = 0

        for (y in 0 until h) {
            rsum = 0
            gsum = 0
            bsum = 0
            for (i in -r..r) {
                p = src[yi + i.coerceIn(0, wm)]
                rsum += (p ushr 16) and 0xff
                gsum += (p ushr 8) and 0xff
                bsum += p and 0xff
            }
            for (x in 0 until w) {
                rAcc[yi] = rsum / div
                gAcc[yi] = gsum / div
                bAcc[yi] = bsum / div

                val p1 = src[yw + (x + r + 1).coerceAtMost(wm)]
                val p2 = src[yw + (x - r).coerceAtLeast(0)]

                rsum += ((p1 ushr 16) and 0xff) - ((p2 ushr 16) and 0xff)
                gsum += ((p1 ushr 8) and 0xff) - ((p2 ushr 8) and 0xff)
                bsum += (p1 and 0xff) - (p2 and 0xff)
                yi++
            }
            yw += w
        }

        for (x in 0 until w) {
            rsum = 0
            gsum = 0
            bsum = 0
            var yp = -r * w
            for (i in -r..r) {
                yi = (yp + x).coerceIn(0, wh - 1)
                rsum += rAcc[yi]
                gsum += gAcc[yi]
                bsum += bAcc[yi]
                yp += w
            }
            yi = x
            for (y in 0 until h) {
                dst[yi] = (0xff shl 24) or ((rsum / div) shl 16) or ((gsum / div) shl 8) or (bsum / div)
                val y1 = (y + r + 1).coerceAtMost(hm) * w + x
                val y2 = (y - r).coerceAtLeast(0) * w + x
                rsum += rAcc[y1] - rAcc[y2]
                gsum += gAcc[y1] - gAcc[y2]
                bsum += bAcc[y1] - bAcc[y2]
                yi += w
            }
        }
        return dst
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
