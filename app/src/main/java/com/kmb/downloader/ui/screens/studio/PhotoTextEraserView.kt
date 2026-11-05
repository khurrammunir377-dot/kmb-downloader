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
fun PhotoTextEraserView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) }

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
            CenteredCircularMediaPicker(
                icon = Icons.Default.CleaningServices,
                title = "Select Image with Text/Logos",
                subtitle = "Detects text & inpaint-reconstructs background pixels seamlessly without covering",
                gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
                hasMedia = originalBitmap != null,
                onClick = { photoPickerLauncher.launch("image/*") }
            )
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
                                .background(com.kmb.downloader.ui.theme.DarkSurfaceCard),
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
                                Text("Tap Erase Text", fontSize = 11.sp, color = Color.Gray)
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
                                        Toast.makeText(context, "No text detected in this image.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isErasing = true
                                        val rects = boxes.map { it.rect }
                                        val eraseRes = ImageInpaintHelper.eraseAreas(orig, rects)
                                        isErasing = false
                                        eraseRes.fold(
                                            onSuccess = { cleanBmp ->
                                                resultBitmap = cleanBmp
                                                Toast.makeText(context, "✓ Erased ${boxes.size} text areas seamlessly!", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, "Erase failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                onFailure = {
                                    Toast.makeText(context, "Detection failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
                    enabled = !isDetecting && !isErasing
                ) {
                    Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            isDetecting -> "Scanning all text scripts..."
                            isErasing -> "Inpainting background pixels..."
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
                                    },
                                    onFailure = { Toast.makeText(context, "Save failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KmbSecondary, contentColor = Color.White)
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
 * Tool 5: Video Delogo (Auto Watermark Remover)
 */
