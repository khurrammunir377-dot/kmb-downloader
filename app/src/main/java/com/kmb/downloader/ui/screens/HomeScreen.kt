package com.kmb.downloader.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.kmb.downloader.data.ReferralManager
import com.kmb.downloader.data.SavedLink
import com.kmb.downloader.ui.components.AudioStudioDialog
import com.kmb.downloader.ui.components.GlowCard
import com.kmb.downloader.ui.components.ReferralDialog
import com.kmb.downloader.ui.components.RotatingDownloadOrb
import kotlinx.coroutines.launch
import com.kmb.downloader.ui.theme.DarkBackground
import com.kmb.downloader.ui.theme.KmbGlow
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbPrimaryLight
import com.kmb.downloader.ui.theme.KmbSecondary
import com.kmb.downloader.viewmodel.MainViewModel

const val KMB_SUPPORT_EMAIL = "mkmbapps@gmail.com"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToWhatsAppStatus: () -> Unit = {},
    onNavigateToAiStudio: () -> Unit = {}
) {
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showManualInput by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showReferralDialog by remember { mutableStateOf(false) }
    var itemToTrim by remember { mutableStateOf<DownloadItem?>(null) }
    var audioStudioItem by remember { mutableStateOf<DownloadItem?>(null) }
    val proState by ReferralManager.proState.collectAsState()

    val trimEnabled by viewModel.trimEnabled.collectAsState()
    val trimStartTime by viewModel.trimStartTime.collectAsState()
    val trimEndTime by viewModel.trimEndTime.collectAsState()

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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 46.dp, height = 30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, KmbSecondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.kmb.downloader.R.drawable.kmb_logo),
                                contentDescription = "KMB Logo",
                                contentScale = ContentScale.Fit,
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
                                    letterSpacing = 1.1.sp,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Instant Media Grabber",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = KmbSecondary
                            )
                        }
                    }
                },
                actions = {
                    // VIP Crown Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (proState.isPro) Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFF9800)))
                                else Brush.horizontalGradient(listOf(Color(0xFF2E3440), Color(0xFF1E222B)))
                            )
                            .border(
                                1.dp,
                                if (proState.isPro) Color(0xFFFFD700) else Color(0xFF4B5563),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { showReferralDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (proState.isPro) "👑 PRO" else "🎁 VIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (proState.isPro) Color.Black else Color(0xFFFFD700)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    // 3 DOTS MENU BUTTON
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("👑 Refer & Earn VIP Pro", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700)) },
                                onClick = {
                                    showMenu = false
                                    showReferralDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CardGiftcard,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700)
                                    )
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("WhatsApp Status Saver", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToWhatsAppStatus()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFF25D366)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("AI Media Studio", fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA)) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToAiStudio()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = Color(0xFFA78BFA)
                                    )
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Update Download Engine") },
                                onClick = {
                                    showMenu = false
                                    viewModel.updateEngine()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = KmbSecondary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Scan Clipboard for Links") },
                                onClick = {
                                    showMenu = false
                                    viewModel.checkClipboardAndSave()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = KmbPrimaryLight)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Saved Links") },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearSavedLinks()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Download History") },
                                onClick = {
                                    showMenu = false
                                    viewModel.clearHistory()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Send Feedback") },
                                onClick = {
                                    showMenu = false
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:$KMB_SUPPORT_EMAIL")
                                        putExtra(Intent.EXTRA_SUBJECT, "KMB Downloader Feedback")
                                    }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null, tint = KmbSecondary)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About & Info") },
                                onClick = {
                                    showMenu = false
                                    showAboutDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = KmbSecondary)
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Engine status banner
            item {
                AnimatedVisibility(
                    visible = isEngineLoading || engineInitState == false,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
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
                Spacer(Modifier.height(4.dp))
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

                Spacer(Modifier.height(12.dp))

                // Prompt subtitle below the rotating orb
                val pendingSavedCount = savedLinks.count { !it.isDownloaded }
                Text(
                    text = if (isEngineLoading) {
                        "Loading engine… please wait before downloading"
                    } else if (isDownloading) {
                        if (downloadProgress <= 0f) "Connecting to ${activeDownload?.platform ?: "video"}…"
                        else "Downloading ${activeDownload?.platform} • ${downloadProgress.toInt()}%"
                    } else if (hasReadyLink) {
                        if (url.isNotBlank()) {
                            "Link Ready to Download\nTap the orb to start now!"
                        } else if (pendingSavedCount > 1) {
                            "$pendingSavedCount Links Saved & Ready!\nTap the orb to start downloading, or scroll down to view all."
                        } else {
                            "Link Ready: ${latestPending?.title ?: latestPending?.url ?: url}\nTap the orb to download now!"
                        }
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

                Spacer(Modifier.height(12.dp))

                // Quality Selector - ALL 5 BUTTONS SHIFTED IN THE EXACT SAME LINE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    viewModel.qualities.forEach { q ->
                        val isSelected = quality == q
                        val label = if (q == "Audio Only") "Audio" else q
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(KmbPrimary, KmbPrimaryLight))
                                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { viewModel.onQualitySelected(q) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Video Time-Range Trimmer Card (Optional)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (trimEnabled) KmbPrimary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (trimEnabled) KmbPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleTrim(!trimEnabled) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = if (trimEnabled) KmbPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Trim Specific Part (e.g. 01:50 to 02:55)",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (trimEnabled) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (trimEnabled) KmbPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = trimEnabled,
                                onCheckedChange = { viewModel.toggleTrim(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        AnimatedVisibility(visible = trimEnabled) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Downloads ONLY this section, saving your time and mobile data.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = trimStartTime,
                                        onValueChange = { viewModel.setTrimStartTime(it) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Start Time", fontSize = 11.sp) },
                                        placeholder = { Text("01:50", fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = trimEndTime,
                                        onValueChange = { viewModel.setTrimEndTime(it) },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("End Time", fontSize = 11.sp) },
                                        placeholder = { Text("02:55", fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
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
                        .padding(vertical = 2.dp)
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
                    GlowCard(modifier = Modifier.padding(top = 6.dp)) {
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
            val pendingCount = savedLinks.count { !it.isDownloaded }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                ) {
                    Icon(Icons.Rounded.Bookmark, contentDescription = null, tint = KmbPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (savedLinks.isNotEmpty()) "Saved Links (${savedLinks.size})" else "Saved Links",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.weight(1f))
                    if (pendingCount > 1) {
                        TextButton(onClick = { viewModel.downloadAllSaved() }) {
                            Text("Download All ($pendingCount)", color = KmbSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = { viewModel.checkClipboardAndSave() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan clipboard", tint = KmbSecondary, modifier = Modifier.size(18.dp))
                    }
                    if (savedLinks.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearSavedLinks() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (savedLinks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "No saved links yet.\n\nCopy any YouTube, TikTok, Instagram, Facebook, or X link anywhere on your phone. It will automatically appear here so you can download anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(
                    items = savedLinks,
                    key = { link -> "saved_${link.id}_${link.url.hashCode()}" }
                ) { link ->
                    CompactSavedLinkCard(
                        link = link,
                        dateText = viewModel.formatDate(link.detectedAt),
                        onDownload = { viewModel.startDownload(link.url) },
                        onDelete = { viewModel.deleteSavedLink(link.id) }
                    )
                }
            }

            // ===== DOWNLOAD HISTORY =====
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = KmbPrimary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (downloads.isNotEmpty()) "Download History (${downloads.size})" else "Download History",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.weight(1f))
                    if (downloads.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearHistory() }) {
                            Text("Clear", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (downloads.isEmpty()) {
                item {
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                items(
                    items = downloads,
                    key = { item -> "download_${item.id}_${item.url.hashCode()}" }
                ) { item ->
                    val liveP = liveProgressMap[item.id]
                    CompactDownloadHistoryCard(
                        item = item,
                        liveProgress = liveP,
                        onPlay = { viewModel.openFile(context, item) },
                        onShare = { viewModel.shareFile(context, item) },
                        onTrim = { itemToTrim = item },
                        onAudioStudio = { audioStudioItem = item },
                        onCancel = { viewModel.cancelDownload(item.id) },
                        onDelete = { viewModel.deleteDownload(item) }
                    )
                }
            }

            item { Spacer(Modifier.height(6.dp)) }
        }
    }

    // Video Trimming Dialog for Completed Items
    itemToTrim?.let { item ->
        var startTimeInput by remember { mutableStateOf("00:00") }
        var endTimeInput by remember { mutableStateOf("01:00") }

        AlertDialog(
            onDismissRequest = { itemToTrim = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, tint = KmbPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Trim Video / Audio Clip", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.title.ifBlank { item.url },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Enter timestamps (e.g. 00:30 to 01:45):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTimeInput,
                            onValueChange = { startTimeInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Start") },
                            placeholder = { Text("00:00") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTimeInput,
                            onValueChange = { endTimeInput = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("End") },
                            placeholder = { Text("01:00") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = itemToTrim
                        itemToTrim = null
                        if (target != null) {
                            viewModel.trimExistingDownload(target, startTimeInput, endTimeInput)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary)
                ) {
                    Text("Trim & Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToTrim = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // About & App Info Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = KmbSecondary)
                    Spacer(Modifier.width(8.dp))
                    Text("KMB Downloader v3.9.0", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Supercharged Media Downloader & Status Saver",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "• All-in-One video and audio downloader\n" +
                        "• Supports YouTube, Instagram, TikTok, Facebook, X, and 1000+ sites\n" +
                        "• Precision Time-Range Trimming (e.g. 01:50 to 02:55)\n" +
                        "• Full WhatsApp & WhatsApp Business Status Saver\n" +
                        "• Direct phone Gallery MediaStore integration\n" +
                        "• Ultra-fast multi-threaded download engine",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$KMB_SUPPORT_EMAIL")
                                putExtra(Intent.EXTRA_SUBJECT, "KMB Downloader Feedback")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = KmbSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Support: $KMB_SUPPORT_EMAIL",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = KmbSecondary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = KmbPrimary)
                }
            }
        )
    }

    // Referral & VIP Pro Dialog
    if (showReferralDialog) {
        ReferralDialog(
            onDismiss = { showReferralDialog = false },
            onShowMessage = { msg ->
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    // Audio Studio & Ringtone Maker Dialog
    audioStudioItem?.let { item ->
        AudioStudioDialog(
            item = item,
            onDismiss = { audioStudioItem = null },
            onCompleted = { msg ->
                scope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }
}

/**
 * Compact Saved Link Card (Reduced height & sleek layout)
 */
@Composable
fun CompactSavedLinkCard(
    link: SavedLink,
    dateText: String,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        KmbPrimary.copy(alpha = 0.35f),
                        KmbPrimaryLight.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
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
                        .size(width = 44.dp, height = 36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(KmbPrimary, KmbPrimaryLight))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = link.platform.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = link.title ?: link.url,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = link.platform,
                        style = MaterialTheme.typography.labelSmall,
                        color = KmbPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (!link.note.isNullOrBlank()) "${link.note} • $dateText" else dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (link.isDownloaded) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = KmbSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(2.dp))
            }

            IconButton(onClick = onDownload, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Download, contentDescription = "Download", tint = KmbPrimary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(17.dp))
            }
        }
    }
}

/**
 * Compact Download History Card (With 1-Tap Play, Share, and ✂️ Trim)
 */
@Composable
fun CompactDownloadHistoryCard(
    item: DownloadItem,
    liveProgress: Float? = null,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onTrim: () -> Unit,
    onAudioStudio: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val currentProgress = liveProgress ?: item.progress
    val isDownloading = item.status == "Downloading"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        KmbPrimary.copy(alpha = 0.35f),
                        KmbPrimaryLight.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Thumbnail or platform circle
                if (!item.thumbnail.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.thumbnail)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 44.dp, height = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(KmbPrimary, KmbPrimaryLight))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.platform.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Middle Info: Title (1 line) & Subtitle (Platform • Quality • Size)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { item.url },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))

                    val subtitleText = buildString {
                        append(item.platform)
                        append(" • ")
                        append(item.quality)
                        if (!item.fileSize.isNullOrBlank() && item.status != "Failed") {
                            append(" • ")
                            append(item.fileSize)
                        }
                        if (item.status != "Completed") {
                            append(" • ")
                            append(item.status)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (item.status == "Completed") {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = "Completed",
                                tint = KmbSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (item.status == "Failed") MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                // Action buttons right aligned
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.status == "Completed") {
                        IconButton(onClick = onPlay, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = KmbSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onTrim, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.ContentCut,
                                contentDescription = "Trim Clip",
                                tint = KmbPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onAudioStudio, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "Audio Studio & Ringtone",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onShare, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = KmbPrimaryLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    if (isDownloading) {
                        IconButton(onClick = onCancel, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Slim progress bar for downloading
            if (isDownloading) {
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { if (currentProgress > 0) currentProgress / 100f else 0.1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = KmbPrimary,
                    trackColor = KmbPrimary.copy(alpha = 0.2f)
                )
            }
        }
    }
}
