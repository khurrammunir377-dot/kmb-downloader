package com.kmb.downloader.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.mutableIntStateOf
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
    var detailsItemToShow by remember { mutableStateOf<MediaDetailsInfo?>(null) }
    var selectedMediaTab by remember { mutableIntStateOf(0) }
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
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.kmb.downloader.R.drawable.kmb_ribbon_logo),
                            contentDescription = "KMB Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "KMB ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        fontSize = 16.sp
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "DOWNLOADER",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 15.sp
                                    ),
                                    color = KmbPrimary
                                )
                            }
                            Text(
                                text = "Download • Save • Enjoy",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                                color = com.kmb.downloader.ui.theme.TextSecondary
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

            // ===== 2-TAB SEGMENTED SELECTOR: SAVED LINKS vs DOWNLOAD HISTORY =====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C2234))
                        .border(1.dp, Color(0xFF2E3850), RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .then(
                                if (selectedMediaTab == 0) Modifier.background(Brush.horizontalGradient(listOf(KmbPrimary, KmbPrimaryLight)))
                                else Modifier.background(Color.Transparent)
                            )
                            .clickable { selectedMediaTab = 0 }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Rounded.Bookmark,
                                contentDescription = null,
                                tint = if (selectedMediaTab == 0) Color.White else Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Saved Links (${savedLinks.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMediaTab == 0) Color.White else Color(0xFF9CA3AF)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .then(
                                if (selectedMediaTab == 1) Modifier.background(Brush.horizontalGradient(listOf(KmbPrimary, KmbPrimaryLight)))
                                else Modifier.background(Color.Transparent)
                            )
                            .clickable { selectedMediaTab = 1 }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = if (selectedMediaTab == 1) Color.White else Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Download History (${downloads.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMediaTab == 1) Color.White else Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }

            // ===== TAB 0: SAVED LINKS =====
            if (selectedMediaTab == 0) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = if (savedLinks.isNotEmpty()) "${savedLinks.size} Links Stored" else "Saved Links",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        if (pendingCount > 1) {
                            TextButton(onClick = { viewModel.downloadAllSaved() }) {
                                Text("Download All ($pendingCount)", color = KmbSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        IconButton(onClick = { viewModel.checkClipboardAndSave() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Scan clipboard", tint = KmbSecondary, modifier = Modifier.size(17.dp))
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
                        val formattedDate = viewModel.formatDate(link.detectedAt)
                        CompactSavedLinkCard(
                            link = link,
                            dateText = formattedDate,
                            onDownload = { viewModel.startDownload(link.url) },
                            onDelete = { viewModel.deleteSavedLink(link.id) },
                            onClickIcon = {
                                val cleanTitle = link.title?.takeIf { it.isNotBlank() && it != "Quick Download" && !it.endsWith("Video") }
                                    ?: link.title ?: link.url
                                val cleanDesc = link.note?.takeIf { it.isNotBlank() }
                                    ?: "Preserved offline link from ${link.platform}"
                                detailsItemToShow = MediaDetailsInfo(
                                    title = cleanTitle,
                                    platform = link.platform,
                                    url = link.url,
                                    dateText = formattedDate,
                                    quality = "Original Web Stream",
                                    status = if (link.isDownloaded) "Downloaded" else "Saved for Later",
                                    description = cleanDesc,
                                    thumbnail = link.thumbnail,
                                    onDownload = { viewModel.startDownload(link.url) }
                                )
                            }
                        )
                    }
                }
            }

            // ===== TAB 1: DOWNLOAD HISTORY =====
            if (selectedMediaTab == 1) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = if (downloads.isNotEmpty()) "${downloads.size} Downloads Complete" else "Download History",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        if (downloads.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Text("Clear History", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
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
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(
                        items = downloads,
                        key = { item -> "download_${item.id}_${item.url.hashCode()}" }
                    ) { item ->
                        val liveP = liveProgressMap[item.id]
                        val formattedDate = viewModel.formatDate(item.createdAt)
                        CompactDownloadHistoryCard(
                            item = item,
                            liveProgress = liveP,
                            onPlay = { viewModel.openFile(context, item) },
                            onShare = { viewModel.shareFile(context, item) },
                            onTrim = {
                                if (!proState.isPro) {
                                    showReferralDialog = true
                                    scope.launch {
                                        snackbarHostState.showSnackbar("👑 Video Trimming is a VIP PRO feature. Refer friends to unlock PRO free!")
                                    }
                                } else {
                                    itemToTrim = item
                                }
                            },
                            onAudioStudio = { audioStudioItem = item },
                            onCancel = { viewModel.cancelDownload(item.id) },
                            onDelete = { viewModel.deleteDownload(item) },
                            onClickIcon = {
                                val cleanTitle = item.title.takeIf { it.isNotBlank() && it != "Unknown" && it != "Downloading…" }
                                    ?: item.url
                                val cleanDesc = item.description?.takeIf { it.isNotBlank() && it != cleanTitle }
                                    ?: "Downloaded video from ${item.platform} (${item.quality})"
                                detailsItemToShow = MediaDetailsInfo(
                                    title = cleanTitle,
                                    platform = item.platform,
                                    url = item.url,
                                    dateText = formattedDate,
                                    quality = item.quality,
                                    fileSize = item.fileSize,
                                    status = item.status,
                                    filePath = item.filePath,
                                    description = cleanDesc,
                                    thumbnail = item.thumbnail,
                                    onPlay = if (item.status == "Completed") { { viewModel.openFile(context, item) } } else null,
                                    onShare = if (item.status == "Completed") { { viewModel.shareFile(context, item) } } else null,
                                    onTrim = if (item.status == "Completed") {
                                        {
                                            if (!proState.isPro) {
                                                showReferralDialog = true
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("👑 Video Trimmer is a VIP PRO feature. Refer friends to unlock PRO free!")
                                                }
                                            } else {
                                                itemToTrim = item
                                            }
                                        }
                                    } else null
                                )
                            }
                        )
                    }
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
                    Text("KMB Downloader v3.10.5", fontWeight = FontWeight.Bold)
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

    // Comprehensive Media & Link Details Dialog
    detailsItemToShow?.let { info ->
        MediaDetailsDialog(
            info = info,
            onDismiss = { detailsItemToShow = null }
        )
    }
}

data class MediaDetailsInfo(
    val title: String,
    val platform: String,
    val url: String,
    val dateText: String,
    val duration: String? = null,
    val quality: String? = null,
    val fileSize: String? = null,
    val status: String? = null,
    val filePath: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val onDownload: (() -> Unit)? = null,
    val onPlay: (() -> Unit)? = null,
    val onShare: (() -> Unit)? = null,
    val onTrim: (() -> Unit)? = null
)

/**
 * Authentic Platform Icon Badge
 */
@Composable
fun PlatformIconBadge(
    platform: String,
    thumbnail: String? = null,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 38.dp,
    onClick: (() -> Unit)? = null
) {
    val norm = platform.lowercase()
    val isYouTube = norm.contains("youtube") || norm.contains("youtu")
    val isTikTok = norm.contains("tiktok")
    val isInstagram = norm.contains("instagram") || norm.contains("insta")
    val isFacebook = norm.contains("facebook") || norm.contains("fb")
    val isTwitter = norm.contains("twitter") || norm == "x" || norm.contains("x.com")
    val isWhatsApp = norm.contains("whatsapp")
    val isPinterest = norm.contains("pinterest")
    val isReddit = norm.contains("reddit")

    val (badgeBg, badgeLabel) = when {
        isYouTube -> Pair(Color(0xFFFF0000), "▶")
        isTikTok -> Pair(Color(0xFF010101), "♪")
        isInstagram -> Pair(Color(0xFFE1306C), "📷")
        isFacebook -> Pair(Color(0xFF1877F2), "f")
        isTwitter -> Pair(Color(0xFF000000), "𝕏")
        isWhatsApp -> Pair(Color(0xFF25D366), "💬")
        isPinterest -> Pair(Color(0xFFE60023), "P")
        isReddit -> Pair(Color(0xFFFF4500), "🔴")
        else -> Pair(KmbPrimary, platform.take(2).uppercase())
    }

    val instaBrush = if (isInstagram) {
        Brush.linearGradient(listOf(Color(0xFF833AB4), Color(0xFFFD1D1D), Color(0xFFFCB045)))
    } else {
        Brush.linearGradient(listOf(badgeBg, badgeBg.copy(alpha = 0.88f)))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(9.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnail.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnail)
                    .crossfade(true)
                    .build(),
                contentDescription = platform,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Authentic platform badge overlay in corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(1.dp)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(instaBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(instaBrush),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badgeLabel,
                    fontSize = if (badgeLabel.length <= 2) 13.sp else 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Compact Saved Link Card (Reduced height, authentic platform icon & copy button)
 */
@Composable
fun CompactSavedLinkCard(
    link: SavedLink,
    dateText: String,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onClickIcon: () -> Unit
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
            PlatformIconBadge(
                platform = link.platform,
                thumbnail = link.thumbnail,
                size = 38.dp,
                onClick = onClickIcon
            )

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

            // Copy Link Button
            IconButton(
                onClick = {
                    val clip = ClipData.newPlainText("URL", link.url)
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(clip)
                    Toast.makeText(context, "Link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy Link",
                    tint = KmbSecondary,
                    modifier = Modifier.size(17.dp)
                )
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
 * Compact Download History Card (With authentic clickable platform icon, Play, Share, and ✂️ Trim)
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
    onDelete: () -> Unit,
    onClickIcon: () -> Unit
) {
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
                PlatformIconBadge(
                    platform = item.platform,
                    thumbnail = item.thumbnail,
                    size = 38.dp,
                    onClick = onClickIcon
                )

                Spacer(Modifier.width(10.dp))

                // Middle Info: Title (Line 1), Description (Line 2) & Subtitle (Line 3: Platform • Quality • Size)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title.ifBlank { item.url },
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!item.description.isNullOrBlank() && item.description != item.title) {
                        Spacer(Modifier.height(1.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

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

/**
 * Media Details Dialog (Shows title, original app icon, date/time, duration, size, and actions)
 */
@Composable
fun MediaDetailsDialog(
    info: MediaDetailsInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlatformIconBadge(platform = info.platform, thumbnail = info.thumbnail, size = 36.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.platform,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = info.status ?: "Media Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = KmbSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Video Title
                Column {
                    Text(
                        text = "TITLE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = info.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Description (if available)
                if (!info.description.isNullOrBlank()) {
                    Column {
                        Text(
                            text = "DESCRIPTION",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = info.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Date & Time + Format
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "DATE & TIME",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = info.dateText.ifBlank { "Recently Added" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!info.quality.isNullOrBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "QUALITY / FORMAT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = info.quality,
                                style = MaterialTheme.typography.bodySmall,
                                color = KmbPrimaryLight
                            )
                        }
                    }
                }

                // File Size & Duration
                if (!info.fileSize.isNullOrBlank() || !info.duration.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!info.fileSize.isNullOrBlank()) {
                            Column {
                                Text(
                                    text = "FILE SIZE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = info.fileSize,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (!info.duration.isNullOrBlank()) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "DURATION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = info.duration,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // File Path (if stored locally)
                if (!info.filePath.isNullOrBlank()) {
                    Column {
                        Text(
                            text = "STORED AT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = info.filePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // URL + 1-Tap Copy
                Column {
                    Text(
                        text = "SOURCE URL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(3.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = info.url,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy URL",
                            tint = KmbSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    val clip = ClipData.newPlainText("URL", info.url)
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(clip)
                                    Toast.makeText(context, "Link copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (info.onPlay != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            info.onPlay.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KmbSecondary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Play", color = Color.White, fontSize = 12.sp)
                    }
                }
                if (info.onDownload != null) {
                    Button(
                        onClick = {
                            onDismiss()
                            info.onDownload.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Download", color = Color.White, fontSize = 12.sp)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    )
}

