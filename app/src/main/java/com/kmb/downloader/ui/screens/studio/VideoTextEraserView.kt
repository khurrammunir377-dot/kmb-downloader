package com.kmb.downloader.ui.screens.studio

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.core.content.ContextCompat
import com.kmb.downloader.tools.*
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun VideoTextEraserView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoPath by remember { mutableStateOf<String?>(null) }
    var selectedRegion by remember { mutableStateOf(VideoTextRegion.ALL_SCREEN_TEXT) }
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
            CenteredCircularMediaPicker(
                icon = Icons.Default.Subtitles,
                title = "Select Video to Erase Text",
                subtitle = "Blurs/delogos subtitles or channel handles",
                gradient = listOf(Color(0xFFF97316), Color(0xFFEF4444)),
                hasMedia = videoPath != null,
                onClick = { videoPicker.launch("video/*") }
            )
        }

        videoPath?.let { path ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Region to Erase:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    val regions = listOf(
                        Pair(VideoTextRegion.ALL_SCREEN_TEXT, "🌟 All Screen Text (Top, Center & Subtitles)"),
                        Pair(VideoTextRegion.BOTTOM_SUBTITLES, "Bottom Subtitles / Captions"),
                        Pair(VideoTextRegion.TOP_CAPTIONS, "Top Channel Tag / Handle"),
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
                                .background(if (isSelected) KmbPrimary.copy(alpha = 0.35f) else com.kmb.downloader.ui.theme.DarkSurface)
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
                                },
                                onFailure = { err ->
                                    snackbarHostState.showSnackbar("Eraser Error: ${err.message}")
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
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
                        colors = CardDefaults.cardColors(containerColor = KmbPrimary.copy(alpha = 0.15f)),
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
 * Tool 3: Video to Text (Real-time Speech Recognition, Video Audio Transcription & Google Voice Input)
 */
