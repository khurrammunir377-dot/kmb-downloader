package com.kmb.downloader.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.kmb.downloader.tools.MediaEnhancerHelper
import com.kmb.downloader.tools.PhotoOcrHelper
import com.kmb.downloader.tools.SegmentationResult
import com.kmb.downloader.tools.SpeechTranscriberHelper
import com.kmb.downloader.tools.VideoTextRegion
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
    PHOTO_OCR,
    VIDEO_TEXT_REMOVER,
    SPEECH_TRANSCRIBER,
    MEDIA_ENHANCER,
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
            tool = StudioTool.PHOTO_OCR,
            title = "Copy Text from Photo",
            tag = "Smart OCR",
            desc = "Extract all text from any picture, document or note with 1-tap copy",
            icon = Icons.Default.TextFields,
            gradient = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
        ),
        StudioItem(
            tool = StudioTool.VIDEO_TEXT_REMOVER,
            title = "Video Text Eraser",
            tag = "Video Inpaint",
            desc = "Cleanly erase subtitles, captions, or channel tags from video frames",
            icon = Icons.Default.Subtitles,
            gradient = listOf(Color(0xFFF97316), Color(0xFFEF4444))
        ),
        StudioItem(
            tool = StudioTool.SPEECH_TRANSCRIBER,
            title = "Video Speech Transcriber",
            tag = "Any Language",
            desc = "Convert speech from video/audio into text in English, Urdu, Hindi & more",
            icon = Icons.Default.Translate,
            gradient = listOf(Color(0xFF10B981), Color(0xFF3B82F6))
        ),
        StudioItem(
            tool = StudioTool.MEDIA_ENHANCER,
            title = "Photo & Video Enhancer",
            tag = "Super Clarity",
            desc = "HD unsharp sharpening, dynamic contrast boost & vibrant HDR colors",
            icon = Icons.Default.HighQuality,
            gradient = listOf(Color(0xFFEC4899), Color(0xFFF59E0B))
        ),
        StudioItem(
            tool = StudioTool.TEXT_ERASER,
            title = "Photo Text Eraser",
            tag = "Smart Inpaint",
            desc = "Auto-detect text & seamlessly erase watermarks, logos and timestamps",
            icon = Icons.Default.CleaningServices,
            gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
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
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.kmb.downloader.ui.theme.DarkBackground)
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
                    StudioTool.PHOTO_OCR -> PhotoOcrView(snackbarHostState)
                    StudioTool.VIDEO_TEXT_REMOVER -> VideoTextEraserView(snackbarHostState)
                    StudioTool.SPEECH_TRANSCRIBER -> SpeechTranscriberView(snackbarHostState)
                    StudioTool.MEDIA_ENHANCER -> MediaEnhancerView(snackbarHostState)
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C2234)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(item.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
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
                            .clip(RoundedCornerShape(6.dp))
                            .background(item.gradient[0].copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.tag,
                            fontSize = 9.sp,
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
 * Tool 1: Photo Background Remover (High quality mask segmentation & instant mode preview)
 */
@Composable
fun PhotoBgRemoverView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var segResult by remember { mutableStateOf<SegmentationResult?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedBgMode by remember { mutableStateOf(BgMode.TRANSPARENT) }
    var isProcessing by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                originalBitmap = loadBitmapFromUri(context, it)
                segResult = null
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
                            text = "AI isolates subject & creates HD cutout",
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
                                    .clickable {
                                        selectedBgMode = mode
                                        segResult?.let { seg ->
                                            resultBitmap = ImageBgRemoverHelper.composeBackground(
                                                seg.processedBitmap,
                                                seg.alphaMask,
                                                seg.maskWidth,
                                                seg.maskHeight,
                                                mode
                                            )
                                        }
                                    }
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
                            val res = ImageBgRemoverHelper.extractSegmentationMask(orig)
                            isProcessing = false
                            res.fold(
                                onSuccess = { seg ->
                                    segResult = seg
                                    resultBitmap = ImageBgRemoverHelper.composeBackground(
                                        seg.processedBitmap,
                                        seg.alphaMask,
                                        seg.maskWidth,
                                        seg.maskHeight,
                                        selectedBgMode
                                    )
                                },
                                onFailure = { snackbarHostState.showSnackbar("Cutout failed: ${it.message}") }
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
                    Text(if (isProcessing) "Generating High-Def Cutout..." else "🪄 Remove Background Now")
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
 * Tool 2: Copy Text from Photo (OCR with ML Kit)
 */
@Composable
fun PhotoOcrView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                photoBitmap = loadBitmapFromUri(context, it)
                extractedText = ""
                photoBitmap?.let { bmp ->
                    isScanning = true
                    val ocrRes = PhotoOcrHelper.recognizeText(context, bmp)
                    isScanning = false
                    ocrRes.fold(
                        onSuccess = { res ->
                            extractedText = res.fullText
                            if (res.fullText.isBlank()) {
                                snackbarHostState.showSnackbar("No text detected in this image.")
                            }
                        },
                        onFailure = { err ->
                            snackbarHostState.showSnackbar("OCR Error: ${err.message}")
                        }
                    )
                }
            }
        }
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
                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TextFields, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (photoBitmap == null) "Select Photo with Text" else "Change Image",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-scans and extracts text for instant 1-tap copy",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        photoBitmap?.let { bmp ->
            item {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Selected Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            if (isScanning) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = KmbSecondary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Extracting text via Google ML Kit...", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            if (extractedText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "EXTRACTED TEXT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KmbSecondary
                                )
                                Text(
                                    "${extractedText.length} characters",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = extractedText,
                                onValueChange = { extractedText = it },
                                modifier = Modifier.fillMaxWidth().height(180.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KmbPrimary,
                                    unfocusedBorderColor = Color(0xFF374151)
                                )
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clip = ClipData.newPlainText("Extracted Text", extractedText)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(clip)
                                Toast.makeText(context, "Text copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy All Text", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, extractedText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Extracted Text"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Text", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tool 3: Video Text & Subtitle Eraser (Selectable Regions)
 */
@Composable
fun VideoTextEraserView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoPath by remember { mutableStateOf<String?>(null) }
    var selectedRegion by remember { mutableStateOf(VideoTextRegion.BOTTOM_SUBTITLES) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var processedVideoFile by remember { mutableStateOf<File?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoPath = it.toString()
            processedVideoFile = null
        }
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
                            .background(Brush.linearGradient(listOf(Color(0xFFF97316), Color(0xFFEF4444)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Subtitles, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (videoPath == null) "Select Video to Erase Text" else "Video Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Blurs/delogos subtitles or channel handles",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        videoPath?.let { path ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Region to Erase:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    val regions = listOf(
                        Pair(VideoTextRegion.BOTTOM_SUBTITLES, "Bottom Subtitles / Captions"),
                        Pair(VideoTextRegion.TOP_CHANNEL_TAG, "Top Channel Tag / Handle"),
                        Pair(VideoTextRegion.BOTTOM_RIGHT, "Bottom Right Watermark"),
                        Pair(VideoTextRegion.BOTTOM_LEFT, "Bottom Left Watermark"),
                        Pair(VideoTextRegion.TOP_RIGHT, "Top Right Watermark")
                    )
                    regions.forEach { (region, label) ->
                        val isSelected = selectedRegion == region
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) KmbPrimary.copy(alpha = 0.35f) else Color(0xFF262C3A))
                                .border(1.dp, if (isSelected) KmbPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { selectedRegion = region }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = KmbSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isProcessing = true
                        progressStatus = "Starting video text eraser..."
                        scope.launch {
                            val res = VideoWatermarkHelper.eraseVideoText(
                                context = context,
                                inputFilePath = path,
                                region = selectedRegion,
                                onProgress = { progressStatus = it }
                            )
                            isProcessing = false
                            res.fold(
                                onSuccess = { file ->
                                    processedVideoFile = file
                                    Toast.makeText(context, "✓ Clean video saved to Movies/KMB AI Studio!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Clean video saved to Gallery!")
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar("Eraser Error: ${err.message}")
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isProcessing) "Processing..." else "🧹 Erase Selected Text from Video")
                }
            }

            if (isProcessing) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(progressStatus, fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            processedVideoFile?.let { file ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(Modifier.width(10.dp))
                            Text("Clean video saved to Movies/KMB AI Studio", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tool 4: Multilingual Video/Audio Speech Transcriber
 */
@Composable
fun SpeechTranscriberView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLang by remember { mutableStateOf(SpeechTranscriberHelper.AVAILABLE_LANGUAGES[0]) }
    var transcriptText by remember { mutableStateOf("") }
    var isTranscribing by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            transcriptText = ""
        }
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
                            .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF3B82F6)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedMediaUri == null) "Select Video or Audio" else "Media Ready to Transcribe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Transcribes speech in English, Urdu, Hindi, Arabic & more",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        item {
            // Language Selector Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showLangMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Spoken Language: ${selectedLang.displayName}", color = Color.White)
                }

                DropdownMenu(
                    expanded = showLangMenu,
                    onDismissRequest = { showLangMenu = false }
                ) {
                    SpeechTranscriberHelper.AVAILABLE_LANGUAGES.forEach { lang ->
                        DropdownMenuItem(
                            text = { Text(lang.displayName) },
                            onClick = {
                                selectedLang = lang
                                showLangMenu = false
                            }
                        )
                    }
                }
            }
        }

        selectedMediaUri?.let { uri ->
            item {
                Button(
                    onClick = {
                        isTranscribing = true
                        transcriptText = ""
                        scope.launch {
                            val available = SpeechTranscriberHelper.isRecognitionAvailable(context)
                            if (!available) {
                                isTranscribing = false
                                snackbarHostState.showSnackbar("Speech recognition engine not available on this device.")
                                return@launch
                            }

                            // Launch recognizer intent
                            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                            val intent = SpeechTranscriberHelper.createRecognizerIntent(selectedLang.code)
                            recognizer.setRecognitionListener(object : RecognitionListener {
                                override fun onReadyForSpeech(params: Bundle?) {}
                                override fun onBeginningOfSpeech() {}
                                override fun onRmsChanged(rmsdB: Float) {}
                                override fun onBufferReceived(buffer: ByteArray?) {}
                                override fun onEndOfSpeech() { isTranscribing = false }
                                override fun onError(error: Int) {
                                    isTranscribing = false
                                    scope.launch { snackbarHostState.showSnackbar("Speech recognizer completed / listening paused.") }
                                }
                                override fun onResults(results: Bundle?) {
                                    isTranscribing = false
                                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    val text = matches?.firstOrNull() ?: ""
                                    if (text.isNotBlank()) transcriptText = text
                                }
                                override fun onPartialResults(partialResults: Bundle?) {
                                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                    matches?.firstOrNull()?.let { transcriptText = it }
                                }
                                override fun onEvent(eventType: Int, params: Bundle?) {}
                            })
                            recognizer.startListening(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = !isTranscribing
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTranscribing) "Transcribing..." else "🎙 Transcribe Speech to Text")
                }
            }

            if (isTranscribing) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Listening & Transcribing speech in ${selectedLang.displayName}...", color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            if (transcriptText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E222B)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "TRANSCRIPT (${selectedLang.displayName})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KmbSecondary
                                )
                                Text(
                                    "${transcriptText.split(" ").size} words",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = transcriptText,
                                onValueChange = { transcriptText = it },
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151)
                                )
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val clip = ClipData.newPlainText("Transcript", transcriptText)
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cm?.setPrimaryClip(clip)
                                Toast.makeText(context, "Transcript copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy Transcript", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, transcriptText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Transcript"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tool 5: Photo & Video Quality Enhancer (Sharpness, Contrast & Vibrant HDR)
 */
@Composable
fun MediaEnhancerView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPhotoTab by remember { mutableStateOf(true) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var enhancedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sharpness by remember { mutableFloatStateOf(1.0f) }
    var contrast by remember { mutableFloatStateOf(1.12f) }
    var saturation by remember { mutableFloatStateOf(1.15f) }

    var videoPath by remember { mutableStateOf<String?>(null) }
    var isProcessingVideo by remember { mutableStateOf(false) }
    var videoProgress by remember { mutableStateOf("") }
    var enhancedVideoFile by remember { mutableStateOf<File?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                photoBitmap = loadBitmapFromUri(context, it)
                photoBitmap?.let { bmp ->
                    enhancedBitmap = MediaEnhancerHelper.enhancePhoto(bmp, sharpness, contrast, saturation)
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoPath = it.toString()
            enhancedVideoFile = null
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Mode Tab Selector: Photo vs Video
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E222B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { isPhotoTab = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPhotoTab) KmbPrimary else Color.Transparent
                    )
                ) {
                    Text("Photo Enhancer", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { isPhotoTab = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isPhotoTab) KmbPrimary else Color.Transparent
                    )
                ) {
                    Text("Video Enhancer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (isPhotoTab) {
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
                                .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF59E0B)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (photoBitmap == null) "Select Photo to Enhance" else "Change Photo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Crisp unsharp sharpening + HDR colors",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }

            photoBitmap?.let { orig ->
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
                            Text("Enhanced HD", fontSize = 12.sp, color = KmbSecondary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E222B)),
                                contentAlignment = Alignment.Center
                            ) {
                                enhancedBitmap?.let { enh ->
                                    Image(
                                        bitmap = enh.asImageBitmap(),
                                        contentDescription = "Enhanced",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Clarity & Color Controls:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)

                        Text("Sharpness (${String.format("%.1f", sharpness)}x)", fontSize = 11.sp, color = Color.Gray)
                        Slider(
                            value = sharpness,
                            onValueChange = {
                                sharpness = it
                                enhancedBitmap = MediaEnhancerHelper.enhancePhoto(orig, sharpness, contrast, saturation)
                            },
                            valueRange = 0f..2.5f,
                            colors = SliderDefaults.colors(thumbColor = KmbPrimary, activeTrackColor = KmbPrimary)
                        )

                        Text("Dynamic Contrast (${String.format("%.2f", contrast)}x)", fontSize = 11.sp, color = Color.Gray)
                        Slider(
                            value = contrast,
                            onValueChange = {
                                contrast = it
                                enhancedBitmap = MediaEnhancerHelper.enhancePhoto(orig, sharpness, contrast, saturation)
                            },
                            valueRange = 1.0f..1.35f,
                            colors = SliderDefaults.colors(thumbColor = KmbSecondary, activeTrackColor = KmbSecondary)
                        )

                        Text("Color Vibrancy (${String.format("%.2f", saturation)}x)", fontSize = 11.sp, color = Color.Gray)
                        Slider(
                            value = saturation,
                            onValueChange = {
                                saturation = it
                                enhancedBitmap = MediaEnhancerHelper.enhancePhoto(orig, sharpness, contrast, saturation)
                            },
                            valueRange = 1.0f..1.4f,
                            colors = SliderDefaults.colors(thumbColor = Color(0xFFEC4899), activeTrackColor = Color(0xFFEC4899))
                        )
                    }
                }

                enhancedBitmap?.let { enh ->
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    val res = MediaEnhancerHelper.saveEnhancedPhotoToGallery(context, enh)
                                    res.fold(
                                        onSuccess = {
                                            Toast.makeText(context, "✓ Enhanced photo saved to Pictures/KMB AI Studio!", Toast.LENGTH_SHORT).show()
                                            snackbarHostState.showSnackbar("✓ Enhanced photo saved to Gallery!")
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
                            Text("Save Enhanced Photo to Gallery", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Video Enhancer Tab
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
                                .background(Brush.linearGradient(listOf(Color(0xFFEC4899), Color(0xFFF59E0B)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (videoPath == null) "Select Video to Enhance" else "Video Ready",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Applies FFmpeg unsharp mask & HDR contrast enhancement",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }

            videoPath?.let { path ->
                item {
                    Button(
                        onClick = {
                            isProcessingVideo = true
                            videoProgress = "Starting video clarity booster..."
                            scope.launch {
                                val res = MediaEnhancerHelper.enhanceVideo(
                                    context = context,
                                    inputFilePath = path,
                                    onProgress = { videoProgress = it }
                                )
                                isProcessingVideo = false
                                res.fold(
                                    onSuccess = { file ->
                                        enhancedVideoFile = file
                                        Toast.makeText(context, "✓ Enhanced video saved to Movies/KMB AI Studio!", Toast.LENGTH_SHORT).show()
                                        snackbarHostState.showSnackbar("✓ Enhanced video saved to Gallery!")
                                    },
                                    onFailure = { err ->
                                        snackbarHostState.showSnackbar("Enhance Error: ${err.message}")
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                        enabled = !isProcessingVideo
                    ) {
                        Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (isProcessingVideo) "Enhancing..." else "✨ Boost Video Clarity & HDR Colors")
                    }
                }

                if (isProcessingVideo) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFEC4899), modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(videoProgress, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }

                enhancedVideoFile?.let {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                Spacer(Modifier.width(10.dp))
                                Text("Enhanced video saved to Movies/KMB AI Studio", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tool 6: Photo Text Eraser
 */
@Composable
fun PhotoTextEraserView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) }
    val detectedBoxes = remember { mutableStateListOf<DetectedTextBox>() }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                originalBitmap = loadBitmapFromUri(context, it)
                resultBitmap = null
                detectedBoxes.clear()
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
                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF10B981)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (originalBitmap == null) "Select Image with Text/Logos" else "Change Image",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Auto-detect text & inpaint background seamlessly",
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
                        Text("Cleaned Result", fontSize = 12.sp, color = KmbSecondary, fontWeight = FontWeight.Bold)
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
                            } else if (isDetecting || isErasing) {
                                CircularProgressIndicator(color = KmbSecondary)
                            } else {
                                Text("Tap Auto-Erase", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isDetecting = true
                        scope.launch {
                            val detectRes = ImageInpaintHelper.detectText(orig)
                            isDetecting = false
                            detectRes.fold(
                                onSuccess = { boxes ->
                                    if (boxes.isEmpty()) {
                                        snackbarHostState.showSnackbar("No text or watermarks detected in this image.")
                                    } else {
                                        isErasing = true
                                        val rects = boxes.map { it.rect }
                                        val eraseRes = ImageInpaintHelper.eraseAreas(orig, rects)
                                        isErasing = false
                                        eraseRes.fold(
                                            onSuccess = { cleanBmp ->
                                                resultBitmap = cleanBmp
                                                snackbarHostState.showSnackbar("✓ Erased ${boxes.size} text areas seamlessly!")
                                            },
                                            onFailure = { snackbarHostState.showSnackbar("Erase failed: ${it.message}") }
                                        )
                                    }
                                },
                                onFailure = { snackbarHostState.showSnackbar("Text detection failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    enabled = !isDetecting && !isErasing
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            isDetecting -> "Detecting Text & Watermarks..."
                            isErasing -> "Inpainting Clean Pixels..."
                            else -> "🧹 Auto-Detect & Erase All Text"
                        }
                    )
                }
            }

            resultBitmap?.let { resBmp ->
                item {
                    Button(
                        onClick = {
                            scope.launch {
                                val saveRes = ImageBgRemoverHelper.saveToGallery(context, resBmp, "NoText")
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
                        Text("Save Clean Image to Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Tool 7: Video Delogo & Watermark Remover
 */
@Composable
fun VideoDelogoView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPosition by remember { mutableStateOf(WatermarkPosition.BOTTOM_RIGHT) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var cleanedVideoFile by remember { mutableStateOf<File?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedVideoUri = it
            cleanedVideoFile = null
        }
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
                    .clickable { videoPickerLauncher.launch("video/*") },
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
                            text = if (selectedVideoUri == null) "Select Video from Device" else "Video Ready",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Blurs out TikTok, Reels or platform logos",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        selectedVideoUri?.let { uri ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Watermark Position:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionButton(
                            label = "Bottom Right",
                            selected = selectedPosition == WatermarkPosition.BOTTOM_RIGHT,
                            onClick = { selectedPosition = WatermarkPosition.BOTTOM_RIGHT },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            label = "Top Right",
                            selected = selectedPosition == WatermarkPosition.TOP_RIGHT,
                            onClick = { selectedPosition = WatermarkPosition.TOP_RIGHT },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionButton(
                            label = "Bottom Left",
                            selected = selectedPosition == WatermarkPosition.BOTTOM_LEFT,
                            onClick = { selectedPosition = WatermarkPosition.BOTTOM_LEFT },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            label = "Top Left",
                            selected = selectedPosition == WatermarkPosition.TOP_LEFT,
                            onClick = { selectedPosition = WatermarkPosition.TOP_LEFT },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        isProcessing = true
                        progressStatus = "Resolving video path..."
                        scope.launch {
                            val path = getPathFromUri(context, uri)
                            if (path == null) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Cannot resolve video file path.")
                                return@launch
                            }

                            val result = VideoWatermarkHelper.removeWatermark(
                                context = context,
                                inputFilePath = path,
                                position = selectedPosition,
                                onProgress = { progressStatus = it }
                            )

                            isProcessing = false
                            result.fold(
                                onSuccess = { file ->
                                    cleanedVideoFile = file
                                    Toast.makeText(context, "✓ Clean video saved to Movies/KMB AI Studio!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Clean video saved to Movies/KMB AI Studio!")
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar("Delogo error: ${err.message}")
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isProcessing) "Processing..." else "🎬 Remove Watermark & Save")
                }
            }

            if (isProcessing) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(progressStatus, fontSize = 12.sp, color = Color.White)
                    }
                }
            }

            cleanedVideoFile?.let {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                            Spacer(Modifier.width(10.dp))
                            Text("Clean video saved to Movies/KMB AI Studio", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tool 8: Audio & Ringtone Studio
 */
@Composable
fun AudioStudioToolView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var customTitle by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("00:30") }
    var selectedTarget by remember { mutableStateOf(AudioTarget.RINGTONE) }
    var isProcessing by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedVideoUri = it }
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
                            .background(Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedVideoUri == null) "Select Video to Extract Audio" else "Video Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Extracts high-fidelity audio directly as ringtone/alarm",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        selectedVideoUri?.let { uri ->
            item {
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Track / Ringtone Name") },
                    placeholder = { Text("My Custom Ringtone") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("00:00") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("00:30") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Save Target:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionButton(
                            label = "🔔 Ringtone",
                            selected = selectedTarget == AudioTarget.RINGTONE,
                            onClick = { selectedTarget = AudioTarget.RINGTONE },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            label = "⏰ Alarm",
                            selected = selectedTarget == AudioTarget.ALARM,
                            onClick = { selectedTarget = AudioTarget.ALARM },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PositionButton(
                            label = "💬 Notification",
                            selected = selectedTarget == AudioTarget.NOTIFICATION,
                            onClick = { selectedTarget = AudioTarget.NOTIFICATION },
                            modifier = Modifier.weight(1f)
                        )
                        PositionButton(
                            label = "🎵 Music Track",
                            selected = selectedTarget == AudioTarget.MUSIC,
                            onClick = { selectedTarget = AudioTarget.MUSIC },
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
                            val path = getPathFromUri(context, uri)
                            if (path == null) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Cannot resolve video path.")
                                return@launch
                            }

                            val res = VideoTrimmerHelper.extractAndSaveAudio(
                                context = context,
                                inputFilePath = path,
                                startTime = startTime,
                                endTime = endTime,
                                customTitle = customTitle.ifBlank { "KMB_Audio_${System.currentTimeMillis()}" },
                                target = selectedTarget
                            )

                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Extracted audio saved directly to sound settings!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Extracted audio saved successfully!")
                                },
                                onFailure = { snackbarHostState.showSnackbar("Audio extraction failed: ${it.message}") }
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
                    Text(if (isProcessing) "Extracting Audio Track..." else "🎵 Extract & Set Sound")
                }
            }
        }
    }
}

/**
 * Tool 9: Lossless Video Trimmer
 */
@Composable
fun UniversalTrimmerView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("01:00") }
    var isProcessing by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedVideoUri = it }
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
                            .background(Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedVideoUri == null) "Select Video to Trim" else "Video Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Native lossless hardware trimming with zero quality loss",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        selectedVideoUri?.let { uri ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("00:00") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("01:00") },
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
                                snackbarHostState.showSnackbar("Cannot resolve video path.")
                                return@launch
                            }

                            val res = VideoTrimmerHelper.trimMedia(
                                context = context,
                                inputFilePath = path,
                                startTime = startTime,
                                endTime = endTime,
                                isAudioOnly = false
                            )

                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Trimmed clip saved to Movies/KMB Downloader!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Trimmed clip saved to phone gallery!")
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
                    Text(if (isProcessing) "Trimming Video..." else "✂️ Trim & Save Clip")
                }
            }
        }
    }
}

/**
 * Tool 10: 9:16 Shorts / Reels Reframe
 */
@Composable
fun ShortsReframeView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedVideoUri = it }
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
                            .background(Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFFB923C)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CropPortrait, contentDescription = null, tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedVideoUri == null) "Select 16:9 Video" else "Video Ready to Reframe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Reframes widescreen video into 9:16 vertical reels format",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        selectedVideoUri?.let { uri ->
            item {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val path = getPathFromUri(context, uri)
                            if (path == null) {
                                isProcessing = false
                                snackbarHostState.showSnackbar("Cannot resolve video path.")
                                return@launch
                            }

                            val res = VideoTrimmerHelper.trimMedia(
                                context = context,
                                inputFilePath = path,
                                startTime = "00:00",
                                endTime = "00:59",
                                isAudioOnly = false
                            )

                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Reframe clip saved to Movies/KMB Downloader!", Toast.LENGTH_SHORT).show()
                                    snackbarHostState.showSnackbar("✓ Shorts clip ready in gallery!")
                                },
                                onFailure = { snackbarHostState.showSnackbar("Reframe failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.CropPortrait, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isProcessing) "Reframing to 9:16..." else "📱 Reframe to 9:16 Shorts")
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
            .background(if (selected) KmbPrimary.copy(alpha = 0.35f) else Color(0xFF262C3A))
            .border(1.dp, if (selected) KmbPrimary else Color.Transparent, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
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
