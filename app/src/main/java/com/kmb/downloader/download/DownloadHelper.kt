package com.kmb.downloader.download

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DownloadHelper {

    private const val TAG = "DownloadHelper"

    private val URL_PATTERN = Pattern.compile("https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+")

    private val SOCIAL_PATTERNS = listOf(
        "youtube.com", "youtu.be",
        "tiktok.com", "vm.tiktok.com", "vt.tiktok.com",
        "instagram.com", "reel", "reels",
        "facebook.com", "fb.watch", "fb.com",
        "twitter.com", "x.com",
        "reddit.com",
        "vimeo.com",
        "dailymotion.com",
        "pinterest.com",
        "twitch.tv",
        "soundcloud.com",
        "bilibili.com",
        "streamable.com",
        "threads.net"
    )

    fun isSocialMediaLink(text: String): Boolean {
        val lower = text.lowercase().trim()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) return false
        return SOCIAL_PATTERNS.any { lower.contains(it) }
    }

    /** Fast URL extraction with regex on bounded input to prevent UI freezes */
    fun extractUrl(text: String): String? {
        val sample = text.take(2048).trim()
        val matcher = URL_PATTERN.matcher(sample)
        var firstValid: String? = null
        while (matcher.find()) {
            val candidate = matcher.group()
            if (isSocialMediaLink(candidate)) {
                return cleanUrl(candidate)
            }
            if (firstValid == null && candidate.contains(".")) {
                firstValid = candidate
            }
        }
        return firstValid?.let { cleanUrl(it) }
    }

    /**
     * Extracts ALL valid video/media URLs from text (multi-line pastes, lists of 5+ links, etc.).
     */
    fun extractAllUrls(text: String): List<String> {
        val sample = text.take(16384).trim()
        val matcher = URL_PATTERN.matcher(sample)
        val results = mutableListOf<String>()
        val seen = mutableSetOf<String>()

        while (matcher.find()) {
            val candidate = matcher.group()
            val cleaned = cleanUrl(candidate)
            if (isSocialMediaLink(cleaned) || isSocialMediaLink(candidate) || (candidate.startsWith("http") && candidate.contains("."))) {
                if (seen.add(cleaned)) {
                    results.add(cleaned)
                }
            }
        }
        return results
    }

    /**
     * Clean and preserve critical video parameters.
     * Never destroys Facebook watch '?v=' or required query parameters.
     */
    fun cleanUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return try {
            val uri = Uri.parse(trimmed)
            when {
                trimmed.contains("youtube.com/watch") -> {
                    val v = uri.getQueryParameter("v")
                    if (!v.isNullOrBlank()) "https://www.youtube.com/watch?v=$v" else trimmed
                }
                trimmed.contains("facebook.com/watch") || trimmed.contains("fb.watch") -> {
                    val v = uri.getQueryParameter("v")
                    if (!v.isNullOrBlank()) "https://www.facebook.com/watch/?v=$v" else trimmed
                }
                trimmed.contains("instagram.com/reel/") ||
                trimmed.contains("instagram.com/p/") ||
                trimmed.contains("tiktok.com") ||
                trimmed.contains("threads.net") -> {
                    // For Instagram/TikTok, strip tracking query while preserving path
                    val base = trimmed.substringBefore("?")
                    if (base.length > 20) base else trimmed
                }
                else -> trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    fun detectPlatform(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> "YouTube"
            lower.contains("tiktok.com") -> "TikTok"
            lower.contains("instagram.com") -> "Instagram"
            lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") -> "Facebook"
            lower.contains("twitter.com") || lower.contains("x.com") -> "X / Twitter"
            lower.contains("threads.net") -> "Threads"
            lower.contains("reddit.com") -> "Reddit"
            lower.contains("vimeo.com") -> "Vimeo"
            lower.contains("dailymotion.com") -> "Dailymotion"
            lower.contains("pinterest.com") -> "Pinterest"
            lower.contains("twitch.tv") -> "Twitch"
            lower.contains("soundcloud.com") -> "SoundCloud"
            lower.contains("streamable.com") -> "Streamable"
            else -> "Video"
        }
    }

    /**
     * Builds a clean, highly reliable yt-dlp request.
     * Uses standard options that work across all platforms without 403 Forbidden issues.
     */
    fun buildRequest(url: String, quality: String, outputDir: File, timeRange: String? = null): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)

        // Standard output format template
        request.addOption("-o", "${outputDir.absolutePath}/%(title)s [%(id)s].%(ext)s")

        // Download specific time range if specified (e.g. 01:50-02:55)
        if (!timeRange.isNullOrBlank()) {
            val normalizedRange = normalizeDownloadSections(timeRange)
            if (normalizedRange != null) {
                request.addOption("--download-sections", "*$normalizedRange")
                request.addOption("--force-keyframes-at-cuts")
            }
        }

        // Select optimal format based on user choice
        val formatSelector = when (quality) {
            "Audio Only" -> "bestaudio[ext=m4a]/bestaudio/best"
            "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best"
            "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]/best"
            "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]/best"
            else -> "bestvideo*+bestaudio/best"
        }
        request.addOption("-f", formatSelector)

        // Merge to MP4 container if video + audio streams are separate
        if (quality != "Audio Only") {
            request.addOption("--merge-output-format", "mp4")
        }

        // Pass mobile & web player client to prevent YouTube bot check / 403 blocks
        request.addOption("--extractor-args", "youtube:player_client=android,web")

        // Reliability and speed options
        request.addOption("--no-playlist")
        request.addOption("--restrict-filenames")
        request.addOption("--no-mtime")
        request.addOption("--no-check-certificates")

        // VIP Pro Turbo boost: concurrent fragment downloading
        val fragments = if (com.kmb.downloader.data.ReferralManager.proState.value.isPro) "5" else "3"
        request.addOption("--concurrent-fragments", fragments)
        request.addOption("--no-warnings")
        request.addOption("--prefer-free-formats")
        request.addOption("--force-overwrites")
        request.addOption("--socket-timeout", "30")
        request.addOption("--retries", "10")
        request.addOption("--fragment-retries", "10")

        Log.d(TAG, "Request: url=$url, quality=$quality, format=$formatSelector, dir=${outputDir.absolutePath}")
        return request
    }

    /**
     * Bulletproof parsing and normalization of time ranges.
     * Handles "2.0.-4.00", "01:50-02:55", "1.50-2.55", "120-240", etc.
     */
    fun normalizeDownloadSections(rawRange: String): String? {
        val clean = rawRange.trim().removePrefix("*")
        val parts = clean.split("-")
        if (parts.size != 2) return null
        val startSec = parseTimeToSeconds(parts[0]) ?: 0
        val endSec = parseTimeToSeconds(parts[1]) ?: return null
        if (endSec <= startSec) return null
        return "${formatSeconds(startSec)}-${formatSeconds(endSec)}"
    }

    fun parseTimeToSeconds(raw: String): Int? {
        val s = raw.trim().trimEnd('.').replace(";", ":")
        val tokens = if (s.contains(":")) s.split(":") else s.split(".")
        return when (tokens.size) {
            1 -> tokens[0].trim().toIntOrNull()
            2 -> {
                val m = tokens[0].trim().toIntOrNull() ?: return null
                val sec = tokens[1].trim().toIntOrNull() ?: return null
                m * 60 + sec
            }
            3 -> {
                val h = tokens[0].trim().toIntOrNull() ?: return null
                val m = tokens[1].trim().toIntOrNull() ?: return null
                val sec = tokens[2].trim().toIntOrNull() ?: return null
                h * 3600 + m * 60 + sec
            }
            else -> null
        }
    }

    private fun formatSeconds(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", m, s)
        }
    }

    /**
     * Resolves direct master stream URLs for special article pages (like BBC News articles)
     * where yt-dlp's default extractor cannot locate Next.js embedded clips.
     */
    suspend fun resolveDirectStreamIfSpecial(url: String): String = withContext(Dispatchers.IO) {
        val clean = url.trim()
        if (clean.contains("bbc.com/") && clean.contains("/articles/")) {
            try {
                val resolved = resolveBbcArticleStream(clean)
                if (!resolved.isNullOrBlank()) {
                    Log.i(TAG, "Resolved BBC article video stream: $resolved")
                    return@withContext resolved
                }
            } catch (e: Exception) {
                Log.w(TAG, "BBC resolver fallback to original url: ${e.message}")
            }
        }
        return@withContext clean
    }

    private fun resolveBbcArticleStream(articleUrl: String): String? {
        val conn = java.net.URL(articleUrl).openConnection() as java.net.HttpURLConnection
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val html = conn.inputStream.bufferedReader().use { it.readText() }

        val versionRegex = Regex("[\"']versionId[\"']\\s*:\\s*[\"']([a-zA-Z0-9]+)[\"']")
        val vpidMatch = versionRegex.find(html) ?: Regex("[\"']vpid[\"']\\s*:\\s*[\"']([a-zA-Z0-9]+)[\"']").find(html)
        val vpid = vpidMatch?.groupValues?.get(1) ?: return null

        val mediaSelectorUrl = "https://open.live.bbc.co.uk/mediaselector/6/select/version/2.0/mediaset/pc/vpid/$vpid/format/json"
        val mConn = java.net.URL(mediaSelectorUrl).openConnection() as java.net.HttpURLConnection
        mConn.setRequestProperty("User-Agent", "Mozilla/5.0")
        mConn.connectTimeout = 10000
        mConn.readTimeout = 10000
        val jsonStr = mConn.inputStream.bufferedReader().use { it.readText() }
        val root = org.json.JSONObject(jsonStr)
        val mediaArr = root.optJSONArray("media") ?: return null
        for (i in 0 until mediaArr.length()) {
            val m = mediaArr.getJSONObject(i)
            val connArr = m.optJSONArray("connection") ?: continue
            for (j in 0 until connArr.length()) {
                val c = connArr.getJSONObject(j)
                val href = c.optString("href")
                if (href.contains(".m3u8")) {
                    return href
                }
            }
        }
        return null
    }

    /** Internal app storage: 100% writable by Linux subprocess on all Android versions */
    fun getDownloadDir(context: Context): File {
        val internalDir = File(context.filesDir, "downloads")
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    /**
     * Saves downloaded media file to user-accessible public storage.
     * Uses MediaStore.Downloads on Android 10+ (API 29+) so the file shows immediately
     * in Downloads, Gallery, Videos, and Files without requiring WRITE_EXTERNAL_STORAGE.
     * Falls back to public directory on Android 9 and below.
     *
     * Returns the final file path/URI or null.
     */
    fun saveToPublicStorage(context: Context, sourceFile: File, isAudio: Boolean): String? {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return null

        val cleanName = sanitizeFilename(sourceFile.name)
        val ext = sourceFile.extension.lowercase()
        val mimeType = when (ext) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "webm" -> if (isAudio) "audio/webm" else "video/webm"
            "mkv" -> "video/x-matroska"
            else -> if (isAudio) "audio/mp4" else "video/mp4"
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, cleanName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/KMBDownloader")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collectionUri, contentValues)
                if (itemUri != null) {
                    resolver.openOutputStream(itemUri).use { outStream ->
                        if (outStream != null) {
                            FileInputStream(sourceFile).use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    }

                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)

                    Log.i(TAG, "Saved to MediaStore: $itemUri")
                    try { sourceFile.delete() } catch (_: Throwable) {}
                    return itemUri.toString()
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "MediaStore insert failed, falling back to file copy: ${e.message}")
        }

        // Fallback for Android 9 and below, or if MediaStore insertion encountered an issue:
        try {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val kmbDir = File(publicDir, "KMBDownloader")
            if (!kmbDir.exists()) kmbDir.mkdirs()
            val targetFile = File(kmbDir, cleanName)

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            scanMediaFile(context, targetFile)
            Log.i(TAG, "Saved to public directory: ${targetFile.absolutePath}")
            try { sourceFile.delete() } catch (_: Throwable) {}
            return targetFile.absolutePath
        } catch (e: Throwable) {
            Log.e(TAG, "Fallback save failed: ${e.message}")
        }

        return sourceFile.absolutePath
    }

    fun scanMediaFile(context: Context, file: File) {
        try {
            MediaScannerConnection.scanFile(
                context.applicationContext,
                arrayOf(file.absolutePath),
                null, null
            )
        } catch (e: Exception) {
            Log.w(TAG, "Media scan failed: ${e.message}")
        }
    }

    suspend fun updateEngine(context: Context): Result<String> {
        return try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
            Result.success(status?.name ?: "Updated")
        } catch (e: Exception) {
            Log.e(TAG, "Engine update failed", e)
            Result.failure(e)
        }
    }

    fun cancelDownload(processId: String) {
        try {
            YoutubeDL.getInstance().destroyProcessById(processId)
        } catch (e: Exception) {
            Log.w(TAG, "Cancel failed for $processId: ${e.message}")
        }
    }
}
