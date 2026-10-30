package com.kmb.downloader.tools

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import kotlin.math.roundToInt

object MediaEnhancerHelper {
    private const val TAG = "MediaEnhancerHelper"

    /**
     * Enhances photo sharpness, contrast, and color vibrancy.
     */
    fun enhancePhoto(
        source: Bitmap,
        sharpnessAmount: Float = 1.0f,
        contrast: Float = 1.12f,
        saturation: Float = 1.15f
    ): Bitmap {
        val width = source.width
        val height = source.height

        // 1. ColorMatrix for Contrast + Saturation
        val cm = ColorMatrix()
        // Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
        // Contrast
        val scale = contrast
        val translate = (-0.5f * scale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.setConcat(contrastMatrix, satMatrix)

        val colorEnhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(colorEnhanced)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)

        // 2. Unsharp Masking / Sharpen Kernel Pass for ultra-sharp edge clarity
        if (sharpnessAmount <= 0.05f) return colorEnhanced

        val inputPixels = IntArray(width * height)
        colorEnhanced.getPixels(inputPixels, 0, width, 0, 0, width, height)
        val outputPixels = IntArray(width * height)

        val weight = sharpnessAmount * 0.75f
        val center = 1f + 4f * weight

        for (y in 0 until height) {
            val yPrev = (y - 1).coerceAtLeast(0) * width
            val yCurr = y * width
            val yNext = (y + 1).coerceAtMost(height - 1) * width

            for (x in 0 until width) {
                val xPrev = (x - 1).coerceAtLeast(0)
                val xNext = (x + 1).coerceAtMost(width - 1)

                val cCenter = inputPixels[yCurr + x]
                val cTop = inputPixels[yPrev + x]
                val cBottom = inputPixels[yNext + x]
                val cLeft = inputPixels[yCurr + xPrev]
                val cRight = inputPixels[yCurr + xNext]

                val a = (cCenter ushr 24) and 0xFF

                var r = ((cCenter ushr 16) and 0xFF) * center -
                        (((cTop ushr 16) and 0xFF) + ((cBottom ushr 16) and 0xFF) +
                                ((cLeft ushr 16) and 0xFF) + ((cRight ushr 16) and 0xFF)) * weight

                var g = ((cCenter ushr 8) and 0xFF) * center -
                        (((cTop ushr 8) and 0xFF) + ((cBottom ushr 8) and 0xFF) +
                                ((cLeft ushr 8) and 0xFF) + ((cRight ushr 8) and 0xFF)) * weight

                var b = (cCenter and 0xFF) * center -
                        ((cTop and 0xFF) + (cBottom and 0xFF) +
                                (cLeft and 0xFF) + (cRight and 0xFF)) * weight

                val cr = r.roundToInt().coerceIn(0, 255)
                val cg = g.roundToInt().coerceIn(0, 255)
                val cb = b.roundToInt().coerceIn(0, 255)

                outputPixels[yCurr + x] = (a shl 24) or (cr shl 16) or (cg shl 8) or cb
            }
        }

        val sharpened = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        sharpened.setPixels(outputPixels, 0, width, 0, 0, width, height)
        return sharpened
    }

    /**
     * Enhances video clarity, contrast, and color vibrance using FFmpeg unsharp & eq filters.
     */
    suspend fun enhanceVideo(
        context: Context,
        inputFilePath: String,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val inputFile = VideoTrimmerHelper.resolveInputMediaFile(context, inputFilePath)
        if (inputFile == null || !inputFile.exists()) {
            return@withContext Result.failure(Exception("Input video file not found at: $inputFilePath"))
        }
        val isTemp = inputFile.name.startsWith("kmb_src_input_")

        val ffmpegBin = VideoWatermarkHelper.findFFmpegBinary(context)
        if (ffmpegBin == null || !ffmpegBin.exists()) {
            if (isTemp) try { inputFile.delete() } catch (_: Exception) {}
            return@withContext Result.failure(Exception("FFmpeg engine is not available on this device."))
        }

        val tempOutput = File(context.cacheDir, "kmb_enhanced_${System.currentTimeMillis()}.mp4")
        val enhanceFilter = "unsharp=5:5:1.1:5:5:0.6,eq=contrast=1.12:saturation=1.18:brightness=0.01"

        try {
            onProgress("Enhancing video clarity & colors...")
            val cmd = arrayOf(
                ffmpegBin.absolutePath,
                "-y",
                "-i", inputFile.absolutePath,
                "-vf", enhanceFilter,
                "-c:a", "copy",
                "-preset", "ultrafast",
                tempOutput.absolutePath
            )

            val pb = ProcessBuilder(*cmd).redirectErrorStream(true)
            val env = pb.environment()
            val ldPath = buildString {
                append(context.applicationInfo.nativeLibraryDir)
                val packagesUsrLib = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
                if (packagesUsrLib.exists()) {
                    append(":")
                    append(packagesUsrLib.absolutePath)
                }
            }
            env["LD_LIBRARY_PATH"] = ldPath

            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("frame=") == true) {
                    onProgress("Enhancing video frames...")
                }
            }
            process.waitFor()

            if (tempOutput.exists() && tempOutput.length() > 0) {
                saveEnhancedVideoToGallery(context, tempOutput)
                Result.success(tempOutput)
            } else {
                Result.failure(Exception("Video enhancement produced no output."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Video enhancement error: ${e.message}", e)
            Result.failure(e)
        } finally {
            if (isTemp) try { inputFile.delete() } catch (_: Exception) {}
        }
    }

    suspend fun saveEnhancedPhotoToGallery(context: Context, bitmap: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val filename = "KMB_Enhanced_${System.currentTimeMillis()}.png"

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
            ?: return@withContext Result.failure(Exception("Failed to save enhanced photo"))

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

    private fun saveEnhancedVideoToGallery(context: Context, file: File): Uri? {
        val resolver = context.contentResolver
        val filename = "KMB_Enhanced_${System.currentTimeMillis()}.mp4"
        val relativePath = "Movies/KMB AI Studio"

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(collection, values) ?: return null

        return try {
            FileInputStream(file).use { input ->
                resolver.openOutputStream(itemUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("video/mp4"), null)
            itemUri
        } catch (e: Exception) {
            try { resolver.delete(itemUri, null, null) } catch (_: Exception) {}
            null
        }
    }
}
