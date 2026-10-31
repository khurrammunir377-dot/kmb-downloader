package com.kmb.downloader.tools

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
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
    ALL_SCREEN_TEXT,  // Cleans all text across screen (Top captions, Middle handle, and Subtitles)
    TOP_CAPTIONS,     // Top captions & headers
    MIDDLE_TEXT,      // Middle text, TikTok handle & center captions
    BOTTOM_SUBTITLES, // Bottom subtitles / captions
    BOTTOM_RIGHT,     // Bottom-right watermark
    BOTTOM_LEFT,      // Bottom-left watermark
    TOP_RIGHT,        // Top-right watermark
    TOP_LEFT          // Top-left watermark
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

    fun buildLdLibraryPath(context: Context): String {
        return buildString {
            append(context.applicationInfo.nativeLibraryDir)
            val ffmpegUsrLib = File(context.noBackupFilesDir, "youtubedl-android/packages/ffmpeg/usr/lib")
            if (ffmpegUsrLib.exists()) {
                append(":")
                append(ffmpegUsrLib.absolutePath)
            }
            val pythonUsrLib = File(context.noBackupFilesDir, "youtubedl-android/packages/python/usr/lib")
            if (pythonUsrLib.exists()) {
                append(":")
                append(pythonUsrLib.absolutePath)
            }
            val aria2cUsrLib = File(context.noBackupFilesDir, "youtubedl-android/packages/aria2c/usr/lib")
            if (aria2cUsrLib.exists()) {
                append(":")
                append(aria2cUsrLib.absolutePath)
            }
        }
    }

    fun getVideoDimensions(context: Context, videoFile: File): Pair<Int, Int> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoFile.absolutePath)
            val rawW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 720
            val rawH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1280
            val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rot == 90 || rot == 270) Pair(rawH, rawW) else Pair(rawW, rawH)
        } catch (_: Exception) {
            Pair(720, 1280)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    /**
     * Executes FFmpeg reliably via shell wrapper with complete library path environment.
     */
    suspend fun runFFmpegCommand(
        context: Context,
        args: List<String>,
        onProgress: (String) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val ffmpegBin = findFFmpegBinary(context)
            ?: return@withContext Result.failure(Exception("FFmpeg engine is not available on this device."))

        val ldPath = buildLdLibraryPath(context)
        val quotedArgs = args.joinToString(" ") { if (it.contains(" ") || it.contains(";") || it.contains("[")) "'$it'" else it }
        val shellScript = "export LD_LIBRARY_PATH=\"$ldPath:\$LD_LIBRARY_PATH\"; export PATH=\"${context.applicationInfo.nativeLibraryDir}:\$PATH\"; exec '${ffmpegBin.absolutePath}' $quotedArgs"

        try {
            val pb = ProcessBuilder("/system/bin/sh", "-c", shellScript)
                .redirectErrorStream(true)
            val env = pb.environment()
            env["LD_LIBRARY_PATH"] = ldPath

            val process = pb.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            val lastLines = mutableListOf<String>()

            while (reader.readLine().also { line = it } != null) {
                line?.let { l ->
                    lastLines.add(l)
                    if (lastLines.size > 20) lastLines.removeAt(0)
                    if (l.contains("frame=") || l.contains("time=")) {
                        onProgress("Processing frames...")
                    }
                }
            }

            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Result.success(Unit)
            } else {
                val errorDetails = lastLines.joinToString("\n")
                Log.e(TAG, "FFmpeg failed with exit code $exitCode:\n$errorDetails")
                Result.failure(Exception("FFmpeg error ($exitCode): ${lastLines.lastOrNull() ?: "Execution failed"}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "FFmpeg process error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Auto Delogo: Automatically removes TikTok and Reels watermarks (top-left & bottom-right)
     * using border interpolation delogo filter without asking for manual location or blurring.
     */
    suspend fun autoRemoveWatermarks(
        context: Context,
        inputFilePath: String,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val inputFile = VideoTrimmerHelper.resolveInputMediaFile(context, inputFilePath)
            ?: return@withContext Result.failure(Exception("Input video file not found at: $inputFilePath"))
        val isTemp = inputFile.name.startsWith("kmb_src_input_")

        val tempOutput = File(context.cacheDir, "kmb_clean_video_${System.currentTimeMillis()}.mp4")
        val (width, height) = getVideoDimensions(context, inputFile)

        val w = (width * 0.22).toInt().coerceIn(120, 220)
        val h = (height * 0.06).toInt().coerceIn(50, 95)
        val x1 = 16
        val y1 = 16
        val x2 = (width - w - 16).coerceAtLeast(1)
        val y2 = (height - h - 16).coerceAtLeast(1)

        // FFmpeg delogo filter interpolates surrounding pixels across the rectangle
        // band=2 solves the boundary equations to seamlessly remove the logo without blurring
        val filter = "delogo=x=$x1:y=$y1:w=$w:h=$h:band=2:show=0,delogo=x=$x2:y=$y2:w=$w:h=$h:band=2:show=0"

        onProgress("Auto-detecting & removing watermarks...")
        val args = listOf(
            "-y",
            "-i", inputFile.absolutePath,
            "-vf", filter,
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-pix_fmt", "yuv420p",
            "-c:a", "copy",
            tempOutput.absolutePath
        )

        val execRes = runFFmpegCommand(context, args, onProgress)
        if (isTemp) try { inputFile.delete() } catch (_: Exception) {}

        execRes.fold(
            onSuccess = {
                if (tempOutput.exists() && tempOutput.length() > 0) {
                    saveCleanVideoToGallery(context, tempOutput)
                    Result.success(tempOutput)
                } else {
                    Result.failure(Exception("Processing completed with empty output file."))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun removeWatermark(
        context: Context,
        inputFilePath: String,
        position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        onProgress: (String) -> Unit = {}
    ): Result<File> = autoRemoveWatermarks(context, inputFilePath, onProgress)

    /**
     * Erases subtitles, channel handles, or all text across the screen from video.
     */
    suspend fun eraseVideoText(
        context: Context,
        inputFilePath: String,
        region: VideoTextRegion = VideoTextRegion.ALL_SCREEN_TEXT,
        onProgress: (String) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val inputFile = VideoTrimmerHelper.resolveInputMediaFile(context, inputFilePath)
            ?: return@withContext Result.failure(Exception("Input video file not found at: $inputFilePath"))
        val isTemp = inputFile.name.startsWith("kmb_src_input_")

        val tempOutput = File(context.cacheDir, "kmb_text_erased_${System.currentTimeMillis()}.mp4")
        val (width, height) = getVideoDimensions(context, inputFile)

        val filter = when (region) {
            VideoTextRegion.ALL_SCREEN_TEXT -> {
                // Erases Top Captions + Middle TikTok handle/text + Bottom Subtitles using boundary delogo interpolation
                val topH = (height * 0.16).toInt().coerceAtLeast(70)
                val topY = (height * 0.04).toInt()
                val midH = (height * 0.18).toInt().coerceAtLeast(80)
                val midY = (height * 0.44).toInt()
                val botH = (height * 0.14).toInt().coerceAtLeast(70)
                val botY = (height * 0.82).toInt()
                "delogo=x=0:y=$topY:w=$width:h=$topH:band=2:show=0,delogo=x=0:y=$midY:w=$width:h=$midH:band=2:show=0,delogo=x=0:y=$botY:w=$width:h=$botH:band=2:show=0"
            }
            VideoTextRegion.TOP_CAPTIONS -> {
                val h = (height * 0.18).toInt().coerceAtLeast(80)
                val y = (height * 0.04).toInt()
                "delogo=x=0:y=$y:w=$width:h=$h:band=2:show=0"
            }
            VideoTextRegion.MIDDLE_TEXT -> {
                val h = (height * 0.20).toInt().coerceAtLeast(90)
                val y = (height * 0.42).toInt()
                "delogo=x=0:y=$y:w=$width:h=$h:band=2:show=0"
            }
            VideoTextRegion.BOTTOM_SUBTITLES -> {
                val h = (height * 0.15).toInt().coerceAtLeast(70)
                val y = (height * 0.82).toInt()
                "delogo=x=0:y=$y:w=$width:h=$h:band=2:show=0"
            }
            VideoTextRegion.BOTTOM_RIGHT -> {
                val w = (width * 0.26).toInt().coerceIn(120, 240)
                val h = (height * 0.08).toInt().coerceIn(55, 110)
                val x = (width - w - 16).coerceAtLeast(1)
                val y = (height - h - 16).coerceAtLeast(1)
                "delogo=x=$x:y=$y:w=$w:h=$h:band=2:show=0"
            }
            VideoTextRegion.BOTTOM_LEFT -> {
                val w = (width * 0.26).toInt().coerceIn(120, 240)
                val h = (height * 0.08).toInt().coerceIn(55, 110)
                val y = (height - h - 16).coerceAtLeast(1)
                "delogo=x=16:y=$y:w=$w:h=$h:band=2:show=0"
            }
            VideoTextRegion.TOP_RIGHT -> {
                val w = (width * 0.26).toInt().coerceIn(120, 240)
                val h = (height * 0.08).toInt().coerceIn(55, 110)
                val x = (width - w - 16).coerceAtLeast(1)
                "delogo=x=$x:y=16:w=$w:h=$h:band=2:show=0"
            }
            VideoTextRegion.TOP_LEFT -> {
                val w = (width * 0.26).toInt().coerceIn(120, 240)
                val h = (height * 0.08).toInt().coerceIn(55, 110)
                "delogo=x=16:y=16:w=$w:h=$h:band=2:show=0"
            }
        }

        onProgress("Erasing text from video frames...")
        val args = listOf(
            "-y",
            "-i", inputFile.absolutePath,
            "-vf", filter,
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-pix_fmt", "yuv420p",
            "-c:a", "copy",
            tempOutput.absolutePath
        )

        val execRes = runFFmpegCommand(context, args, onProgress)
        if (isTemp) try { inputFile.delete() } catch (_: Exception) {}

        execRes.fold(
            onSuccess = {
                if (tempOutput.exists() && tempOutput.length() > 0) {
                    saveCleanVideoToGallery(context, tempOutput)
                    Result.success(tempOutput)
                } else {
                    Result.failure(Exception("Video processing produced empty output."))
                }
            },
            onFailure = { Result.failure(it) }
        )
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
