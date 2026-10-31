package com.kmb.downloader.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kmb.downloader.data.WhatsAppStatusItem
import com.kmb.downloader.ui.theme.BackgroundDark
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbPrimaryLight
import com.kmb.downloader.ui.theme.KmbSecondary
import com.kmb.downloader.viewmodel.WhatsAppStatusViewModel
import com.kmb.downloader.whatsapp.WhatsAppStatusHelper

val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF128C7E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppStatusScreen(
    viewModel: WhatsAppStatusViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isBusiness by viewModel.isBusiness.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val statuses by viewModel.statuses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val isAppInstalled by viewModel.isAppInstalled.collectAsState()
    val previewItem by viewModel.previewItem.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDirectChatDialog by remember { mutableStateOf(false) }

    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderUriSelected(context, uri)
        }
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WhatsApp Status Saver",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                            Text(
                                text = if (isBusiness) "WhatsApp Business" else "Standard WhatsApp",
                                style = MaterialTheme.typography.labelSmall,
                                color = WhatsAppGreen
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadStatuses(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = KmbSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // App Type Toggle: WhatsApp vs WhatsApp Business
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Standard WhatsApp Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (!isBusiness) Brush.horizontalGradient(listOf(WhatsAppDarkGreen, WhatsAppGreen))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { viewModel.setBusiness(false, context) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WhatsApp",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (!isBusiness) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (!isBusiness) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // WhatsApp Business Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isBusiness) Brush.horizontalGradient(listOf(WhatsAppDarkGreen, WhatsAppGreen))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { viewModel.setBusiness(true, context) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WhatsApp Business",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isBusiness) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isBusiness) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Prominent Direct Chat Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { showDirectChatDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101E17)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direct Chat (No Contact Save)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = WhatsAppGreen
                        )
                        Text(
                            text = "Message any WhatsApp number directly without saving to contacts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "START 💬",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WhatsAppGreen
                    )
                }
            }

            // Filter Tabs: All, Photos, Videos
            val filteredStatuses = remember(statuses, selectedTab) {
                when (selectedTab) {
                    1 -> statuses.filter { !it.isVideo }
                    2 -> statuses.filter { it.isVideo }
                    else -> statuses
                }
            }

            val photoCount = remember(statuses) { statuses.count { !it.isVideo } }
            val videoCount = remember(statuses) { statuses.count { it.isVideo } }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = WhatsAppGreen
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("All (${statuses.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Photos ($photoCount)", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("Videos ($videoCount)", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            // Body Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = WhatsAppGreen)
                        }
                    }

                    !hasPermission -> {
                        // Permission Guidance Box
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.15f))
                                    .border(1.5.dp, WhatsAppGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = "Permission Required",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "To view and save WhatsApp statuses, grant access to the WhatsApp Statuses folder on your device.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(16.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("How it works:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text("1. View statuses inside WhatsApp first.", style = MaterialTheme.typography.bodySmall)
                                    Text("2. Tap 'Grant Permission' below.", style = MaterialTheme.typography.bodySmall)
                                    Text("3. In the picker, tap 'USE THIS FOLDER' and 'ALLOW'.", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val initialUri = WhatsAppStatusHelper.getInitialUri(isBusiness)
                                    openDocumentTreeLauncher.launch(initialUri)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Grant Permission", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    filteredStatuses.isEmpty() -> {
                        // Empty statuses state
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Text(
                                text = "No Statuses Found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "Open WhatsApp, view your friends' statuses, then return here to download or share them!",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(18.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { viewModel.loadStatuses(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Refresh", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        val pkg = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp"
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        } else {
                                            android.widget.Toast.makeText(context, "WhatsApp is not installed", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Open WhatsApp", color = Color.White)
                                }
                            }
                        }
                    }

                    else -> {
                        // Media Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 6.dp)
                        ) {
                            items(filteredStatuses, key = { it.uri.toString() }) { item ->
                                StatusCard(
                                    item = item,
                                    onClick = { viewModel.setPreview(item) },
                                    onSave = { viewModel.saveStatus(context, item) },
                                    onShare = { viewModel.shareStatus(context, item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // WhatsApp Direct Chat Dialog
    if (showDirectChatDialog) {
        var phoneInput by remember { mutableStateOf("") }
        var msgInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDirectChatDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(WhatsAppGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Direct Chat", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Chat without saving contact", style = MaterialTheme.typography.labelSmall, color = WhatsAppGreen)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter phone number with country code:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1234567890 / 03001234567") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = msgInput,
                        onValueChange = { msgInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Message (Optional)") },
                        placeholder = { Text("Hi, I wanted to reach out...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rawPhone = phoneInput.trim()
                        if (rawPhone.isNotBlank()) {
                            showDirectChatDialog = false
                            val cleanPhone = rawPhone.replace(Regex("[^0-9]"), "")
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msgInput.trim())}")
                            val chatIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                setPackage(if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp")
                            }
                            try {
                                context.startActivity(chatIntent)
                            } catch (e: Exception) {
                                val fallback = Intent(Intent.ACTION_VIEW, uri)
                                try { context.startActivity(fallback) } catch (_: Exception) {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Chat", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Full screen preview dialog
    previewItem?.let { item ->
        StatusPreviewDialog(
            item = item,
            onDismiss = { viewModel.setPreview(null) },
            onSave = { viewModel.saveStatus(context, item) },
            onShare = { viewModel.shareStatus(context, item) }
        )
    }
}

@Composable
fun StatusCard(
    item: WhatsAppStatusItem,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.uri)
                    .size(300, 300)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Video indicator badge on top right
            if (item.isVideo) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Video",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Gradient shadow at bottom for action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Bottom action icons: Share and Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onSave,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (item.isSaved) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = "Save to Gallery",
                        tint = if (item.isSaved) WhatsAppGreen else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusPreviewDialog(
    item: WhatsAppStatusItem,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Main media view
            if (item.isVideo) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            val mediaController = MediaController(ctx)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setVideoURI(item.uri)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(item.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                )
            }

            // Top Bar: Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, start = 16.dp, end = 16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    text = if (item.isVideo) "Video Status" else "Photo Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.width(36.dp))
            }

            // Bottom Bar: Save & Share buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Share", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WhatsAppGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (item.isSaved) Icons.Default.CheckCircle else Icons.Default.Download,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (item.isSaved) "Saved" else "Save to Gallery", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
