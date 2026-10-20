package com.kmb.downloader.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.kmb.downloader.KmbApp
import com.kmb.downloader.MainActivity
import com.kmb.downloader.R
import com.kmb.downloader.data.AppDatabase
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class DownloadService : Service() {

    private var serviceJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine error in DownloadService", throwable)
    }
    private var serviceScope = CoroutineScope(Dispatchers.IO + serviceJob + exceptionHandler)

    private lateinit var db: AppDatabase
    private val activeTasks = ConcurrentHashMap<Long, String>()
    private val activeJobsCount = AtomicInteger(0)
    private var isForegroundActive = false

    override fun onCreate() {
        super.onCreate()
        try {
            db = AppDatabase.getDatabase(this)
            createNotificationChannel()
            resetServiceScope()
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing DownloadService", e)
        }
    }

    private fun resetServiceScope() {
        if (serviceJob.isCancelled || serviceJob.isCompleted) {
            serviceJob = SupervisorJob()
            serviceScope = CoroutineScope(Dispatchers.IO + serviceJob + exceptionHandler)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val itemId = intent?.getLongExtra(EXTRA_ITEM_ID, -1L) ?: -1L
        val notifId = if (itemId > 0) (itemId % 10000 + 2000).toInt() else 2001

        // Immediate startForeground call to satisfy Android 5-second foreground requirement
        startForegroundSafely(notifId, buildNotification(notifId, itemId, "Starting KMB Downloader…", 0, indeterminate = true))

        if (intent == null) {
            checkAndStopIfIdle()
            return START_NOT_STICKY
        }

        val action = intent.action
        if (action == ACTION_CANCEL && itemId > 0) {
            val processId = activeTasks[itemId]
            if (processId != null) {
                DownloadHelper.cancelDownload(processId)
            }
            resetServiceScope()
            serviceScope.launch {
                try {
                    db.downloadDao().getDownloadById(itemId)?.let {
                        db.downloadDao().updateDownload(it.copy(status = "Cancelled", progress = 0f))
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "Failed to update cancel status: ${t.message}")
                }
            }
            checkAndStopIfIdle()
            return START_NOT_STICKY
        }

        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            checkAndStopIfIdle()
            return START_NOT_STICKY
        }

        val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "Best"
        activeJobsCount.incrementAndGet()
        resetServiceScope()

        val processId = "kmb_${itemId}_${System.currentTimeMillis()}"
        if (itemId > 0) {
            activeTasks[itemId] = processId
        }

        serviceScope.launch {
            val logRingBuffer = java.util.ArrayDeque<String>(40)
            try {
                // Wait up to 60s for engine initialization if not ready yet
                val engineReady = withTimeoutOrNull(60_000L) {
                    KmbApp.initState.first { it != null }
                } ?: false

                if (!engineReady) {
                    Log.e(TAG, "Engine not ready, cannot proceed with download")
                    updateDbItem(itemId) { it.copy(status = "Failed", fileSize = "Engine not ready", progress = 0f) }
                    updateNotification(notifId, itemId, "Engine not ready — restart app", 0, isOngoing = false)
                    return@launch
                }

                // Initial DB state: Downloading (only written ONCE at start)
                updateDbItem(itemId) { it.copy(status = "Downloading", progress = 0f) }
                DownloadTracker.updateProgress(itemId, 0f, "Connecting…")
                updateNotification(notifId, itemId, "Connecting to server…", 0, indeterminate = true)

                // Safe output directory in app internal storage (100% writable Linux subprocess)
                val internalOutputDir = DownloadHelper.getDownloadDir(this@DownloadService)
                val request = DownloadHelper.buildRequest(url, quality, internalOutputDir)

                val existingFiles = internalOutputDir.listFiles()?.map { it.absolutePath }?.toSet() ?: emptySet()

                var lastProgressTime = 0L
                var lastNotifTime = 0L
                var lastStatusNotifTime = 0L

                YoutubeDL.getInstance().execute(request, processId) { progress, _, line ->
                    // Maintain bounded log ring buffer
                    synchronized(logRingBuffer) {
                        if (logRingBuffer.size >= 40) logRingBuffer.removeFirst()
                        logRingBuffer.addLast(line)
                    }

                    if (progress >= 0f) {
                        val validProg = progress.coerceIn(0f, 100f)
                        val now = System.currentTimeMillis()

                        // Fast in-memory UI state update (every 250ms) - ZERO disk I/O!
                        if (now - lastProgressTime > 250 || validProg >= 100f) {
                            lastProgressTime = now
                            DownloadTracker.updateProgress(itemId, validProg, "Downloading… ${validProg.toInt()}%")
                        }

                        // Strictly throttled notification updates (at most once every 1500ms) dispatched asynchronously
                        if (now - lastNotifTime > 1500 || validProg >= 100f) {
                            lastNotifTime = now
                            serviceScope.launch {
                                updateNotification(
                                    notifId,
                                    itemId,
                                    "Downloading… ${validProg.toInt()}%",
                                    validProg.toInt(),
                                    indeterminate = false
                                )
                            }
                        }
                    } else {
                        // Negative progress = status message like extracting URL / connecting
                        val now = System.currentTimeMillis()
                        if (now - lastStatusNotifTime > 2500) {
                            lastStatusNotifTime = now
                            val cleanStatus = if (line.contains("]")) line.substringAfter("]").trim() else line.trim()
                            if (cleanStatus.isNotBlank() && cleanStatus.length < 35 && !cleanStatus.startsWith("ETA")) {
                                DownloadTracker.updateStatus(itemId, cleanStatus)
                                serviceScope.launch {
                                    updateNotification(notifId, itemId, cleanStatus, 0, indeterminate = true)
                                }
                            }
                        }
                    }
                }

                // Identify the downloaded file
                val currentFiles = internalOutputDir.listFiles() ?: emptyArray()
                val newFile = currentFiles
                    .filter { !existingFiles.contains(it.absolutePath) && !it.name.endsWith(".part") && !it.name.endsWith(".ytdl") }
                    .maxByOrNull { it.lastModified() }
                    ?: currentFiles.maxByOrNull { it.lastModified() }

                var finalPath: String? = null
                var finalTitle: String? = null
                var sizeString: String? = null

                if (newFile != null && newFile.exists() && newFile.length() > 0) {
                    finalTitle = newFile.nameWithoutExtension
                    sizeString = "${String.format("%.1f", newFile.length() / 1048576.0)} MB"

                    // Save to user public Downloads via MediaStore (Android 10+) or public dir
                    finalPath = DownloadHelper.saveToPublicStorage(
                        this@DownloadService,
                        newFile,
                        quality == "Audio Only"
                    ) ?: newFile.absolutePath

                    Log.i(TAG, "Download saved successfully at: $finalPath")
                } else {
                    finalPath = newFile?.absolutePath
                }

                val finalResolvedPath = finalPath
                val finalResolvedTitle = finalTitle
                val finalResolvedSize = sizeString

                // Single final DB update upon completion
                updateDbItem(itemId) { prev ->
                    prev.copy(
                        title = if (finalResolvedTitle != null && (prev.title == "Downloading…" || prev.title == "Unknown")) finalResolvedTitle else prev.title,
                        status = "Completed",
                        progress = 100f,
                        filePath = finalResolvedPath ?: prev.filePath,
                        fileSize = finalResolvedSize ?: prev.fileSize
                    )
                }

                DownloadTracker.remove(itemId)
                updateNotification(notifId, itemId, "✓ Download complete", 100, isOngoing = false, indeterminate = false)
                Log.i(TAG, "Download finished successfully: $finalResolvedPath")

            } catch (e: Throwable) {
                Log.e(TAG, "Download error for item $itemId: ${e.message}", e)

                // Extract human-readable error from logs
                val errorLine = synchronized(logRingBuffer) {
                    logRingBuffer.filter { it.contains("ERROR:", ignoreCase = true) }
                        .lastOrNull()
                        ?.substringAfter("ERROR:")
                        ?.trim()
                        ?.take(80)
                }

                val displayError = errorLine ?: (e.message ?: "Download failed").take(60)

                updateDbItem(itemId) { it.copy(status = "Failed", fileSize = displayError, progress = 0f) }
                DownloadTracker.remove(itemId)
                updateNotification(notifId, itemId, "Failed: $displayError", 0, isOngoing = false, indeterminate = false)
            } finally {
                if (itemId > 0) activeTasks.remove(itemId)
                activeJobsCount.decrementAndGet()
                checkAndStopIfIdle()
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun updateDbItem(itemId: Long, transform: (com.kmb.downloader.data.DownloadItem) -> com.kmb.downloader.data.DownloadItem) {
        if (itemId <= 0) return
        try {
            val item = db.downloadDao().getDownloadById(itemId) ?: return
            db.downloadDao().updateDownload(transform(item))
        } catch (e: Throwable) {
            Log.w(TAG, "DB update error: ${e.message}")
        }
    }

    private fun checkAndStopIfIdle() {
        if (activeJobsCount.get() <= 0) {
            try {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundActive = false
            } catch (_: Throwable) {}
            stopSelf()
        }
    }

    private fun startForegroundSafely(notifId: Int, notification: Notification) {
        if (isForegroundActive) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(notifId, notification)
            }
            isForegroundActive = true
        } catch (e: Throwable) {
            Log.e(TAG, "startForeground error", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "KMB Downloader download progress"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        notifId: Int,
        itemId: Long,
        text: String,
        progress: Int,
        isOngoing: Boolean = true,
        indeterminate: Boolean = false
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanProgress = progress.coerceIn(0, 100)

        // Use valid monochrome vector drawable (never an adaptive-icon XML)
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KMB Downloader")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppIntent)
            .setOngoing(isOngoing)
            .setProgress(100, cleanProgress, indeterminate)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (isOngoing && itemId > 0) {
            val cancelIntent = PendingIntent.getService(
                this,
                notifId,
                Intent(this, DownloadService::class.java).apply {
                    action = ACTION_CANCEL
                    putExtra(EXTRA_ITEM_ID, itemId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
        }

        return builder.build()
    }

    private fun updateNotification(
        notifId: Int,
        itemId: Long,
        text: String,
        progress: Int,
        isOngoing: Boolean = true,
        indeterminate: Boolean = false
    ) {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(notifId, buildNotification(notifId, itemId, text, progress, isOngoing, indeterminate))
        } catch (e: Throwable) {
            Log.w(TAG, "updateNotification failed: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "DownloadService"
        const val CHANNEL_ID = "kmb_downloads"
        const val ACTION_CANCEL = "com.kmb.downloader.CANCEL"
        const val EXTRA_URL = "url"
        const val EXTRA_QUALITY = "quality"
        const val EXTRA_ITEM_ID = "item_id"
    }
}
