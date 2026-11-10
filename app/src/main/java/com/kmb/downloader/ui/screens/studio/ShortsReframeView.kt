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
            CenteredCircularMediaPicker(
                icon = Icons.Default.CropPortrait,
                title = "Select 16:9 Video",
                subtitle = "Reframes widescreen video into 9:16 vertical reels format",
                gradient = listOf(Color(0xFFF43F5E), Color(0xFFFB923C)),
                hasMedia = selectedVideoUri != null,
                onClick = { videoPicker.launch("video/*") }
            )
        }

        selectedVideoUri?.let { uri ->
            item {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val res = VideoTrimmerHelper.trimMedia(
                                context = context,
                                inputFilePath = uri.toString(),
                                startTime = "00:00",
                                endTime = "00:59",
                                isAudioOnly = false
                            )

                            isProcessing = false
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, "✓ Reframe clip saved to Movies/KMB Downloader!", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { snackbarHostState.showSnackbar("Reframe failed: ${it.message}") }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
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
fun PositionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) KmbPrimary.copy(alpha = 0.35f) else com.kmb.downloader.ui.theme.DarkSurface)
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
