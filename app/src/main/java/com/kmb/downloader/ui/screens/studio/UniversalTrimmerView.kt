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
                            val res = VideoTrimmerHelper.trimMedia(
                                context = context,
                                inputFilePath = uri.toString(),
                                startTime = startTime,
                                endTime = endTime,
                                isAudioOnly = false
                            )

                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Trimmed clip saved to Movies/KMB Downloader!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { snackbarHostState.showSnackbar("Trim failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbSecondary, contentColor = Color.White),
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



/**
 * Tool 4: Video Clipper (Multi-Segment Lossless Clipper)
 */
@Composable
fun VideoClipperView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("00:30") }
    var isProcessing by remember { mutableStateOf(false) }
    var clippedFile by remember { mutableStateOf<File?>(null) }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedVideoUri = it
            clippedFile = null
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
                icon = Icons.Default.ContentCut,
                title = "Select Video to Clip",
                subtitle = "Lossless multi-segment clip creator for WhatsApp, Reels & Stories",
                gradient = listOf(Color(0xFF6366F1), Color(0xFF3B82F6)),
                hasMedia = selectedVideoUri != null,
                onClick = { videoPicker.launch("video/*") }
            )
        }

        selectedVideoUri?.let { uri ->
            item {
                Text("Quick Clip Presets:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { startTime = "00:00"; endTime = "00:15" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.kmb.downloader.ui.theme.DarkSurface)
                    ) {
                        Text("15s Story", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = { startTime = "00:00"; endTime = "00:30" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary.copy(alpha = 0.5f))
                    ) {
                        Text("30s Status", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = { startTime = "00:00"; endTime = "01:00" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = com.kmb.downloader.ui.theme.DarkSurface)
                    ) {
                        Text("60s Reel", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (mm:ss)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (mm:ss)") },
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
                            val res = VideoTrimmerHelper.trimMedia(
                                context = context,
                                inputFilePath = uri.toString(),
                                startTime = startTime,
                                endTime = endTime
                            )
                            isProcessing = false
                            res.fold(
                                onSuccess = { file ->
                                    clippedFile = file
                                    Toast.makeText(context, "✓ Video clip saved to Movies/KMB AI Studio!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, "Clipper error: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isProcessing) "Clipping..." else "✂️ Export Lossless Video Clip")
                }
            }

            clippedFile?.let {
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
                            Text("Clip saved to Movies/KMB AI Studio", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
