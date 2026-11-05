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
            CenteredCircularMediaPicker(
                icon = Icons.Default.PhotoCamera,
                title = "Select Image from Gallery",
                subtitle = "AI isolates subject & creates HD cutout",
                gradient = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
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
                        Text("Result", fontSize = 12.sp, color = KmbSecondary, fontWeight = FontWeight.Bold)
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
                                    .background(if (isSelected) KmbPrimary.copy(alpha = 0.3f) else com.kmb.downloader.ui.theme.DarkSurface)
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
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
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
                        Text("Save to Phone Gallery", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Tool 2: Copy Text from Photo & Multi-Language Translation
 */
