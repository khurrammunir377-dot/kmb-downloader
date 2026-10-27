package com.kmb.downloader.data

import android.net.Uri

data class WhatsAppStatusItem(
    val uri: Uri,
    val name: String,
    val isVideo: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val isSaved: Boolean = false
)
