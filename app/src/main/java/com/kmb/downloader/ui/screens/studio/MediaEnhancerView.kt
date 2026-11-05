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
                    .background(com.kmb.downloader.ui.theme.DarkSurfaceCard)
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
                                    .background(com.kmb.downloader.ui.theme.DarkSurfaceCard),
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
                                        },
                                        onFailure = { snackbarHostState.showSnackbar("Save failed: ${it.message}") }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KmbSecondary, contentColor = Color.White)
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
                CenteredCircularMediaPicker(
                    icon = Icons.Default.HighQuality,
                    title = "Select Video to Enhance",
                    subtitle = "Applies FFmpeg unsharp mask & HDR contrast enhancement",
                    gradient = listOf(Color(0xFFEC4899), Color(0xFFF59E0B)),
                    hasMedia = videoPath != null,
                    onClick = { videoPicker.launch("video/*") }
                )
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
                                    },
                                    onFailure = { err ->
                                        snackbarHostState.showSnackbar("Enhance Error: ${err.message}")
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
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
                            colors = CardDefaults.cardColors(containerColor = KmbPrimary.copy(alpha = 0.15f)),
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
 * Tool 7: Photo Text Eraser (Seamless Boundary Inpainting)
 */
