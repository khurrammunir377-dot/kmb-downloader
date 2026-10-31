package com.kmb.downloader.viewmodel

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kmb.downloader.KmbApp
import com.kmb.downloader.data.AppDatabase
import com.kmb.downloader.data.DownloadItem
import com.kmb.downloader.data.SavedLink
import com.kmb.downloader.download.DownloadHelper
import com.kmb.downloader.download.DownloadService
import com.kmb.downloader.download.MetadataFetcherHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val downloadDao = db.downloadDao()
    private val savedDao = db.savedLinkDao()

    val downloads = downloadDao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedLinks = savedDao.getAllSaved()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Live in-memory progress of active downloads (0f..100f) */
    val liveProgress: StateFlow<Map<Long, Float>> = com.kmb.downloader.download.DownloadTracker.liveProgress

    /** Live in-memory status messages ("Connecting…", "Downloading 45%", etc.) */
    val liveStatus: StateFlow<Map<Long, String>> = com.kmb.downloader.download.DownloadTracker.liveStatus

    /** Exposed engine init state: null=loading, true=ready, false=failed */
    val engineInitState: StateFlow<Boolean?> = KmbApp.initState
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** True while engine is being initialized */
    val isEngineLoading: StateFlow<Boolean> = KmbApp.initState
        .map { it == null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _selectedQuality = MutableStateFlow("Best")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUpdatingEngine = MutableStateFlow(false)
    val isUpdatingEngine: StateFlow<Boolean> = _isUpdatingEngine.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _trimEnabled = MutableStateFlow(false)
    val trimEnabled: StateFlow<Boolean> = _trimEnabled.asStateFlow()

    private val _trimStartTime = MutableStateFlow("")
    val trimStartTime: StateFlow<String> = _trimStartTime.asStateFlow()

    private val _trimEndTime = MutableStateFlow("")
    val trimEndTime: StateFlow<String> = _trimEndTime.asStateFlow()

    val qualities = listOf("Best", "1080p", "720p", "480p", "Audio Only")

    fun finishSplash() {
        _showSplash.value = false
    }

    fun toggleTrim(enabled: Boolean) {
        _trimEnabled.value = enabled
    }

    fun setTrimStartTime(time: String) {
        _trimStartTime.value = time
    }

    fun setTrimEndTime(time: String) {
        _trimEndTime.value = time
    }

    fun onUrlChange(newUrl: String) {
        _url.value = newUrl.trim()
    }

    fun onQualitySelected(quality: String) {
        _selectedQuality.value = quality
    }

    /** Called when app comes to foreground — scan clipboard for new links */
    fun checkClipboardAndSave() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val clipboard = getApplication<Application>()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@launch
                val clip = clipboard.primaryClip ?: return@launch
                if (clip.itemCount == 0) return@launch

                var newCount = 0
                var lastPlatform = "Video"

                for (i in 0 until clip.itemCount) {
                    val rawText = clip.getItemAt(i)?.coerceToText(getApplication())?.toString()?.trim() ?: continue
                    if (rawText.isBlank()) continue

                    val urls = DownloadHelper.extractAllUrls(rawText)
                    for (rawUrl in urls) {
                        val clean = DownloadHelper.cleanUrl(rawUrl)
                        if (DownloadHelper.isSocialMediaLink(clean)) {
                            val existing = savedDao.findByUrl(clean)
                            if (existing == null) {
                                val platform = DownloadHelper.detectPlatform(clean)
                                lastPlatform = platform
                                savedDao.insert(
                                    SavedLink(
                                        url = clean,
                                        platform = platform,
                                        title = "$platform Video",
                                        detectedAt = System.currentTimeMillis()
                                    )
                                )
                                fetchAndStoreMetadata(clean)
                                newCount++
                            }
                        }
                    }
                }

                if (newCount > 0) {
                    _message.value = if (newCount == 1) {
                        "New $lastPlatform link detected & saved ✓"
                    } else {
                        "$newCount new video links detected & saved ✓"
                    }
                }
            } catch (e: Throwable) {
                // Clipboard privacy limits on Android 12+ — silently ignore
            }
        }
    }

    fun pasteFromClipboard(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                val urls = DownloadHelper.extractAllUrls(trimmed)
                if (urls.isNotEmpty()) {
                    _url.value = urls.first()
                    var newCount = 0
                    for (rawUrl in urls) {
                        val clean = DownloadHelper.cleanUrl(rawUrl)
                        if (DownloadHelper.isSocialMediaLink(clean)) {
                            val existing = savedDao.findByUrl(clean)
                            if (existing == null) {
                                val platform = DownloadHelper.detectPlatform(clean)
                                savedDao.insert(
                                    SavedLink(
                                        url = clean,
                                        platform = platform,
                                        title = "$platform Video",
                                        detectedAt = System.currentTimeMillis()
                                    )
                                )
                                fetchAndStoreMetadata(clean)
                                newCount++
                            }
                        }
                    }
                    if (newCount > 1) {
                        _message.value = "$newCount links added to Saved Links ✓"
                    }
                } else {
                    _url.value = trimmed
                }
            }
        }
    }

    /** Tap the orb: if url set, download it. Otherwise grab from clipboard. */
    fun downloadCurrentOrClipboard() {
        // Check engine state
        val engineReady = engineInitState.value
        if (engineReady == null) {
            _message.value = "Engine still loading, please wait…"
            return
        }
        if (engineReady == false) {
            _message.value = "Engine failed to load. Try restarting the app."
            return
        }

        // Use typed URL first
        if (_url.value.isNotBlank()) {
            startDownload(_url.value)
            return
        }

        // Try first undownloaded saved link
        val unDownloaded = savedLinks.value.firstOrNull { !it.isDownloaded }
        if (unDownloaded != null) {
            startDownload(unDownloaded.url)
            return
        }

        // Fall back to clipboard
        viewModelScope.launch {
            try {
                val clipboard = getApplication<Application>()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard == null || !clipboard.hasPrimaryClip()) {
                    _message.value = "Copy a video link first!"
                    return@launch
                }
                val clip = clipboard.primaryClip ?: run {
                    _message.value = "Clipboard empty — copy a video link!"
                    return@launch
                }
                val text = clip.getItemAt(0)?.coerceToText(getApplication())?.toString()?.trim() ?: ""
                val detected = DownloadHelper.extractUrl(text)
                if (detected != null && DownloadHelper.isSocialMediaLink(detected)) {
                    val clean = DownloadHelper.cleanUrl(detected)
                    if (savedDao.findByUrl(clean) == null) {
                        val platform = DownloadHelper.detectPlatform(clean)
                        savedDao.insert(
                            SavedLink(url = clean, platform = platform, title = "Quick Download", detectedAt = System.currentTimeMillis())
                        )
                    }
                    startDownload(clean)
                } else {
                    _message.value = "No video link in clipboard!"
                }
            } catch (e: Throwable) {
                _message.value = "Copy a video link and tap again!"
            }
        }
    }

    fun startDownload(urlToDownload: String? = null, qualityOverride: String? = null, timeRangeOverride: String? = null) {
        val currentUrl = urlToDownload ?: _url.value
        if (currentUrl.isBlank()) {
            _message.value = "Paste a video link first"
            return
        }

        // Guard: check engine
        if (engineInitState.value != true) {
            _message.value = if (engineInitState.value == null) "Engine loading…" else "Engine failed. Restart app."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val clean = DownloadHelper.cleanUrl(currentUrl)
                val platform = DownloadHelper.detectPlatform(clean)
                val q = qualityOverride ?: _selectedQuality.value

                val saved = savedDao.findByUrl(clean)
                val initialTitle = saved?.title?.takeIf {
                    it != "Loading preview…" && it != "Copied link" && it.isNotBlank()
                } ?: "Downloading…"

                val rawRange = timeRangeOverride ?: if (_trimEnabled.value && _trimStartTime.value.isNotBlank() && _trimEndTime.value.isNotBlank()) {
                    "${_trimStartTime.value.trim()}-${_trimEndTime.value.trim()}"
                } else null
                val timeRange = rawRange?.let { DownloadHelper.normalizeDownloadSections(it) } ?: rawRange

                val item = DownloadItem(
                    url = clean,
                    title = if (!timeRange.isNullOrBlank()) "$initialTitle [Trim $timeRange]" else initialTitle,
                    description = saved?.note,
                    thumbnail = saved?.thumbnail,
                    platform = platform,
                    quality = q,
                    status = "Queued"
                )
                val id = downloadDao.insertDownload(item)

                val serviceIntent = Intent(getApplication(), DownloadService::class.java).apply {
                    putExtra(DownloadService.EXTRA_URL, clean)
                    putExtra(DownloadService.EXTRA_QUALITY, q)
                    putExtra(DownloadService.EXTRA_ITEM_ID, id)
                    if (!timeRange.isNullOrBlank()) {
                        putExtra(DownloadService.EXTRA_TIME_RANGE, timeRange)
                    }
                }

                try {
                    getApplication<Application>().startForegroundService(serviceIntent)
                } catch (t: Throwable) {
                    // Fallback for edge cases
                    getApplication<Application>().startService(serviceIntent)
                }

                if (saved != null) {
                    savedDao.update(saved.copy(isDownloaded = true))
                }

                _message.value = if (!timeRange.isNullOrBlank()) "⬇ Trimming & downloading section ($timeRange)..." else "⬇ Download started!"
                if (urlToDownload == null) _url.value = ""

            } catch (e: Exception) {
                _message.value = "Error starting download: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun trimExistingDownload(item: DownloadItem, start: String, end: String) {
        val path = item.filePath
        if (path.isNullOrBlank()) {
            _message.value = "File path not available for trimming"
            return
        }
        viewModelScope.launch {
            _message.value = "✂️ Trimming video ($start to $end)…"
            val isAudio = item.quality == "Audio Only"
            val result = com.kmb.downloader.tools.VideoTrimmerHelper.trimMedia(
                getApplication(),
                path,
                start,
                end,
                isAudioOnly = isAudio
            )
            result.fold(
                onSuccess = { file ->
                    _message.value = "✓ Trimmed clip saved to Gallery & Downloads!"
                },
                onFailure = { err ->
                    _message.value = "Trim error: ${err.message}"
                }
            )
        }
    }

    fun cancelDownload(itemId: Long) {
        val intent = Intent(getApplication(), DownloadService::class.java).apply {
            action = DownloadService.ACTION_CANCEL
            putExtra(DownloadService.EXTRA_ITEM_ID, itemId)
        }
        getApplication<Application>().startService(intent)
        _message.value = "Cancelled"
    }

    fun openFile(context: Context, item: DownloadItem) {
        val path = item.filePath ?: return
        try {
            val uri: android.net.Uri = if (path.startsWith("content://")) {
                android.net.Uri.parse(path)
            } else {
                val file = File(path)
                if (!file.exists()) {
                    _message.value = "File not found on device"
                    return
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val mimeType = when {
                item.quality == "Audio Only" || path.endsWith(".m4a") || path.endsWith(".mp3") -> "audio/*"
                else -> "video/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Play with"))
        } catch (e: Exception) {
            _message.value = "No player found: ${e.message}"
        }
    }

    fun shareFile(context: Context, item: DownloadItem) {
        val path = item.filePath ?: return
        try {
            val uri: android.net.Uri = if (path.startsWith("content://")) {
                android.net.Uri.parse(path)
            } else {
                val file = File(path)
                if (!file.exists()) {
                    _message.value = "File not found"
                    return
                }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
            val mimeType = when {
                item.quality == "Audio Only" || path.endsWith(".m4a") || path.endsWith(".mp3") -> "audio/*"
                else -> "video/*"
            }
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    },
                    "Share media"
                )
            )
        } catch (e: Exception) {
            _message.value = "Cannot share: ${e.message}"
        }
    }

    fun deleteDownload(item: DownloadItem) {
        viewModelScope.launch {
            item.filePath?.let { path ->
                runCatching { File(path).delete() }
            }
            downloadDao.deleteDownload(item.id)
            _message.value = "Removed"
        }
    }

    fun updateEngine() {
        viewModelScope.launch {
            _isUpdatingEngine.value = true
            _message.value = "Updating yt-dlp engine…"
            val res = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DownloadHelper.updateEngine(getApplication())
            }
            _message.value = if (res.isSuccess) "yt-dlp updated ✓" else "Update failed: ${res.exceptionOrNull()?.message}"
            _isUpdatingEngine.value = false
        }
    }

    fun deleteSavedLink(id: Long) {
        viewModelScope.launch { savedDao.delete(id) }
    }

    fun downloadAllSaved() {
        val pending = savedLinks.value.filter { !it.isDownloaded }
        if (pending.isEmpty()) {
            _message.value = "No pending links to download"
            return
        }
        viewModelScope.launch {
            _message.value = "Starting ${pending.size} downloads…"
            for (link in pending) {
                startDownload(link.url)
            }
        }
    }

    fun clearSavedLinks() {
        viewModelScope.launch { savedDao.clearAll() }
    }

    fun clearHistory() {
        viewModelScope.launch { downloadDao.clearDownloads() }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun formatDate(timestamp: Long): String {
        return try {
            if (timestamp <= 0L) ""
            else SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault()).format(Date(timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    private fun fetchAndStoreMetadata(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val meta = MetadataFetcherHelper.fetchMetadata(url) ?: return@launch
            val existing = savedDao.findByUrl(url) ?: return@launch
            savedDao.update(
                existing.copy(
                    title = meta.title,
                    note = meta.description,
                    thumbnail = meta.thumbnail ?: existing.thumbnail
                )
            )
        }
    }
}
