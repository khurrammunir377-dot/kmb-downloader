package com.kmb.downloader.tools

import android.content.ContentValues
import android.content.Context
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

enum class WatermarkPosition {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

enum class VideoTextRegion {
    BOTTOM_SUBTITLES, // Captions / Subtitles bar at bottom
    TOP_CHANNEL_TAG,  // Channel tag / handle at top
    BOTTOM_RIGHT,     // Bottom-right watermark/logo
    BOTTOM_LEFT,      // Bottom-left watermark/logo
    TOP_RIGHT,        // Top-right watermark/logo
    TOP_LEFT          // Top-left watermark/logo
}

object VideoWatermarkHelper {
    private const val TAG = "VideoWatermarkHelper"

    /**
     * Finds the extracted ffmpeg executable path on the device.
     */
    fun findFFmpegBinary(context: Context): File? {
        val candidates = listOf(
            File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so"),
            File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/bin/ffmpeg"),
            File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/ffmpeg"),
            File(context.noBackupFilesDir, "youtubedl-android/bin/ffmpeg"),
            File(context.noBackupFilesDir, "youtubedl-android/ffmpeg"),
            File(context.noBackupFilesDir, "ffmpeg"),
            File(context.filesDir, "packages/ffmpeg/bin/ffmpeg"),
            File(context.filesDir, "packages/ffmpeg"),
            File(context.filesDir, "bin/ffmpeg"),
            File(context.filesDir, "ffmpeg")
        )
        for (candidate in candidates) {
            if (candidate.exists()) {
                candidate.setExecutable(true, false)
                return candidate
            }
        }
        // Dynamic search in noBackupFilesDir & filesDir
        try {
            context.noBackupFilesDir.walkTopDown().maxDepth(5).firstOrNull {
                (it.name == "ffmpeg" || it.name == "libffmpeg.so") && it.isFile
            }?.let {
                it.setExecutable(true, false)
                return it
            }
            context.filesDir.walkTopDown().maxDepth(5).firstOrNull {
                (it.name == "ffmpeg" || it.name == "libffmpeg.so") && it.isFile
            }?.let {
                it.setExecutable(true, false)
                return it
            }
        } catch (_: Exception) {}
        return null
    }

    private fun configureProcessEnvironment(context: Context, pb: ProcessBuilder) {
        val env = pb.environment()
        val ldPath = buildString {
            append(context.applicationInfo.nativeLibraryDir)
            val packagesUsrLib = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
            if (packagesUsrLib.exists()) {
                append(":")
                append(packagesUsrLib.absolutePath)
            }
            val existing = env["LD_LIBRARY_PATH"]
            if (!existing.isNullOrBlank()) {
                append(":")
                append(existing)
            }
        }
        env["LD_LIBRARY_PATH"] = ldPath
    }

    /**
     * Removes/blurs watermarks from video using delogo filter.
     */
    suspend fun removeWatermark(
        context: Context,
        inputFilePath: String,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val inputFile = VideoTrimmerHelper.resolveInputMediaFile(context, inputFilePath)
        if (inputFile == null || !inputFile.exists()) {
            return@withContext Result.failure(Exception("Input video file not found at: $inputFilePath"))
        }
        val isTemp = inputFile.name.startsWith("kmb_src_input_")

        val ffmpegBin = findFFmpegBinary(context)
        if (ffmpegBin == null || !ffmpegBin.exists()) {
            if (isTemp) try { inputFile.delete() } catch (_: Exception) {}
            return@withContext Result.failure(Exception("FFmpeg engine is not available on this device."))
        }

        val tempOutput = File(context.cacheDir, "kmb_clean_video_${System.currentTimeMillis()}.mp4")

        val delogoFilter = when (position) {
            WatermarkPosition.TOP_LEFT -> "delogo=x=20:y=20:w=220:h=90"
            WatermarkPosition.TOP_RIGHT -> "delogo=x=main_w-240:y=20:w=220:h=90"
            WatermarkPosition.BOTTOM_LEFT -> "delogo=x=20:y=main_h-110:w=220:h=90"
            WatermarkPosition.BOTTOM_RIGHT -> "delogo=x=main_w-240:y=main_h-110:w=220:h=90"
        }

        try {
            onProgress("Applying AI watermark removal...")
            val cmd = arrayOf(
                ffmpegBin.absolutePath,
                "-y",
                "-i", inputFile.absolutePath,
                "-vf", delogoFilter,
                "-c:a", "copy",
                "-preset", "ultrafast",
                tempOutput.absolutePath
            )

            val pb = ProcessBuilder(*cmd).redirectErrorStream(true)
            configureProcessEnvironment(context, pb)
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("frame=") == true) {
                    onProgress("Processing video frames...")
                }
            }
            process.waitFor()

