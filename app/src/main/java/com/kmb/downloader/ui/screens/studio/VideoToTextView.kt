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
fun VideoToTextView(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedLang by remember { mutableStateOf(SpeechTranscriberHelper.AVAILABLE_LANGUAGES[0]) }
    var transcriptText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranscribing by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }
    var showTargetLangMenu by remember { mutableStateOf(false) }
    var selectedTargetLang by remember { mutableStateOf(PhotoOcrHelper.TRANSLATION_LANGUAGES[0]) }
    var isMicMode by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission required for speech recognition", Toast.LENGTH_LONG).show()
        }
    }

    // Google Voice Input Dialog Launcher (100% Reliable Android Voice Recognizer)
    val speechDialogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotBlank()) {
                transcriptText = if (transcriptText.isBlank()) text else transcriptText + "\n" + text
                Toast.makeText(context, "Speech transcribed successfully! ✓", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchGoogleVoiceDialog() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        try {
            val intent = SpeechTranscriberHelper.createRecognizerIntent(
                selectedLang.code,
                prompt = "Listening in " + selectedLang.displayName + "..."
            )
            speechDialogLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Voice input dialog: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    fun startListening() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!SpeechTranscriberHelper.isRecognitionAvailable(context)) {
            launchGoogleVoiceDialog()
            return
        }

        isTranscribing = true
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = SpeechTranscriberHelper.createRecognizerIntent(selectedLang.code)
                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isTranscribing = false
                    }
                    override fun onError(error: Int) {
                        isTranscribing = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Speak closer or use Google Voice Input."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            else -> "Recognition stopped (code " + error + "). Tap Google Voice Input below."
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                    override fun onResults(results: Bundle?) {
                        isTranscribing = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            transcriptText = if (transcriptText.isBlank()) text else transcriptText + "\n" + text
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { transcriptText = it }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                recognizer.startListening(intent)
            } catch (e: Exception) {
                isTranscribing = false
                launchGoogleVoiceDialog()
            }
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            transcriptText = ""
            translatedText = ""
            try {
                player?.stop()
                player?.release()
                player = null
                isPlayingAudio = false
            } catch (_: Exception) {}
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
                icon = Icons.Default.Translate,
                title = if (isMicMode) "Live Microphone Mode" else "Select Video or Audio",
                subtitle = if (isMicMode) "Transcribe voice live via Google Speech" else "Transcribes speech in English, Urdu, Hindi, Arabic, Chinese & more",
                gradient = listOf(Color(0xFF10B981), Color(0xFF3B82F6)),
                hasMedia = selectedMediaUri != null || isMicMode,
                onClick = {
                    if (isMicMode) isMicMode = false
                    mediaPicker.launch("video/*")
                }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { isMicMode = false },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isMicMode) KmbPrimary else com.kmb.downloader.ui.theme.DarkSurface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🎬 Video File", color = if (!isMicMode) Color(0xFF030A16) else Color.White, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { isMicMode = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMicMode) KmbPrimary else com.kmb.downloader.ui.theme.DarkSurface
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🎙️ Live Speech", color = if (isMicMode) Color(0xFF030A16) else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Language Selector Dropdown
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showLangMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Speech Language: " + selectedLang.displayName, color = Color.White)
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

        // If video file is chosen, show playback control
        if (!isMicMode && selectedMediaUri != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = com.kmb.downloader.ui.theme.DarkSurfaceCard),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Loaded Video Audio", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Play audio while transcribing speech", fontSize = 11.sp, color = Color.Gray)
                        }

                        Button(
                            onClick = {
                                if (isPlayingAudio) {
                                    player?.pause()
                                    isPlayingAudio = false
                                } else {
                                    try {
                                        if (player == null) {
                                            player = MediaPlayer().apply {
                                                setDataSource(context, selectedMediaUri!!)
                                                prepare()
                                                setOnCompletionListener { isPlayingAudio = false }
                                            }
                                        }
                                        player?.start()
                                        isPlayingAudio = true
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot play audio: " + e.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPlayingAudio) Color(0xFFEF4444) else KmbSecondary
                            )
                        ) {
                            Text(if (isPlayingAudio) "⏸ Pause Audio" else "▶ Play Audio")
                        }
                    }
                }
            }
        }

        // Two reliable transcription action buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary: Google Voice Input Dialog (Works 100% on all Android versions)
                Button(
                    onClick = { launchGoogleVoiceDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16))
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("🎙️ Google Voice Transcribe (" + selectedLang.displayName + ")", fontWeight = FontWeight.Bold)
                }

                // Secondary: Live In-App Listener
                OutlinedButton(
                    onClick = { startListening() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isTranscribing
                ) {
                    Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTranscribing) "Listening & Transcribing..." else "Live Speech Listener", color = Color.White)
                }
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
                    Text("Listening in " + selectedLang.displayName + "...", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        if (transcriptText.isNotBlank()) {
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
                                "TRANSCRIBED TEXT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                "" + transcriptText.length + " characters",
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
                        colors = ButtonDefaults.buttonColors(containerColor = KmbPrimary, contentColor = Color(0xFF030A16))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Text", fontWeight = FontWeight.Bold)
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
                        colors = ButtonDefaults.buttonColors(containerColor = KmbSecondary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Translation for Speech Transcript
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = com.kmb.downloader.ui.theme.DarkSurfaceCard),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "TRANSLATE TRANSCRIPT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { showTargetLangMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Translate to: " + selectedTargetLang.displayName, color = Color.White)
                            }

                            DropdownMenu(
                                expanded = showTargetLangMenu,
                                onDismissRequest = { showTargetLangMenu = false }
                            ) {
                                PhotoOcrHelper.TRANSLATION_LANGUAGES.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.displayName) },
                                        onClick = {
                                            selectedTargetLang = lang
                                            showTargetLangMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                isTranslating = true
                                scope.launch {
                                    val res = PhotoOcrHelper.translateText(transcriptText, selectedTargetLang.code)
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
                                    val clip = ClipData.newPlainText("Translated Transcript", translatedText)
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
/**
 * Tool 5: Photo & Video Quality Enhancer (Sharpness, Contrast & Vibrant HDR)
 */
