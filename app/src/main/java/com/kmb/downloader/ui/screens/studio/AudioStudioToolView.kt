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
            CenteredCircularMediaPicker(
                icon = Icons.Default.MusicNote,
                title = "Select Video to Extract Audio",
                subtitle = "Extracts high-fidelity audio directly as ringtone/alarm",
                gradient = listOf(Color(0xFF7C3AED), Color(0xFF6366F1)),
                hasMedia = selectedVideoUri != null,
                onClick = { videoPicker.launch("video/*") }
            )
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
                                },
                                onFailure = { snackbarHostState.showSnackbar("Audio extraction failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
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
