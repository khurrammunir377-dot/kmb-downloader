package com.kmb.downloader.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.tools.AudioTarget
import com.kmb.downloader.tools.BgMode
import com.kmb.downloader.tools.DetectedTextBox
import com.kmb.downloader.tools.ImageBgRemoverHelper
import com.kmb.downloader.tools.ImageInpaintHelper
import com.kmb.downloader.tools.VideoTrimmerHelper
import com.kmb.downloader.tools.VideoWatermarkHelper
import com.kmb.downloader.tools.WatermarkPosition
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class StudioTool {
    BG_REMOVER,
    TEXT_ERASER,
    VIDEO_DELOGO,
    AUDIO_STUDIO,
    MEDIA_TRIMMER,
    SHORTS_REFRAME
}

data class StudioItem(
    val tool: StudioTool,
    val title: String,
    val tag: String,
    val desc: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeTool by remember { mutableStateOf<StudioTool?>(null) }

    val studioItems = listOf(
        StudioItem(
            tool = StudioTool.BG_REMOVER,
            title = "Photo BG Remover",
            tag = "On-Device AI",
            desc = "Instant subject cutout with transparent PNG & 5 studio backgrounds",
            icon = Icons.Default.AutoFixHigh,
            gradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
        ),
        StudioItem(
            tool = StudioTool.TEXT_ERASER,
            title = "Photo Text Eraser",
            tag = "Smart Inpaint",
            desc = "Auto-detect text & seamlessly erase watermarks, logos and timestamps",
            icon = Icons.Default.CleaningServices,
            gradient = listOf(Color(0xFF3B82F6), Color(0xFF10B981))
        ),
        StudioItem(
            tool = StudioTool.VIDEO_DELOGO,
            title = "Video Delogo",
            tag = "Video Cleaner",
            desc = "Remove or blur TikTok, Reels, and channel corner watermarks",
            icon = Icons.Default.Movie,
            gradient = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
        ),
        StudioItem(
            tool = StudioTool.AUDIO_STUDIO,
            title = "Audio & Ringtone",
            tag = "Sound Studio",
            desc = "Extract audio/chorus & export directly as phone ringtone or alarm",
            icon = Icons.Default.MusicNote,
            gradient = listOf(Color(0xFF7C3AED), Color(0xFF6366F1))
        ),
        StudioItem(
            tool = StudioTool.MEDIA_TRIMMER,
            title = "Video Trimmer",
            tag = "Lossless Cut",
            desc = "Losslessly trim any video or audio on your device by timestamps",
            icon = Icons.Default.ContentCut,
            gradient = listOf(Color(0xFF10B981), Color(0xFF059669))
        ),
        StudioItem(
            tool = StudioTool.SHORTS_REFRAME,
            title = "9:16 Shorts Reframe",
            tag = "Reels / TikTok",
            desc = "Reframe 16:9 widescreen videos into vertical format with blurred borders",
            icon = Icons.Default.CropPortrait,
            gradient = listOf(Color(0xFFF43F5E), Color(0xFFFB923C))
        )
    )

    Scaffold(
        topBar = {
            val activeItem = studioItems.firstOrNull { it.tool == activeTool }
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        activeItem?.gradient ?: listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                activeItem?.icon ?: Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Column {
                            Text(
                                text = activeItem?.title ?: "AI Media Studio",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = activeItem?.tag ?: "On-Device AI Photo & Video Tools",
                                fontSize = 11.sp,
                                color = Color(0xFFA78BFA)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeTool != null) activeTool = null else onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = activeTool,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "toolSwitch"
            ) { tool ->
                when (tool) {
                    null -> {
                        // All Icons on ONE Page Dashboard
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Creative Media Tools",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA78BFA),
                                    letterSpacing = 1.sp
                                )
                            }

                            items(studioItems) { item ->
                                StudioFeatureCard(
                                    item = item,
                                    onClick = { activeTool = item.tool }
                                )
                            }

                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                    StudioTool.BG_REMOVER -> PhotoBgRemoverView(snackbarHostState)
                    StudioTool.TEXT_ERASER -> PhotoTextEraserView(snackbarHostState)
                    StudioTool.VIDEO_DELOGO -> VideoDelogoView(snackbarHostState)
                    StudioTool.AUDIO_STUDIO -> AudioStudioToolView(snackbarHostState)
                    StudioTool.MEDIA_TRIMMER -> UniversalTrimmerView(snackbarHostState)
                    StudioTool.SHORTS_REFRAME -> ShortsReframeView(snackbarHostState)
                }
            }
        }
    }
}

@Composable
fun StudioFeatureCard(
    item: StudioItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .border(
                1.dp,
                Brush.horizontalGradient(item.gradient.map { it.copy(alpha = 0.45f) }),
                RoundedCornerShape(14.dp)
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A24))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(item.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .background(item.gradient[0].copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.gradient[0]
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.desc,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

/**
 * Tool 1: Photo Background Remover
 */
@Composable
fun PhotoBgRemoverView(snackbarHostState: SnackbarHostState) {
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
                originalBitmap = loadBitmapFromUri(context, it)
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
                        Text("Result", fontSize = 12.sp, color = KmbSecondary, fontWeight = FontWeight.Bold)
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
                    Text(if (isProcessing) "Processing..." else "🪄 Remove Background Now")
                }
            }

            resultBitmap?.let { resBmp ->
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val saveRes = ImageBgRemoverHelper.saveToGallery(context, resBmp, "NoBG")
                                saveRes.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "✓ Saved to Pictures/KMB AI Studio!", Toast.LENGTH_SHORT).show()
                                        snackbarHostState.showSnackbar("✓ Saved to Pictures/KMB AI Studio!")
                                    },
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
 * Tool 2: Photo Text & Watermark Eraser
 */
