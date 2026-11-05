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
fun PhotoOcrView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var extractedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var selectedScript by remember { mutableStateOf(PhotoOcrHelper.SCRIPT_OPTIONS[0]) }
    var showScriptMenu by remember { mutableStateOf(false) }
    var detectedLangCode by remember { mutableStateOf("en") }
    var selectedTargetLang by remember { mutableStateOf(PhotoOcrHelper.TRANSLATION_LANGUAGES[0]) }
    var showLangMenu by remember { mutableStateOf(false) }

    fun runScan(bmp: Bitmap, scriptId: String) {
        scope.launch {
            isScanning = true
            val ocrRes = PhotoOcrHelper.recognizeText(context, bmp, scriptId)
            isScanning = false
            ocrRes.fold(
                onSuccess = { res ->
                    extractedText = res.fullText
                    detectedLangCode = res.detectedLanguage
                    if (res.fullText.isBlank()) {
                        Toast.makeText(context, "No text detected with " + selectedScript.displayName + ". Try another script filter.", Toast.LENGTH_SHORT).show()
                    }
                },
                onFailure = { err ->
                    Toast.makeText(context, "OCR: " + err.message, Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                photoBitmap = loadBitmapFromUri(context, it)
                extractedText = ""
                translatedText = ""
                photoBitmap?.let { bmp ->
                    runScan(bmp, selectedScript.id)
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
            CenteredCircularMediaPicker(
                icon = Icons.Default.TextFields,
                title = "Select Photo with Text",
                subtitle = "Extracts all scripts (English, Urdu, Hindi, Chinese, etc.) + Instant Translate",
                gradient = listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
                hasMedia = photoBitmap != null,
                onClick = { photoPicker.launch("image/*") }
            )
        }

        // Script filter selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = com.kmb.downloader.ui.theme.DarkSurfaceCard),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "OCR SCRIPT FILTER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KmbPrimary
                        )
                        Text(
                            selectedScript.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showScriptMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Language Script: " + selectedScript.displayName, color = Color.White)
                        }

                        DropdownMenu(
                            expanded = showScriptMenu,
                            onDismissRequest = { showScriptMenu = false }
                        ) {
                            PhotoOcrHelper.SCRIPT_OPTIONS.forEach { opt ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(opt.displayName, fontWeight = FontWeight.Bold)
                                            Text(opt.description, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        selectedScript = opt
                                        showScriptMenu = false
                                        photoBitmap?.let { bmp -> runScan(bmp, opt.id) }
                                    }
                                )
                            }
                        }
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
                        Text("Extracting " + selectedScript.displayName + " via Google ML Kit...", color = Color.White, fontSize = 13.sp)
                    }
                }
            }

            if (extractedText.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = com.kmb.downloader.ui.theme.DarkSurfaceCard),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "EXTRACTED TEXT (" + detectedLangCode.uppercase() + ")",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KmbSecondary
                                )
                                Text(
                                    "" + extractedText.length + " characters",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = extractedText,
                                onValueChange = { extractedText = it },
                                modifier = Modifier.fillMaxWidth().height(160.dp),
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
                            colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16))
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
                            colors = ButtonDefaults.buttonColors(containerColor = KmbSecondary, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share Text", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Translation Section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = com.kmb.downloader.ui.theme.DarkSurfaceCard),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "TRANSLATE TEXT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showLangMenu = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Translate to: " + selectedTargetLang.displayName, color = Color.White)
                                }

                                DropdownMenu(
                                    expanded = showLangMenu,
                                    onDismissRequest = { showLangMenu = false }
                                ) {
                                    PhotoOcrHelper.TRANSLATION_LANGUAGES.forEach { lang ->
                                        DropdownMenuItem(
                                            text = { Text(lang.displayName) },
                                            onClick = {
                                                selectedTargetLang = lang
                                                showLangMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    isTranslating = true
                                    scope.launch {
                                        val res = PhotoOcrHelper.translateText(extractedText, selectedTargetLang.code)
                                        isTranslating = false
                                        res.fold(
                                            onSuccess = {
                                                translatedText = it
                                                Toast.makeText(context, "Translation complete ✓", Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = {
                                                Toast.makeText(context, "Translation error: " + it.message, Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16)),
                                enabled = !isTranslating
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (isTranslating) "Translating..." else "🌐 Translate to " + selectedTargetLang.displayName)
                            }

                            if (translatedText.isNotBlank()) {
                                OutlinedTextField(
                                    value = translatedText,
                                    onValueChange = { translatedText = it },
                                    modifier = Modifier.fillMaxWidth().height(140.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF374151)
                                    )
                                )

                                Button(
                                    onClick = {
                                        val clip = ClipData.newPlainText("Translated Text", translatedText)
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(clip)
                                        Toast.makeText(context, "Translation copied! 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = KmbSecondary, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Copy Translation")
                                }
                            }
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
