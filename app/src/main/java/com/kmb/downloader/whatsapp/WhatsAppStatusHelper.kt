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
        Log.i(TAG, "Saved SAF folder URI for business=$isBusiness: $uri")
    }

    fun clearSavedFolderUri(context: Context, isBusiness: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (isBusiness) KEY_WB_URI else KEY_WA_URI
        val cacheKey = if (isBusiness) "cached_wb_doc_id" else "cached_wa_doc_id"
        prefs.edit().remove(key).remove(cacheKey).apply()
        Log.i(TAG, "Cleared saved SAF folder URI for business=$isBusiness")
    }

    /**
     * Checks if we have valid permission to read statuses.
     * Note: We only consider direct file access valid if listFiles() actually returns non-null,
     * preventing Android 11+ Scoped Storage false positives.
     */
    fun hasFolderPermission(context: Context, isBusiness: Boolean): Boolean {
        // 1. Direct file check: Only true if direct listing actually succeeds
        val directDir = getDirectStatusesDir(isBusiness)
        if (directDir != null) {
            try {
                val list = directDir.listFiles()
                if (list != null && list.isNotEmpty()) {
                    return true
                }
            } catch (_: Exception) {}
        }

        // 2. SAF persisted permission check
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
                File(base, "WhatsApp Business/Media/.Statuses"),
                File(base, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses")
            )
        } else {
            listOf(
                File(base, "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                File(base, "WhatsApp/Media/.Statuses"),
                File(base, "Android/media/com.whatsapp/WhatsApp/Media/Statuses")
            )
        }
        return candidatePaths.firstOrNull { it.exists() && it.canRead() }
    }

    /**
     * Constructs the optimal initial URI to point the Android folder picker directly
     * into com.whatsapp or com.whatsapp.w4b in Android/media.
     * The user just has to tap "USE THIS FOLDER" and "ALLOW".
     */
    fun getInitialUri(isBusiness: Boolean): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pkg = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp"
            // Target the package folder inside Android/media
            val path = "primary:Android%2Fmedia%2F$pkg"
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

        // 1. First, check direct file access (works on Android <= 10 or with all-files access)
        try {
            val directCandidates = if (isBusiness) {
                listOf(
                    File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"),
                    File(Environment.getExternalStorageDirectory(), "WhatsApp Business/Media/.Statuses"),
                    File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses")
                )
            } else {
                listOf(
                    File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
                    File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"),
                    File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/Statuses")
                )
            }

            for (dir in directCandidates) {
                if (dir.exists() && dir.canRead()) {
                    val files = dir.listFiles()
                    if (!files.isNullOrEmpty()) {
                        Log.d(TAG, "Found ${files.size} raw files via direct access in: ${dir.path}")
                        for (f in files) {
                            if (isValidStatusFile(f.name)) {
                                result.add(
                                    WhatsAppStatusItem(
                                        uri = f.toUri(),
                                        name = f.name,
                                        isVideo = f.name.endsWith(".mp4", ignoreCase = true),
                                        size = f.length(),
                                        lastModified = f.lastModified()
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct file scan failed: ${e.message}")
        }

        // 2. Next, scan via SAF Tree URI
        val treeUri = getSavedFolderUri(context, isBusiness)
        if (treeUri != null) {
            try {
                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                Log.d(TAG, "Scanning via SAF treeUri: $treeUri, docId: $treeDocId")

                // Generate candidate document IDs for .Statuses based on what folder user picked
                val candidateDocIds = mutableListOf<String>()

                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val cacheKey = if (isBusiness) "cached_wb_doc_id" else "cached_wa_doc_id"
                val cachedDocId = prefs.getString(cacheKey, null)
                if (!cachedDocId.isNullOrBlank()) {
                    candidateDocIds.add(cachedDocId)
                }

                if (treeDocId.endsWith("/.Statuses") || treeDocId.endsWith("/Statuses")) {
                    if (!candidateDocIds.contains(treeDocId)) candidateDocIds.add(treeDocId)
                } else if (treeDocId.endsWith("/Media")) {
                    candidateDocIds.add("$treeDocId/.Statuses")
                    candidateDocIds.add("$treeDocId/Statuses")
                } else if (treeDocId.endsWith("/WhatsApp") || treeDocId.endsWith("/WhatsApp Business")) {
                    candidateDocIds.add("$treeDocId/Media/.Statuses")
                    candidateDocIds.add("$treeDocId/Media/Statuses")
                } else {
                    // Usually com.whatsapp or com.whatsapp.w4b
                    candidateDocIds.add("$treeDocId/WhatsApp/Media/.Statuses")
                    candidateDocIds.add("$treeDocId/WhatsApp Business/Media/.Statuses")
                    candidateDocIds.add("$treeDocId/WhatsApp/Media/Statuses")
                    candidateDocIds.add("$treeDocId/Media/.Statuses")
                    candidateDocIds.add("$treeDocId/.Statuses")
                    candidateDocIds.add(treeDocId)
                }

                var safFound = false

                // Method 2A: Direct ContentResolver Query on childrenUri (super fast & accurate)
                for (docId in candidateDocIds) {
                    try {
                        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                        val cursor = context.contentResolver.query(
                            childrenUri,
                            arrayOf(
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                DocumentsContract.Document.COLUMN_MIME_TYPE,
                                DocumentsContract.Document.COLUMN_SIZE,
                                DocumentsContract.Document.COLUMN_LAST_MODIFIED
                            ),
                            null,
                            null,
                            null
                        )

                        cursor?.use { c ->
                            val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                            val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                            val sizeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                            val modCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                            var count = 0
                            while (c.moveToNext()) {
                                val childId = c.getString(idCol)
                                val name = c.getString(nameCol) ?: continue
                                if (isValidStatusFile(name)) {
                                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                                    val isVid = name.endsWith(".mp4", ignoreCase = true)
                                    val size = if (sizeCol >= 0) c.getLong(sizeCol) else 0L
                                    val mod = if (modCol >= 0) c.getLong(modCol) else 0L

                                    result.add(
                                        WhatsAppStatusItem(
                                            uri = childUri,
                                            name = name,
                                            isVideo = isVid,
                                            size = size,
                                            lastModified = mod
                                        )
                                    )
                                    count++
                                }
                            }
                            if (count > 0) {
                                Log.i(TAG, "Successfully loaded $count statuses from docId: $docId")
                                prefs.edit().putString(cacheKey, docId).apply()
                                safFound = true
                            }
                        }
                        if (safFound) break
                    } catch (e: Exception) {
                        Log.d(TAG, "Candidate docId query skipped for $docId: ${e.message}")
                    }
                }

                // Method 2B: DocumentFile tree navigation fallback
                if (!safFound) {
                    val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
                    if (rootDoc != null) {
                        var target = rootDoc
                        if (!target.name.equals(".Statuses", ignoreCase = true) && !target.name.equals("Statuses", ignoreCase = true)) {
                            val wa = target.findFile("WhatsApp") ?: target.findFile("WhatsApp Business") ?: target
                            val media = wa.findFile("Media") ?: wa
                            target = media.findFile(".Statuses") ?: media.findFile("Statuses") ?: media
                        }

                        val files = target.listFiles()
                        for (doc in files) {
                            val name = doc.name ?: continue
                            if (isValidStatusFile(name)) {
                                result.add(
                                    WhatsAppStatusItem(
                                        uri = doc.uri,
                                        name = name,
                                        isVideo = name.endsWith(".mp4", ignoreCase = true),
                                        size = doc.length(),
                                        lastModified = doc.lastModified()
                                    )
                                )
                            }
                        }
                        Log.d(TAG, "DocumentFile traversal found ${result.size} files in ${target.name}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error listing SAF statuses: ${e.message}", e)
            }
        }

        // De-duplicate by filename and sort newest first
        val finalStatuses = result
            .distinctBy { it.name }
            .sortedByDescending { it.lastModified }

        Log.i(TAG, "Total WhatsApp statuses loaded for business=$isBusiness: ${finalStatuses.size}")
        return finalStatuses
    }

    private fun isValidStatusFile(name: String): Boolean {
        if (name.startsWith(".nomedia", ignoreCase = true)) return false
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
