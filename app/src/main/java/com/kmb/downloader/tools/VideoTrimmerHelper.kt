package com.kmb.downloader.tools

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

object VideoTrimmerHelper {
    private const val TAG = "VideoTrimmerHelper"

    suspend fun trimMedia(
        context: Context,
        inputFilePath: String,
        startTime: String, // e.g. "01:50" or "00:01:50"
        endTime: String,   // e.g. "02:55" or "00:02:55"
        isAudioOnly: Boolean = false
    ): Result<File> = withContext(Dispatchers.IO) {
        val inputFile = File(inputFilePath)
        if (!inputFile.exists()) {
            return@withContext Result.failure(Exception("Source file not found at: $inputFilePath"))
        }

        val startMs = parseTimeToMs(startTime)
        val endMs = parseTimeToMs(endTime)
        if (endMs <= startMs) {
            return@withContext Result.failure(Exception("End time must be greater than start time"))
        }

        val startUs = startMs * 1000L
        val endUs = endMs * 1000L

        val ext = if (isAudioOnly) "m4a" else "mp4"
        val tempOutput = File(context.cacheDir, "kmb_trim_${System.currentTimeMillis()}.$ext")

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(inputFile.absolutePath)
            val trackCount = extractor.trackCount
            muxer = MediaMuxer(tempOutput.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val indexMap = HashMap<Int, Int>(trackCount)
            var bufferSize = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                val isAudio = mime.startsWith("audio/")
                val isVideo = mime.startsWith("video/")

                if (isAudioOnly && !isAudio) continue
                if (!isAudioOnly && !isVideo && !isAudio) continue

                extractor.selectTrack(i)
                val dstIndex = muxer.addTrack(format)
                indexMap[i] = dstIndex

                if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                    val newSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                    bufferSize = maxOf(bufferSize, newSize)
                }
            }

            if (bufferSize < 0) bufferSize = 1024 * 1024
            val dstBuf = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            muxer.start()

            // Seek to start position
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(dstBuf, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                if (bufferInfo.presentationTimeUs > endUs) {
                    break
                }

                bufferInfo.flags = extractor.sampleFlags
                val trackIndex = extractor.sampleTrackIndex

                val dstTrack = indexMap[trackIndex]
                if (dstTrack != null && bufferInfo.presentationTimeUs >= startUs) {
                    muxer.writeSampleData(dstTrack, dstBuf, bufferInfo)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null
            extractor.release()

            if (tempOutput.exists() && tempOutput.length() > 0) {
                saveTrimmedToGallery(context, tempOutput, isAudioOnly)
                Result.success(tempOutput)
            } else {
                Result.failure(Exception("Trimming produced empty file. Check timestamps."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Trim media failed: ${e.message}", e)
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    private fun parseTimeToMs(time: String): Long {
        val clean = time.trim()
        val parts = clean.split(":")
        return when (parts.size) {
            1 -> (parts[0].toLongOrNull() ?: 0L) * 1000L
            2 -> {
                val m = parts[0].toLongOrNull() ?: 0L
                val s = parts[1].toLongOrNull() ?: 0L
                (m * 60L + s) * 1000L
            }
            3 -> {
                val h = parts[0].toLongOrNull() ?: 0L
                val m = parts[1].toLongOrNull() ?: 0L
                val s = parts[2].toLongOrNull() ?: 0L
                (h * 3600L + m * 60L + s) * 1000L
            }
            else -> 0L
        }
    }

    private fun saveTrimmedToGallery(context: Context, file: File, isAudio: Boolean): Uri? {
        val resolver = context.contentResolver
        val mimeType = if (isAudio) "audio/mp4" else "video/mp4"
        val filename = "KMB_Trim_${System.currentTimeMillis()}.${file.extension}"
        val relativePath = if (isAudio) "Music/KMB Downloader" else "Movies/KMB Downloader"

        val collection = if (isAudio) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
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
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mimeType), null)
            itemUri
        } catch (e: Exception) {
            try { resolver.delete(itemUri, null, null) } catch (_: Exception) {}
            null
        }
    }
}