            if (tempOutput.exists() && tempOutput.length() > 0) {
                saveCleanVideoToGallery(context, tempOutput)
                Result.success(tempOutput)
            } else {
                Result.failure(Exception("Watermark removal produced no output."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "FFmpeg delogo execution failed: ${e.message}", e)
            Result.failure(e)
        } finally {
            if (isTemp) try { inputFile.delete() } catch (_: Exception) {}
        }
    }

    /**
     * Erases subtitles or channel handles from video using region delogo / blur.
     */
    suspend fun eraseVideoText(
        context: Context,
        inputFilePath: String,
        region: VideoTextRegion = VideoTextRegion.BOTTOM_SUBTITLES,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val inputFile = VideoTrimmerHelper.resolveInputMediaFile(context, inputFilePath)
        if (inputFile == null || !inputFile.exists()) {
            return@withContext Result.failure(Exception("Input video file not found at: $inputFilePath"))
        }
        val isTemp = inputFile.name.startsWith("kmb_src_input_")

        val ffmpegBin = findFFmpegBinary(context)
        if (ffmpegBin == null || !ffmpegBin.exists()) {
            if (isTemp) try { inputFile.delete() } catch (_: Exception) {}
            return@withContext Result.failure(Exception("FFmpeg engine is not available on this device."))
        }

        val tempOutput = File(context.cacheDir, "kmb_text_erased_${System.currentTimeMillis()}.mp4")

        val filter = when (region) {
            VideoTextRegion.BOTTOM_SUBTITLES -> "delogo=x=20:y=main_h-180:w=main_w-40:h=150"
            VideoTextRegion.TOP_CHANNEL_TAG -> "delogo=x=main_w*0.2:y=20:w=main_w*0.6:h=90"
            VideoTextRegion.BOTTOM_RIGHT -> "delogo=x=main_w-260:y=main_h-120:w=240:h=100"
            VideoTextRegion.BOTTOM_LEFT -> "delogo=x=20:y=main_h-120:w=240:h=100"
            VideoTextRegion.TOP_RIGHT -> "delogo=x=main_w-260:y=20:w=240:h=100"
            VideoTextRegion.TOP_LEFT -> "delogo=x=20:y=20:w=240:h=100"
        }

        try {
            onProgress("Erasing video text / subtitles...")
            val cmd = arrayOf(
                ffmpegBin.absolutePath,
                "-y",
                "-i", inputFile.absolutePath,
                "-vf", filter,
                "-c:a", "copy",
                "-preset", "ultrafast",
                tempOutput.absolutePath
            )

            val pb = ProcessBuilder(*cmd).redirectErrorStream(true)
            configureProcessEnvironment(context, pb)
            val process = pb.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("frame=") == true) {
                    onProgress("Processing video frames...")
                }
            }
            process.waitFor()

            if (tempOutput.exists() && tempOutput.length() > 0) {
                saveCleanVideoToGallery(context, tempOutput)
                Result.success(tempOutput)
            } else {
                Result.failure(Exception("Video processing produced no output."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Video text eraser error: ${e.message}", e)
            Result.failure(e)
        } finally {
            if (isTemp) try { inputFile.delete() } catch (_: Exception) {}
        }
    }

    private fun saveCleanVideoToGallery(context: Context, file: File): Uri? {
        val resolver = context.contentResolver
        val filename = "KMB_Delogo_${System.currentTimeMillis()}.mp4"
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
