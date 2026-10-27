package com.kmb.downloader

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KmbApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    companion object {
        private const val TAG = "KmbApp"

        // Tri-state: null = initializing, true = ready, false = failed
        private val _initState = MutableStateFlow<Boolean?>(null)
        val initState: StateFlow<Boolean?> = _initState.asStateFlow()

        fun isInitialized(): Boolean = _initState.value == true
    }

    override fun onCreate() {
        super.onCreate()

        // Global crash logger to log issues before delegating to the system handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // Initialize YoutubeDL and FFmpeg in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Initializing YoutubeDL...")
                YoutubeDL.getInstance().init(this@KmbApp)
                Log.d(TAG, "YoutubeDL core initialized")

                try {
                    Log.d(TAG, "Initializing FFmpeg...")
                    FFmpeg.getInstance().init(this@KmbApp)
                    Log.d(TAG, "FFmpeg initialized successfully")
                } catch (fe: Throwable) {
                    Log.w(TAG, "FFmpeg init warning (will download without stream merge if unavailable): ${fe.message}")
                }

                _initState.value = true
                Log.i(TAG, "Engine ready for downloads!")
            } catch (e: Throwable) {
                Log.e(TAG, "YoutubeDL engine init failed: ${e.message}", e)
                _initState.value = false
            }
        }
    }
}
