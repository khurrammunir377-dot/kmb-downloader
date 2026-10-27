package com.kmb.downloader.whatsapp

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.kmb.downloader.data.WhatsAppStatusItem
import java.io.File

object WhatsAppStatusHelper {

    private const val TAG = "WhatsAppStatusHelper"
    private const val PREFS_NAME = "kmb_whatsapp_prefs"
    private const val KEY_WA_URI = "wa_tree_uri"
    private const val KEY_WB_URI = "wb_tree_uri"

    const val PACKAGE_WHATSAPP = "com.whatsapp"
    const val PACKAGE_WHATSAPP_BUSINESS = "com.whatsapp.w4b"

    fun isAppInstalled(context: Context, isBusiness: Boolean): Boolean {
        val packageName = if (isBusiness) PACKAGE_WHATSAPP_BUSINESS else PACKAGE_WHATSAPP
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(packageName, 0)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSavedFolderUri(context: Context, isBusiness: Boolean): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (isBusiness) KEY_WB_URI else KEY_WA_URI
        val uriStr = prefs.getString(key, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (e: Exception) {
            null
        }
    }

    fun saveFolderUri(context: Context, uri: Uri, isBusiness: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (isBusiness) KEY_WB_URI else KEY_WA_URI
        prefs.edit().putString(key, uri.toString()).apply()
    }

    fun hasFolderPermission(context: Context, isBusiness: Boolean): Boolean {
        // First check direct file access (works on Android 10 and below or certain devices)
        if (getDirectStatusesDir(isBusiness)?.exists() == true && getDirectStatusesDir(isBusiness)?.canRead() == true) {
            return true
        }

        val uri = getSavedFolderUri(context, isBusiness) ?: return false
        return try {
            val hasPerm = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
            if (hasPerm) {
                val doc = DocumentFile.fromTreeUri(context, uri)
                doc != null && doc.canRead()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getDirectStatusesDir(isBusiness: Boolean): File? {
        val base = Environment.getExternalStorageDirectory()
        val candidatePaths = if (isBusiness) {
            listOf(
                File(base, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                File(base, "WhatsApp Business/Media/.Statuses")
            )
        } else {
            listOf(
                File(base, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(base, "WhatsApp/Media/.Statuses")
            )
        }
        return candidatePaths.firstOrNull { it.exists() && it.canRead() }
    }

    fun getInitialUri(isBusiness: Boolean): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val path = if (isBusiness) {
                "primary:Android%2Fmedia%2Fcom.whatsapp.w4b%2FWhatsApp%20Business%2FMedia%2F.Statuses"
            } else {
                "primary:Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
            }
            return Uri.parse("content://com.android.externalstorage.documents/document/$path")
        }
        return null
    }

    fun createFolderIntent(isBusiness: Boolean): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            val uri = getInitialUri(isBusiness)
            if (uri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
            }
        }
    }

    fun fetchStatuses(context: Context, isBusiness: Boolean): List<WhatsAppStatusItem> {
        val result = mutableListOf<WhatsAppStatusItem>()

        // 1. Try direct file access first
        val directDir = getDirectStatusesDir(isBusiness)
        if (directDir != null && directDir.exists() && directDir.canRead()) {
            val files = directDir.listFiles()
            if (!files.isNullOrEmpty()) {
                for (f in files) {
                    if (isValidStatusFile(f.name)) {
                        val isVid = f.name.endsWith(".mp4", ignoreCase = true)
                        result.add(
                            WhatsAppStatusItem(
                                uri = f.toUri(),
                                name = f.name,
                                isVideo = isVid,
                                size = f.length(),
                                lastModified = f.lastModified()
                            )
                        )
                    }
                }
                if (result.isNotEmpty()) {
                    return result.sortedByDescending { it.lastModified }
                }
            }
        }

        // 2. Try SAF DocumentFile access
        val treeUri = getSavedFolderUri(context, isBusiness) ?: return emptyList()
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
            // If the user selected Media folder, look for .Statuses inside it
            val targetFolder = if (rootDoc.name.equals(".Statuses", ignoreCase = true)) {
                rootDoc
            } else {
                rootDoc.findFile(".Statuses") ?: rootDoc
            }

            val docFiles = targetFolder.listFiles()
            for (doc in docFiles) {
                val name = doc.name ?: continue
                if (isValidStatusFile(name)) {
                    val isVid = name.endsWith(".mp4", ignoreCase = true)
                    result.add(
                        WhatsAppStatusItem(
                            uri = doc.uri,
                            name = name,
                            isVideo = isVid,
                            size = doc.length(),
                            lastModified = doc.lastModified()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing SAF statuses: ${e.message}", e)
        }

        return result.sortedByDescending { it.lastModified }
    }

    private fun isValidStatusFile(name: String): Boolean {
        if (name.equals(".nomedia", ignoreCase = true)) return false
        val lower = name.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".mp4")
    }

    fun saveStatusToGallery(context: Context, item: WhatsAppStatusItem): Boolean {
        val resolver = context.contentResolver
        val isVideo = item.isVideo
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val extension = if (isVideo) ".mp4" else ".jpg"
        val filename = "KMB_Status_${System.currentTimeMillis()}$extension"
        val relativePath = if (isVideo) "Movies/KMB Downloader" else "Pictures/KMB Downloader"

        val collection = if (isVideo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(collection, values) ?: return false

        return try {
            resolver.openInputStream(item.uri)?.use { input ->
                resolver.openOutputStream(itemUri)?.use { output ->
                    input.copyTo(output)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            MediaScannerConnection.scanFile(
                context,
                arrayOf(itemUri.toString()),
                arrayOf(mimeType),
                null
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save status to gallery", e)
            try { resolver.delete(itemUri, null, null) } catch (_: Exception) {}
            false
        }
    }

    fun shareStatus(context: Context, item: WhatsAppStatusItem) {
        try {
            val mimeType = if (item.isVideo) "video/mp4" else "image/jpeg"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Status via")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share status", e)
        }
    }
}
