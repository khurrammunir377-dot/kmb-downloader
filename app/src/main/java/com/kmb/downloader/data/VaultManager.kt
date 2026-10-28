package com.kmb.downloader.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class VaultFileItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val lastModified: Long
)

object VaultManager {
    private const val PREFS_NAME = "kmb_vault_prefs"
    private const val KEY_PIN_HASH = "vault_pin_hash"
    private const val VAULT_DIR_NAME = ".secret_vault"

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PIN_HASH)
    }

    fun setPin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).commit()
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashPin(pin)
    }

    fun getVaultDir(context: Context): File {
        val dir = File(context.filesDir, VAULT_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val noMedia = File(dir, ".nomedia")
        if (!noMedia.exists()) {
            try { noMedia.createNewFile() } catch (_: Exception) {}
        }
        return dir
    }

    fun getVaultFiles(context: Context): List<VaultFileItem> {
        val dir = getVaultDir(context)
        val files = dir.listFiles { f -> f.isFile && f.name != ".nomedia" } ?: return emptyList()
        return files.map { f ->
            val ext = f.extension.lowercase()
            val isVid = ext in listOf("mp4", "mkv", "webm", "avi", "mov", "3gp")
            VaultFileItem(
                file = f,
                name = f.name,
                sizeBytes = f.length(),
                isVideo = isVid,
                lastModified = f.lastModified()
            )
        }.sortedByDescending { it.lastModified }
    }

    fun importUriToVault(context: Context, uri: Uri): Boolean {
        return try {
            var fileName = "hidden_${System.currentTimeMillis()}"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        fileName = cursor.getString(nameIdx)
                    }
                }
            }

            val targetFile = File(getVaultDir(context), fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportToGallery(context: Context, vaultItem: VaultFileItem): Boolean {
        return try {
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val kmbDir = File(publicDir, "KMB Vault Restored")
            if (!kmbDir.exists()) kmbDir.mkdirs()

            val destFile = File(kmbDir, vaultItem.name)
            FileInputStream(vaultItem.file).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            // Notify MediaScanner
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                null
            ) { _, _ -> }

            // Delete from vault after successful restoration
            vaultItem.file.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteFile(vaultItem: VaultFileItem): Boolean {
        return try {
            vaultItem.file.delete()
        } catch (e: Exception) {
            false
        }
    }
}
