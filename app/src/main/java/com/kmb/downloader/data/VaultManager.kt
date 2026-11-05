package com.kmb.downloader.data

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class VaultFileItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val isVideo: Boolean,
    val lastModified: Long
)

/**
 * Hardened Secret Media Vault.
 * Features:
 * - PBKDF2WithHmacSHA256 key derivation with 10,000 iterations & 16-byte SecureRandom salt.
 * - Brute-force rate limiting with 30-second lockout after 5 consecutive failures.
 * - Automatic migration from legacy unsalted SHA-256 PINs.
 * - Private internal storage isolation + .nomedia gallery shielding.
 */
object VaultManager {
    private const val PREFS_NAME = "kmb_vault_prefs"
    private const val KEY_PIN_HASH = "vault_pin_hash"
    private const val KEY_FAILED_ATTEMPTS = "vault_failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "vault_lockout_until"
    private const val VAULT_DIR_NAME = ".secret_vault"

    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH_BITS = 256
    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30000L

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = hex.substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }

    private fun hashPbkdf2(pin: String, salt: ByteArray): String {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return bytesToHex(hash)
    }

    private fun hashPinLegacy(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pin.toByteArray())
        return bytesToHex(hash)
    }

    fun isPinSet(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains(KEY_PIN_HASH)
    }

    fun setPin(context: Context, pin: String): Boolean {
        if (pin.length < 4) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Generate 16-byte cryptographically secure salt
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltHex = bytesToHex(salt)
        val hashHex = hashPbkdf2(pin, salt)

        // Format: saltHex:hashHex
        val formatted = "$saltHex:$hashHex"
        return prefs.edit()
            .putString(KEY_PIN_HASH, formatted)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .commit()
    }

    fun getLockoutRemainingSeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            ((lockoutUntil - now) / 1000).toInt() + 1
        } else {
            0
        }
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Check if locked out
        if (getLockoutRemainingSeconds(context) > 0) {
            return false
        }

        val saved = prefs.getString(KEY_PIN_HASH, null) ?: return false

        val isValid = if (saved.contains(":")) {
            val parts = saved.split(":")
            if (parts.size == 2) {
                try {
                    val salt = hexToBytes(parts[0])
                    val expectedHash = parts[1]
                    val actualHash = hashPbkdf2(pin, salt)
                    actualHash == expectedHash
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }
        } else {
            // Legacy unsalted SHA-256 check
            val legacy = hashPinLegacy(pin)
            if (saved == legacy) {
                // Auto-upgrade to PBKDF2 on successful login
                setPin(context, pin)
                true
            } else {
                false
            }
        }

        if (isValid) {
            // Reset failure counter on success
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            return true
        } else {
            // Increment failure counter
            val attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts)
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS)
                editor.putInt(KEY_FAILED_ATTEMPTS, 0)
            }
            editor.apply()
            return false
        }
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
                        val name = cursor.getString(nameIdx)
                        if (!name.isNullOrBlank()) {
                            fileName = name
                        }
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
