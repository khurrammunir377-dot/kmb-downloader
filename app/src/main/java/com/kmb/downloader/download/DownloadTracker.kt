package com.kmb.downloader.download

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, in-memory live progress tracker.
 * Decouples live 0%-100% updates from SQLite database writes to prevent
 * disk I/O saturation, database locks, and Compose recomposition storms.
 */
object DownloadTracker {

    // itemId -> progress (0f..100f)
    private val progressMap = ConcurrentHashMap<Long, Float>()
    private val _liveProgress = MutableStateFlow<Map<Long, Float>>(emptyMap())
    val liveProgress: StateFlow<Map<Long, Float>> = _liveProgress.asStateFlow()

    // itemId -> status string ("Connecting…", "Downloading 45%", etc.)
    private val statusMap = ConcurrentHashMap<Long, String>()
    private val _liveStatus = MutableStateFlow<Map<Long, String>>(emptyMap())
    val liveStatus: StateFlow<Map<Long, String>> = _liveStatus.asStateFlow()

    fun updateProgress(itemId: Long, progress: Float, statusText: String? = null) {
        progressMap[itemId] = progress
        _liveProgress.value = HashMap(progressMap)

        if (statusText != null) {
            statusMap[itemId] = statusText
            _liveStatus.value = HashMap(statusMap)
        }
    }

    fun updateStatus(itemId: Long, statusText: String) {
        statusMap[itemId] = statusText
        _liveStatus.value = HashMap(statusMap)
    }

    fun remove(itemId: Long) {
        progressMap.remove(itemId)
        statusMap.remove(itemId)
        _liveProgress.value = HashMap(progressMap)
        _liveStatus.value = HashMap(statusMap)
    }

    fun getProgress(itemId: Long): Float? = progressMap[itemId]

    fun getStatus(itemId: Long): String? = statusMap[itemId]
}
