package com.kmb.downloader.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kmb.downloader.data.DownloadItem
import com.kmb.downloader.data.SavedLink
import com.kmb.downloader.ui.components.GlowCard
import com.kmb.downloader.ui.components.RotatingDownloadOrb
import com.kmb.downloader.ui.theme.KmbGlow
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbPrimaryLight
import com.kmb.downloader.ui.theme.KmbSecondary
import com.kmb.downloader.viewmodel.MainViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val url by viewModel.url.collectAsState()
    val quality by viewModel.selectedQuality.collectAsState()
    val isUpdatingEngine by viewModel.isUpdatingEngine.collectAsState()
    val message by viewModel.message.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val savedLinks by viewModel.savedLinks.collectAsState()
    val liveProgressMap by viewModel.liveProgress.collectAsState()
    val liveStatusMap by viewModel.liveStatus.collectAsState()
    val engineInitState by viewModel.engineInitState.collectAsState()
    val isEngineLoading by viewModel.isEngineLoading.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showManualInput by remember { mutableStateOf(false) }

    // Active downloading item
    val activeDownload = downloads.firstOrNull { it.status == "Downloading" }
    val isDownloading = activeDownload != null
    val downloadProgress = activeDownload?.let { liveProgressMap[it.id] ?: it.progress } ?: 0f
    val downloadStatus = activeDownload?.let { liveStatusMap[it.id] }

    // Ready link to download (either typed in url or last copied saved link)
    val latestPending = savedLinks.firstOrNull { !it.isDownloaded }
    val hasReadyLink = url.isNotBlank() || latestPending != null
    val readyPlatform = if (url.isNotBlank()) "URL Input" else latestPending?.platform

    val permissionLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { /* Handled */ }
        )
    } else null

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 56.dp, height = 34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, KmbSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.kmb.downloader.R.drawable.kmb_logo),
                                contentDescription = "KMB Logo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KMB DOWNLOADER",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Instant Media Grabber",
                                style = MaterialTheme.typography.bodySmall,
                                color = KmbSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = { viewModel.updateEngine() },
                        enabled = !isUpdatingEngine
                    ) {
                        if (isUpdatingEngine) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = KmbSecondary
                            )
                        } else {
                            Icon(
                                Icons.Default.SystemUpdate,
                                contentDescription = "Update yt-dlp engine",
                                tint = KmbSecondary
                            )
                        }
                    }
                }
            }

            // Engine status banner
            item {
                AnimatedVisibility(
                    visible = isEngineLoading || engineInitState == false,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(300))
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = if (engineInitState == false)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                KmbPrimary.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (isEngineLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = KmbPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = if (isEngineLoading)
                                    "Preparing download engine… (first launch takes ~60s)"
                                else
                                    "Engine failed to load. Please restart the app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (engineInitState == false)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    KmbPrimary
                            )
                        }
                    }
                }
            }

            // ===== HERO: ROTATING DOWNLOAD ORB =====
            item {
                Spacer(Modifier.height(8.dp))
                RotatingDownloadOrb(
                    isDownloading = isDownloading,
                    downloadProgress = downloadProgress,
                    hasReadyLink = hasReadyLink,
                    platformText = readyPlatform,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } catch (e: Throwable) {
                                // Ignore permission prompt errors
                            }
                        }
                        viewModel.downloadCurrentOrClipboard()
                    }
                )

                Spacer(Modifier.height(14.dp))

                // Prompt subtitle below the rotating orb
                Text(
                    text = if (isEngineLoading) {
                        "Loading engine… please wait before downloading"
                    } else if (isDownloading) {
                        if (downloadProgress <= 0f) "Connecting to ${activeDownload?.platform ?: "video"}…"
                        else "Downloading ${activeDownload?.platform} • ${downloadProgress.toInt()}%"
                    } else if (hasReadyLink) {
                        "Link Ready: ${latestPending?.title ?: latestPending?.url ?: url}\nTap the orb to download now!"
                    } else {
                        "Copy a video link anywhere on your phone,\nthen tap the orb to download immediately."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = if (isEngineLoading) MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                            else if (hasReadyLink) KmbPrimaryLight
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Quality Selector Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    viewModel.qualities.forEach { q ->
                        FilterChip(
                            selected = quality == q,
                            onClick = { viewModel.onQualitySelected(q) },
                            label = { Text(q, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KmbPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }


            // Manual Link Input (Expandable)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showManualInput = !showManualInput }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = KmbSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (showManualInput) "Hide manual link input" else "Or paste link manually",
                        style = MaterialTheme.typography.labelMedium,
                        color = KmbSecondary
                    )
                    Icon(
                        if (showManualInput) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = KmbSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(visible = showManualInput) {
                    GlowCard(modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedTextField(
                            value = url,
                            onValueChange = viewModel::onUrlChange,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste video link here…") },
                            leadingIcon = {
                                Icon(Icons.Default.Link, contentDescription = null, tint = KmbPrimary)
                            },
                            trailingIcon = {
                                Row {
                                    if (url.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.onUrlChange("") }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                    IconButton(onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = clipboard?.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val text = clip.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                                            viewModel.pasteFromClipboard(text)
                                        }
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = KmbSecondary)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KmbPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                cursorColor = KmbPrimary
                            )
                        )
                    }
                }
            }

            // ===== SAVED LINKS =====
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = KmbPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Saved Links",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { viewModel.checkClipboardAndSave() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan clipboard", tint = KmbSecondary)
                    }
                    if (savedLinks.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSavedLinks() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (savedLinks.isEmpty()) {
                item {
                    GlowCard {
                        Text(
                            text = "No saved links yet.\n\nCopy any YouTube, TikTok, Instagram, Facebook, or X link anywhere on your phone. It will automatically appear here with a video preview so you can download anytime.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                itemsIndexed(savedLinks, key = { _, item -> item.id }) { _, link ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(300)) + slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        SavedLinkCard(
                            link = link,
                            dateText = viewModel.formatDate(link.detectedAt),
                            onDownload = { viewModel.startDownload(link.url) },
                            onDelete = { viewModel.deleteSavedLink(link.id) }
                        )
                    }
                }
            }

            // ===== DOWNLOAD HISTORY =====
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = KmbPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Download History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    if (downloads.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (downloads.isEmpty()) {
                item {
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(downloads, key = { it.id }) { item ->
                    val liveP = liveProgressMap[item.id]
                    DownloadHistoryCard(
                        item = item,
                        liveProgress = liveP,
                        onPlay = { viewModel.openFile(context, item) },
                        onShare = { viewModel.shareFile(context, item) },
                        onCancel = { viewModel.cancelDownload(item.id) },
                        onDelete = { viewModel.deleteDownload(item) }
                    )
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun SavedLinkCard(
    link: SavedLink,
    dateText: String,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    GlowCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!link.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(link.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = link.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(KmbPrimary, KmbPrimaryLight))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = link.platform.take(2).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.platform,
                    style = MaterialTheme.typography.labelMedium,
                    color = KmbPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = link.title ?: link.url,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (!link.note.isNullOrBlank()) "${link.note} • $dateText" else dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (link.isDownloaded) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = KmbSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(4.dp))
            }

            IconButton(onClick = onDownload) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = KmbPrimary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun DownloadHistoryCard(
    item: DownloadItem,
    liveProgress: Float? = null,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val currentProgress = liveProgress ?: item.progress
    GlowCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.platform,
                style = MaterialTheme.typography.labelLarge,
                color = KmbPrimary
            )
            Text(
                text = item.status,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = when (item.status) {
                    "Completed" -> KmbSecondary
                    "Failed" -> MaterialTheme.colorScheme.error
                    "Cancelled" -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else -> KmbPrimaryLight
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!item.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.thumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { item.url },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.quality,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!item.fileSize.isNullOrBlank()) {
                        Text(
                            text = if (item.status == "Failed") "Error: ${item.fileSize}" else "• ${item.fileSize}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.status == "Failed") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Live progress bar if downloading
        if (item.status == "Downloading") {
            Spacer(Modifier.height(10.dp))
            if (currentProgress > 0) {
                LinearProgressIndicator(
                    progress = { currentProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = KmbPrimary,
                    trackColor = KmbPrimary.copy(alpha = 0.2f)
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = KmbPrimary,
                    trackColor = KmbPrimary.copy(alpha = 0.2f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentProgress > 0) "${currentProgress.toInt()}%" else "Connecting…",
                    style = MaterialTheme.typography.labelSmall,
                    color = KmbPrimaryLight
                )
                TextButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Action buttons when completed, failed, or cancelled
        if (item.status != "Downloading") {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.status == "Completed") {
                    IconButton(onClick = onPlay) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = KmbSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = onShare) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = KmbPrimaryLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
