package com.kmb.downloader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.data.DownloadItem
import com.kmb.downloader.tools.AudioTarget
import com.kmb.downloader.tools.VideoTrimmerHelper
import kotlinx.coroutines.launch

@Composable
fun AudioStudioDialog(
    item: DownloadItem,
    onDismiss: () -> Unit,
    onCompleted: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var startTime by remember { mutableStateOf("00:00") }
    var endTime by remember { mutableStateOf("00:30") }
    var customTitle by remember { mutableStateOf(item.title ?: "Custom_Audio") }
    var selectedTarget by remember { mutableStateOf(AudioTarget.RINGTONE) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF59E0B))
    )

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        confirmButton = {
            Button(
                onClick = {
                    val path = item.filePath
                    if (path.isNullOrBlank()) {
                        errorMessage = "File not found on device"
                        return@Button
                    }
                    isProcessing = true
                    errorMessage = null

                    scope.launch {
                        val result = VideoTrimmerHelper.extractAndSaveAudio(
                            context = context,
                            inputFilePath = path,
                            startTime = startTime,
                            endTime = endTime,
                            customTitle = customTitle,
                            target = selectedTarget
                        )
                        isProcessing = false
                        result.fold(
                            onSuccess = {
                                val targetName = when (selectedTarget) {
                                    AudioTarget.RINGTONE -> "Phone Ringtone (in Ringtones/)"
                                    AudioTarget.NOTIFICATION -> "Notification Sound (in Notifications/)"
                                    AudioTarget.ALARM -> "Alarm Tone (in Alarms/)"
                                    AudioTarget.MUSIC -> "Music Track (in Music/)"
                                }
                                onCompleted("✓ Saved to $targetName! Available in phone sound settings.")
                                onDismiss()
                            },
                            onFailure = { err ->
                                errorMessage = err.message ?: "Extraction failed"
                            }
                        )
                    }
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extracting...", color = Color.White)
                } else {
                    Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Sound", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isProcessing) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF9CA3AF))
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🎵", fontSize = 24.sp)
                Column {
                    Text(
                        text = "Audio Studio & Ringtone",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Extract chorus & set as Ringtone/Alarm",
                        fontSize = 11.sp,
                        color = Color(0xFFA78BFA)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Source file info
                Text(
                    text = "Source: ${item.title ?: "Unknown"}",
                    fontSize = 12.sp,
                    color = Color(0xFFD1D5DB),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time", fontSize = 11.sp) },
                        placeholder = { Text("00:00", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time", fontSize = 11.sp) },
                        placeholder = { Text("00:30", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Audio Title
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    label = { Text("Sound Name / Title", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // Target Selector
                Text(
                    text = "Export Destination:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetChip(
                        icon = Icons.Default.RingVolume,
                        label = "Ringtone",
                        selected = selectedTarget == AudioTarget.RINGTONE,
                        onClick = { selectedTarget = AudioTarget.RINGTONE },
                        modifier = Modifier.weight(1f)
                    )
                    TargetChip(
                        icon = Icons.Default.Notifications,
                        label = "Notice",
                        selected = selectedTarget == AudioTarget.NOTIFICATION,
                        onClick = { selectedTarget = AudioTarget.NOTIFICATION },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetChip(
                        icon = Icons.Default.Alarm,
                        label = "Alarm",
                        selected = selectedTarget == AudioTarget.ALARM,
                        onClick = { selectedTarget = AudioTarget.ALARM },
                        modifier = Modifier.weight(1f)
                    )
                    TargetChip(
                        icon = Icons.Default.MusicNote,
                        label = "Music Track",
                        selected = selectedTarget == AudioTarget.MUSIC,
                        onClick = { selectedTarget = AudioTarget.MUSIC },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }
            }
        },
        containerColor = Color(0xFF1B1F2A)
    )
}

@Composable
private fun TargetChip(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF8B5CF6).copy(alpha = 0.25f) else Color(0xFF131722))
            .border(
                1.dp,
                if (selected) Color(0xFF8B5CF6) else Color(0xFF374151),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) Color(0xFFA78BFA) else Color(0xFF9CA3AF)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else Color(0xFF9CA3AF)
            )
        }
    }
}
