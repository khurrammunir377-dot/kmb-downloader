package com.kmb.downloader.download

import android.text.Html
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

data class MediaMetadata(
    val title: String,
    val description: String? = null,
    val author: String? = null,
    val thumbnail: String? = null
)

object MetadataFetcherHelper {
    private const val TAG = "MetadataFetcherHelper"

    private val OG_TITLE_PATTERN = Pattern.compile("<meta\\s+[^>]*property=[\"']og:title[\"'][^>]*content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val TWITTER_TITLE_PATTERN = Pattern.compile("<meta\\s+[^>]*name=[\"']twitter:title[\"'][^>]*content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val TITLE_TAG_PATTERN = Pattern.compile("<title[^>]*>([^<]*)</title>", Pattern.CASE_INSENSITIVE)

    private val OG_DESC_PATTERN = Pattern.compile("<meta\\s+[^>]*property=[\"']og:description[\"'][^>]*content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val TWITTER_DESC_PATTERN = Pattern.compile("<meta\\s+[^>]*name=[\"']twitter:description[\"'][^>]*content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)
    private val DESC_TAG_PATTERN = Pattern.compile("<meta\\s+[^>]*name=[\"']description[\"'][^>]*content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)

    private val OG_IMAGE_PATTERN = Pattern.compile("<meta\\s+[^>]*property=[\"']og:image[\"'][^>]*content=[\"']([^\"']*)[\"']", Pattern.CASE_INSENSITIVE)

    /**
     * Fast network probe to extract og:title, og:description, and thumbnail from a URL.
     */
    suspend fun fetchMetadata(rawUrl: String): MediaMetadata? = withContext(Dispatchers.IO) {
        try {
            val url = URL(rawUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                )
                setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9,ur;q=0.8,hi;q=0.7")
            }

            val htmlBuilder = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                var line: String?
                var linesRead = 0
                // Read head section up to ~250 lines
                while (reader.readLine().also { line = it } != null && linesRead < 250) {
                    htmlBuilder.append(line).append("\n")
                    linesRead++
                    if (line?.contains("</head>", ignoreCase = true) == true) break
                }
            }
            connection.disconnect()

            val html = htmlBuilder.toString()
            if (html.isBlank()) return@withContext null

            var title: String? = null
            var desc: String? = null
            var image: String? = null

            // 1. Title
            val ogTitleMatcher = OG_TITLE_PATTERN.matcher(html)
            if (ogTitleMatcher.find()) {
                title = cleanHtmlText(ogTitleMatcher.group(1))
            }
            if (title.isNullOrBlank()) {
                val twTitleMatcher = TWITTER_TITLE_PATTERN.matcher(html)
                if (twTitleMatcher.find()) title = cleanHtmlText(twTitleMatcher.group(1))
            }
            if (title.isNullOrBlank()) {
                val tagMatcher = TITLE_TAG_PATTERN.matcher(html)
                if (tagMatcher.find()) title = cleanHtmlText(tagMatcher.group(1))
            }

            // 2. Description
            val ogDescMatcher = OG_DESC_PATTERN.matcher(html)
            if (ogDescMatcher.find()) {
                desc = cleanHtmlText(ogDescMatcher.group(1))
            }
            if (desc.isNullOrBlank()) {
                val twDescMatcher = TWITTER_DESC_PATTERN.matcher(html)
                if (twDescMatcher.find()) desc = cleanHtmlText(twDescMatcher.group(1))
            }
            if (desc.isNullOrBlank()) {
                val metaDescMatcher = DESC_TAG_PATTERN.matcher(html)
                if (metaDescMatcher.find()) desc = cleanHtmlText(metaDescMatcher.group(1))
            }

            // 3. Image
            val ogImageMatcher = OG_IMAGE_PATTERN.matcher(html)
            if (ogImageMatcher.find()) {
                image = ogImageMatcher.group(1)?.trim()
            }

            // Parse Author / Title split if title has separator
            var author: String? = null
            title?.let { rawT ->
                val separators = listOf(" - ", " – ", " | ", " • ")
                for (sep in separators) {
                    if (rawT.contains(sep)) {
                        val parts = rawT.split(sep, limit = 2)
                        if (parts[0].isNotBlank()) author = parts[0].trim()
                        break
                    }
                }
            }

            if (!title.isNullOrBlank() || !desc.isNullOrBlank()) {
                MediaMetadata(
                    title = title ?: (desc?.take(60) ?: "Video"),
                    description = desc,
                    author = author,
                    thumbnail = image
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scrape metadata for $rawUrl: ${e.message}")
            null
        }
    }

    private fun cleanHtmlText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return try {
            val decoded = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            decoded.replace(Regex("\\s+"), " ")
        } catch (_: Exception) {
            raw.trim()
        }
    }

    /**
     * Cleans raw yt-dlp filename (which is %(title)s [%(id)s]) into a clean title.
     */
    fun cleanFilenameToTitle(rawFilename: String): String {
        return rawFilename
            .replace(Regex("\\[[a-zA-Z0-9_-]{5,}\\]$"), "")
            .replace(Regex("[-_]"), " ")
            .trim()
    }
}
