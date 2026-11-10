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

enum class StudioTool {
    BG_REMOVER,
    PHOTO_OCR,
    VIDEO_TO_TEXT,
    VIDEO_CLIPPER,
    VIDEO_DELOGO,
    VIDEO_TEXT_REMOVER,
    MEDIA_ENHANCER,
    TEXT_ERASER,
    AUDIO_STUDIO,
    MEDIA_TRIMMER,
    SHORTS_REFRAME
}

data class StudioItem(
    val tool: StudioTool,
    val title: String,
    val tag: String,
    val desc: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

suspend fun loadBitmapFromUri(context: Context, uri: Uri, maxDim: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
    try {
        var rotationDegrees = 0f
        try {
            context.contentResolver.openInputStream(uri)?.use { exifStream ->
                val exif = android.media.ExifInterface(exifStream)
                rotationDegrees = when (
                    exif.getAttributeInt(
                        android.media.ExifInterface.TAG_ORIENTATION,
                        android.media.ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        } catch (_: Throwable) {}

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { boundsStream ->
            BitmapFactory.decodeStream(boundsStream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return@withContext null

        var inSampleSize = 1
        while (origW / inSampleSize > maxDim || origH / inSampleSize > maxDim) {
            inSampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            this.inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        } ?: return@withContext null

        if (rotationDegrees != 0f) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) decoded.recycle()
            rotated
        } else {
            decoded
        }
    } catch (_: Throwable) {
        null
    }
}

fun getPathFromUri(context: Context, uri: Uri): String? {
    if (uri.scheme == "file") return uri.path
    val proj = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
    try {
        context.contentResolver.query(uri, proj, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                if (idx != -1) {
                    val path = cursor.getString(idx)
                    if (!path.isNullOrBlank()) {
                        val f = File(path)
                        if (f.exists() && f.canRead() && f.length() > 0L) return path
                    }
                }
            }
        }
    } catch (_: Throwable) {}

    return try {
        val temp = File(context.cacheDir, "temp_media_${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(temp).use { output ->
                input.copyTo(output)
            }
        }
        temp.absolutePath
    } catch (_: Throwable) {
        null
    }
}
