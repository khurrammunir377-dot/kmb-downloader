package com.kmb.downloader.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.data.ReferralManager
import com.kmb.downloader.ui.components.ReferralDialog
import com.kmb.downloader.ui.screens.studio.*
import com.kmb.downloader.ui.theme.KmbPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val proState by ReferralManager.proState.collectAsState()
    var showReferralDialog by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf<StudioTool?>(null) }

    // Intercept hardware back button when inside a sub-tool so user returns to AI Studio menu
    BackHandler(enabled = activeTool != null) {
        activeTool = null
    }

    val studioItems = listOf(
        StudioItem(
            tool = StudioTool.BG_REMOVER,
            title = "Photo BG Remover",
            tag = "On-Device AI",
            desc = "HD subject cutout with transparent PNG & natural portrait blur",
            icon = Icons.Default.AutoFixHigh,
            gradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
        ),
        StudioItem(
            tool = StudioTool.PHOTO_OCR,
            title = "Copy Text from Photo",
            tag = "All Languages",
            desc = "Extract any script (English, Urdu, Hindi, Chinese, etc.) + 1-tap translation",
            icon = Icons.Default.TextFields,
            gradient = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4))
        ),
        StudioItem(
            tool = StudioTool.VIDEO_TO_TEXT,
            title = "Video to Text",
            tag = "Speech AI",
            desc = "Convert speech from video/audio into text with live multi-language transcription",
            icon = Icons.Default.Translate,
            gradient = listOf(Color(0xFF10B981), Color(0xFF3B82F6))
        ),
        StudioItem(
            tool = StudioTool.VIDEO_CLIPPER,
            title = "Video Clipper",
            tag = "Smart Clipper",
            desc = "Lossless multi-segment clipping for 15s Story, 30s Status & 60s Reels",
            icon = Icons.Default.ContentCut,
            gradient = listOf(Color(0xFF6366F1), Color(0xFF3B82F6))
        ),
        StudioItem(
            tool = StudioTool.VIDEO_DELOGO,
            title = "Video Delogo",
            tag = "Auto Watermark",
            desc = "Auto-detects and seamlessly removes TikTok & Reels watermarks without blur",
            icon = Icons.Default.Movie,
            gradient = listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
        ),
        StudioItem(
            tool = StudioTool.VIDEO_TEXT_REMOVER,
            title = "Video Text Eraser",
            tag = "Video Inpaint",
            desc = "Cleanly inpaint subtitles, captions, or channel tags from video frames",
            icon = Icons.Default.Subtitles,
            gradient = listOf(Color(0xFFF97316), Color(0xFFEF4444))
        ),
        StudioItem(
            tool = StudioTool.TEXT_ERASER,
            title = "Photo Text Eraser",
            tag = "Smart Inpaint",
            desc = "Detects text & seamlessly reconstructs background pixels without covering",
            icon = Icons.Default.CleaningServices,
            gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
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
                                    onClick = {
                                        if (!proState.isPro) {
                                            showReferralDialog = true
                                        } else {
                                            activeTool = item.tool
                                        }
                                    }
                                )
                            }

                            item { Spacer(Modifier.height(8.dp)) }
                        }
                    }
                    StudioTool.BG_REMOVER -> PhotoBgRemoverView(snackbarHostState)
                    StudioTool.PHOTO_OCR -> PhotoOcrView(snackbarHostState)
                    StudioTool.VIDEO_TO_TEXT -> VideoToTextView(snackbarHostState)
                    StudioTool.VIDEO_CLIPPER -> VideoClipperView(snackbarHostState)
                    StudioTool.VIDEO_DELOGO -> VideoDelogoView(snackbarHostState)
                    StudioTool.VIDEO_TEXT_REMOVER -> VideoTextEraserView(snackbarHostState)
                    StudioTool.MEDIA_ENHANCER -> MediaEnhancerView(snackbarHostState)
                    StudioTool.TEXT_ERASER -> PhotoTextEraserView(snackbarHostState)
                    StudioTool.AUDIO_STUDIO -> AudioStudioToolView(snackbarHostState)
                    StudioTool.MEDIA_TRIMMER -> UniversalTrimmerView(snackbarHostState)
                    StudioTool.SHORTS_REFRAME -> ShortsReframeView(snackbarHostState)
                }
            }
        }
    }

    if (showReferralDialog) {
        ReferralDialog(onDismiss = { showReferralDialog = false }, onShowMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } })
    }
}

