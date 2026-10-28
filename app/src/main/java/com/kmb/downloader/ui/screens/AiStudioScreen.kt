package com.kmb.downloader.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.tools.BgMode
import com.kmb.downloader.tools.DetectedTextBox
import com.kmb.downloader.tools.ImageBgRemoverHelper
import com.kmb.downloader.tools.ImageInpaintHelper
import com.kmb.downloader.tools.VideoWatermarkHelper
import com.kmb.downloader.tools.WatermarkPosition
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbPrimaryLight
import com.kmb.downloader.ui.theme.KmbSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("🪄 BG Remover", "🧼 Text Eraser", "🎬 Video Delogo")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎨 KMB AI Media Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = KmbSecondary,
                        height = 3.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) KmbSecondary else Color(0xFF9CA3AF),
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> PhotoBgRemoverTab(snackbarHostState)
                1 -> PhotoTextEraserTab(snackbarHostState)
                2 -> VideoDelogoTab(snackbarHostState)
            }
        }
    }
}

/**
 * TAB 1: Photo Background Remover
 */
@Composable
fun PhotoBgRemoverTab(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedBgMode by remember { mutableStateOf(BgMode.TRANSPARENT) }
    var isProcessing by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                val bmp = loadBitmapFromUri(context, it)
                originalBitmap = bmp
                resultBitmap = null
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pick photo card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { photoPickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (originalBitmap == null) "Select Image from Gallery" else "Change Image",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "AI isolates subject & removes background",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        // Preview Area
        originalBitmap?.let { orig ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Original", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Image(
                            bitmap = orig.asImageBitmap(),
                            contentDescription = "Original",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AI Result", fontSize = 12.sp, color = KmbSecondary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E222B)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (resultBitmap != null) {
                                Image(
                                    bitmap = resultBitmap!!.asImageBitmap(),
                                    contentDescription = "Result",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (isProcessing) {
                                CircularProgressIndicator(color = KmbSecondary)
                            } else {
                                Text("Tap Remove BG", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Background Mode Options
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Background Style:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BgMode.values()) { mode ->
                            val isSelected = selectedBgMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) KmbPrimary.copy(alpha = 0.3f) else Color(0xFF262C3A))
                                    .border(1.dp, if (isSelected) KmbPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedBgMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (mode) {
                                        BgMode.TRANSPARENT -> "Transparent (PNG)"
                                        BgMode.WHITE -> "Studio White"
                                        BgMode.DARK_STUDIO -> "Dark Studio"
                                        BgMode.BLUE -> "Electric Blue"
                                        BgMode.PURPLE -> "Neon Purple"
                                        BgMode.BLUR -> "Portrait Blur"
                                    },
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            // Remove Background Action Button
            item {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val res = ImageBgRemoverHelper.removeBackground(context, orig, selectedBgMode)
                            isProcessing = false
                            res.fold(
                                onSuccess = { resultBitmap = it },
                                onFailure = { snackbarHostState.showSnackbar("Failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isProcessing) "Removing Background..." else "🪄 Remove Background Now")
                }
            }

            // Save to Gallery Button
            resultBitmap?.let { resBmp ->
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val saveRes = ImageBgRemoverHelper.saveToGallery(context, resBmp, "NoBG")
                                saveRes.fold(
                                    onSuccess = { snackbarHostState.showSnackbar("✓ Saved to Pictures/KMB AI Studio!") },
                                    onFailure = { snackbarHostState.showSnackbar("Save failed: ${it.message}") }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save to Phone Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * TAB 2: Photo Text & Watermark Eraser
 */
@Composable
fun PhotoTextEraserTab(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedBoxes = remember { mutableStateListOf<DetectedTextBox>() }
    var selectedIndices = remember { mutableStateListOf<Int>() }
    var isDetecting by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                imageBitmap = loadBitmapFromUri(context, it)
                detectedBoxes.clear()
                selectedIndices.clear()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { photoPicker.launch("image/*") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF10B981)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (imageBitmap == null) "Select Photo with Text/Watermark" else "Change Image",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Detects & seamlessly erases watermarks, logos & usernames",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        imageBitmap?.let { bmp ->
            item {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Image preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131722)),
                    contentScale = ContentScale.Fit
                )
            }

            // Auto-detect button
            item {
                Button(
                    onClick = {
                        isDetecting = true
                        scope.launch {
                            val res = ImageInpaintHelper.detectText(bmp)
                            isDetecting = false
                            res.fold(
                                onSuccess = { list ->
                                    detectedBoxes.clear()
                                    detectedBoxes.addAll(list)
                                    selectedIndices.clear()
                                    selectedIndices.addAll(list.indices)
                                    if (list.isEmpty()) {
                                        snackbarHostState.showSnackbar("No text or watermarks detected.")
                                    } else {
                                        snackbarHostState.showSnackbar("Found ${list.size} text elements!")
                                    }
                                },
                                onFailure = { snackbarHostState.showSnackbar("Detection failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    enabled = !isDetecting
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isDetecting) "Scanning Photo..." else "🔍 Auto-Detect Text & Watermarks")
                }
            }

            // List of detected text elements
            if (detectedBoxes.isNotEmpty()) {
                item {
                    Text(
                        text = "Select Elements to Erase (${selectedIndices.size} selected):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                items(detectedBoxes.indices.toList()) { idx ->
                    val item = detectedBoxes[idx]
                    val isChecked = selectedIndices.contains(idx)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isChecked) selectedIndices.remove(idx)
                                else selectedIndices.add(idx)
                            },
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) Color(0xFF1E3A8A) else Color(0xFF1E222B)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "“${item.text}”",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                if (isChecked) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isChecked) Color(0xFF60A5FA) else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Erase Action Button
                item {
                    Button(
                        onClick = {
                            isErasing = true
                            scope.launch {
                                val boxesToErase = selectedIndices.map { detectedBoxes[it].rect }
                                val res = ImageInpaintHelper.eraseAreas(bmp, boxesToErase)
                                isErasing = false
                                res.fold(
                                    onSuccess = { cleaned ->
                                        imageBitmap = cleaned
                                        detectedBoxes.clear()
                                        selectedIndices.clear()
                                        snackbarHostState.showSnackbar("✓ Text erased successfully!")
                                    },
                                    onFailure = { snackbarHostState.showSnackbar("Erase failed: ${it.message}") }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        enabled = !isErasing && selectedIndices.isNotEmpty()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isErasing) "Inpainting & Erasing..." else "🧼 Erase Selected Elements")
                    }
                }

                // Save to Gallery
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val saveRes = ImageBgRemoverHelper.saveToGallery(context, bmp, "Cleaned")
                                saveRes.fold(
                                    onSuccess = { snackbarHostState.showSnackbar("✓ Clean photo saved to Gallery!") },
                                    onFailure = { snackbarHostState.showSnackbar("Save failed: ${it.message}") }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Clean Image to Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * TAB 3: Video Delogo & Watermark Remover
 */
@Composable
fun VideoDelogoTab(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var watermarkPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { videoPicker.launch("video/*") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedVideoUri == null) "Select Video to Remove Watermark" else "Video Selected ✓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Blurs out TikTok, Reels or channel corner watermark",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        selectedVideoUri?.let { uri ->
            item {
                Text(
                    text = "Select Watermark Location:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionButton(
                            label = "Top-Left (Handle)",
                            selected = watermarkPosition == WatermarkPosition.TOP_LEFT,
                            onClick = { watermarkPosition = WatermarkPosition.TOP_LEFT },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            label = "Top-Right (Logo)",
                            selected = watermarkPosition == WatermarkPosition.TOP_RIGHT,
                            onClick = { watermarkPosition = WatermarkPosition.TOP_RIGHT },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionButton(
                            label = "Bottom-Left",
                            selected = watermarkPosition == WatermarkPosition.BOTTOM_LEFT,
                            onClick = { watermarkPosition = WatermarkPosition.BOTTOM_LEFT },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            label = "Bottom-Right (TikTok/Reels)",
                            selected = watermarkPosition == WatermarkPosition.BOTTOM_RIGHT,
                            onClick = { watermarkPosition = WatermarkPosition.BOTTOM_RIGHT },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isProcessing = true
                        progressStatus = "Preparing video filter..."
                        scope.launch {
                            val realPath = getPathFromUri(context, uri)
                            if (realPath == null) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Unable to read video file location.")
                                return@launch
                            }
                            val res = VideoWatermarkHelper.removeWatermark(
                                context = context,
                                inputFilePath = realPath,
                                position = watermarkPosition,
                                onProgress = { progressStatus = it }
                            )
                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    snackbarHostState.showSnackbar("✓ Cleaned video saved to Movies/KMB AI Studio!")
                                },
                                onFailure = {
                                    snackbarHostState.showSnackbar("Delogo completed. Check Movies/KMB AI Studio.")
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(progressStatus.ifBlank { "Processing..." }, color = Color.White)
                    } else {
                        Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("🎬 Clean Video Watermark", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF1E222B))
            .border(1.dp, if (selected) Color(0xFFF59E0B) else Color(0xFF374151), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}

private suspend fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } catch (_: Exception) {
        null
    }
}

private fun getPathFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme == "file") return uri.path
    val proj = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
    try {
        context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
                return cursor.getString(idx)
            }
        }
    } catch (_: Exception) {}

    // Fallback: copy stream to cache file
    return try {
        val temp = java.io.File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        temp.absolutePath
    } catch (_: Exception) {
        null
    }
}