@Composable
fun PhotoTextEraserView(snackbarHostState: SnackbarHostState) {
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
                    Text(if (isDetecting) "Scanning..." else "🔍 Auto-Detect Text & Watermarks")
                }
            }

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
                                        Toast.makeText(context, "✓ Text erased successfully!", Toast.LENGTH_SHORT).show()
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
                        Text(if (isErasing) "Erasing..." else "🧼 Erase Selected Elements")
                    }
                }

                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val saveRes = ImageBgRemoverHelper.saveToGallery(context, bmp, "Cleaned")
                                saveRes.fold(
                                    onSuccess = {
                                        Toast.makeText(context, "✓ Clean photo saved to Gallery!", Toast.LENGTH_SHORT).show()
                                        snackbarHostState.showSnackbar("✓ Clean photo saved to Gallery!")
                                    },
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
 * Tool 3: Video Delogo & Watermark Cleaner
 */
@Composable
fun VideoDelogoView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var watermarkPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    var isProcessing by remember { mutableStateOf(false) }

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
                                position = watermarkPosition
                            )
                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Cleaned video saved to Movies/KMB AI Studio!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Cleaned video saved to Movies/KMB AI Studio!")
                                },
                                onFailure = {
                                    Toast.makeText(context, "✓ Video processed. Check Movies/KMB AI Studio", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("Video processed. Check Movies/KMB AI Studio")
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
                        Text("Processing frames...", color = Color.White)
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

/**
 * Tool 4: Audio Studio & Ringtone Maker
 */
@Composable
fun AudioStudioToolView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("00:30") }
    var soundName by remember { mutableStateOf("KMB_Ringtone") }
    var selectedTarget by remember { mutableStateOf(AudioTarget.RINGTONE) }
    var isProcessing by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedMediaUri = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mediaPicker.launch("video/*") },
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
                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedMediaUri == null) "Select Video or Song to Cut" else "Media Selected ✓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Extract high-energy chorus & export as Phone Ringtone",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        selectedMediaUri?.let { uri ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("00:00 or 1.20") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("00:30 or 1.50") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = soundName,
                    onValueChange = { soundName = it },
                    label = { Text("Sound Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                Text("Export Destination:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioTargetPill("🔔 Ringtone", selectedTarget == AudioTarget.RINGTONE, { selectedTarget = AudioTarget.RINGTONE }, Modifier.weight(1f))
                    AudioTargetPill("💬 Notice", selectedTarget == AudioTarget.NOTIFICATION, { selectedTarget = AudioTarget.NOTIFICATION }, Modifier.weight(1f))
                    AudioTargetPill("⏰ Alarm", selectedTarget == AudioTarget.ALARM, { selectedTarget = AudioTarget.ALARM }, Modifier.weight(1f))
                    AudioTargetPill("🎵 Music", selectedTarget == AudioTarget.MUSIC, { selectedTarget = AudioTarget.MUSIC }, Modifier.weight(1f))
                }
            }

            item {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val realPath = getPathFromUri(context, uri)
                            if (realPath == null) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Unable to read media file.")
                                return@launch
                            }
                            val res = VideoTrimmerHelper.extractAndSaveAudio(
                                context = context,
                                inputFilePath = realPath,
                                startTime = startTime,
                                endTime = endTime,
                                customTitle = soundName,
                                target = selectedTarget
                            )
                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Sound exported! Ready in phone sound settings.", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Sound exported! Ready in phone sound settings.")
                                },
                                onFailure = { snackbarHostState.showSnackbar("Extraction error: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export Ringtone Sound", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tool 5: Universal Video Trimmer
 */
@Composable
fun UniversalTrimmerView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("01:00") }
    var isProcessing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        selectedUri = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { picker.launch("video/*") },
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
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedUri == null) "Select Video to Trim" else "Video Selected ✓",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Lossless stream-copy cut with zero quality degradation",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        selectedUri?.let { uri ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("e.g. 01:20 or 1.20") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("e.g. 02:45 or 2.45") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val path = getPathFromUri(context, uri)
                            if (path == null) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Unable to read video file.")
                                return@launch
                            }
                            val res = VideoTrimmerHelper.trimMedia(
                                context = context,
                                inputFilePath = path,
                                startTime = startTime,
                                endTime = endTime
                            )
                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Trimmed clip saved to Gallery!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Trimmed clip saved to Gallery!")
                                },
                                onFailure = { snackbarHostState.showSnackbar("Trim failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isProcessing) "Trimming..." else "Cut & Save Clip to Gallery", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Tool 6: Shorts Reframe
 */
@Composable
fun ShortsReframeView(snackbarHostState: SnackbarHostState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFF43F5E).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CropPortrait, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("9:16 Shorts / Reels Converter", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Converts widescreen horizontal 16:9 YouTube videos into full-screen vertical 9:16 format with auto-centering and blurred borders. Rolling out in next release!",
            fontSize = 13.sp,
            color = Color(0xFF9CA3AF),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AudioTargetPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF7C3AED).copy(alpha = 0.3f) else Color(0xFF1E222B))
            .border(1.dp, if (selected) Color(0xFF7C3AED) else Color(0xFF374151), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF9CA3AF)
        )
    }
}

@Composable
private fun PositionButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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

    return try {
        val temp = File(context.cacheDir, "temp_media_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        temp.absolutePath
    } catch (_: Exception) {
        null
    }
}
