package com.kmb.downloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val title: String = "Unknown",
    val thumbnail: String? = null,
    val platform: String = "Unknown",
    val quality: String = "Best",
    val filePath: String? = null,
    val status: String = "Queued",
    val progress: Float = 0f,
    val fileSize: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_links")
data class SavedLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val platform: String = "Unknown",
    val title: String? = null,
    val thumbnail: String? = null,
    val detectedAt: Long = System.currentTimeMillis(),
    val note: String? = null,
    val isDownloaded: Boolean = false
)
