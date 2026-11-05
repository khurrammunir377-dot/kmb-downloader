package com.kmb.downloader.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kmb.downloader.MainActivity
import com.kmb.downloader.R
import com.kmb.downloader.data.AppDatabase
import com.kmb.downloader.data.SavedLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Background monitor service that automatically catches and saves any copied
 * video / media link in real time across any app on the phone into Saved Links.
 */
class ClipboardMonitorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private var clipboardManager: ClipboardManager? = null

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        onClipboardChanged()
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForegroundSafely()

        try {
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboardManager?.addPrimaryClipChangedListener(clipListener)
            Log.d(TAG, "Clipboard monitor listener registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register clip listener: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundSafely()
        // Check current clip immediately on start
        onClipboardChanged()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            clipboardManager?.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {}
        job.cancel()
        isRunning = false
        super.onDestroy()
    }

    private fun onClipboardChanged() {
        scope.launch {
            try {
                val cm = clipboardManager ?: return@launch
                val clip = cm.primaryClip ?: return@launch
                if (clip.itemCount == 0) return@launch

                val db = AppDatabase.getDatabase(applicationContext)
                val savedDao = db.savedLinkDao()

                for (i in 0 until clip.itemCount) {
                    val rawText = clip.getItemAt(i)?.coerceToText(applicationContext)?.toString()?.trim() ?: continue
                    if (rawText.isBlank()) continue

                    val urls = DownloadHelper.extractAllUrls(rawText)
                    for (rawUrl in urls) {
                        val clean = DownloadHelper.cleanUrl(rawUrl)
                        if (DownloadHelper.isDownloadableLink(clean)) {
                            val existing = savedDao.findByUrl(clean)
                            if (existing == null) {
                                val platform = DownloadHelper.detectPlatform(clean)
                                val initialTitle = "$platform Video"
                                val newId = savedDao.insert(
                                    SavedLink(
                                        url = clean,
                                        platform = platform,
                                        title = initialTitle,
                                        detectedAt = System.currentTimeMillis()
                                    )
                                )
                                Log.i(TAG, "Auto-saved new copied link [$platform]: $clean (id: $newId)")

                                // Asynchronously fetch full video title and description
                                launch {
                                    try {
                                        val meta = MetadataFetcherHelper.fetchMetadata(clean)
                                        if (meta != null && (meta.title.isNotBlank() || !meta.description.isNullOrBlank())) {
                                            val current = savedDao.findByUrl(clean)
                                            if (current != null) {
                                                savedDao.update(
                                                    current.copy(
                                                        title = meta.title.takeIf { it.isNotBlank() } ?: current.title,
                                                        note = meta.description?.takeIf { it.isNotBlank() } ?: current.note,
                                                        thumbnail = meta.thumbnail ?: current.thumbnail
                                                    )
                                                )
                                                Log.i(TAG, "Updated metadata for $clean: ${meta.title}")
                                            }
                                        }
                                    } catch (_: Throwable) {}
                                }
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Error processing clipboard clip: ${e.message}")
            }
        }
    }

    private fun startForegroundSafely() {
        val notification = buildServiceNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed startForeground: ${e.message}", e)
        }
    }

    private fun buildServiceNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KMB Downloader • Auto Link Saver")
            .setContentText("Listening for copied video links across all apps")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "KMB Clipboard Link Monitor",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors clipboard to auto-save video links you copy"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "ClipboardMonitor"
        private const val CHANNEL_ID = "kmb_clipboard_channel"
        private const val NOTIFICATION_ID = 3001

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            try {
                val intent = Intent(context, ClipboardMonitorService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not start ClipboardMonitorService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, ClipboardMonitorService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {}
        }
    }
}
