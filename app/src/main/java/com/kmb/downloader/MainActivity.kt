package com.kmb.downloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kmb.downloader.ui.components.KmbBottomNav
import com.kmb.downloader.ui.screens.AiStudioScreen
import com.kmb.downloader.ui.screens.HomeScreen
import com.kmb.downloader.ui.screens.SplashScreen
import com.kmb.downloader.ui.screens.VipProScreen
import com.kmb.downloader.ui.screens.WhatsAppStatusScreen
import com.kmb.downloader.ui.theme.KMBDownloaderTheme
import com.kmb.downloader.viewmodel.MainViewModel
import com.kmb.downloader.viewmodel.WhatsAppStatusViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val waViewModel: WhatsAppStatusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                delay(350)
                viewModel.checkClipboardAndSave()
            }
        }

        setContent {
            KMBDownloaderTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val showSplash by viewModel.showSplash.collectAsState()
                    var currentScreen by remember { mutableStateOf("downloader") }

                    BackHandler(enabled = currentScreen != "downloader") {
                        currentScreen = "downloader"
                    }

                    AnimatedContent(
                        targetState = showSplash,
                        transitionSpec = {
                            fadeIn(tween(600)) togetherWith fadeOut(tween(500))
                        },
                        label = "splashTransition"
                    ) { isSplash ->
                        if (isSplash) {
                            SplashScreen(onFinished = { viewModel.finishSplash() })
                        } else {
                            Scaffold(
                                bottomBar = {
                                    KmbBottomNav(
                                        currentRoute = currentScreen,
                                        onNavigate = { currentScreen = it }
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = innerPadding.calculateBottomPadding())
                                ) {
                                    AnimatedContent(
                                        targetState = currentScreen,
                                        transitionSpec = {
                                            fadeIn(tween(250)) togetherWith fadeOut(tween(200))
                                        },
                                        label = "screenTransition"
                                    ) { screen ->
                                        when (screen) {
                                            "whatsapp" -> {
                                                WhatsAppStatusScreen(
                                                    viewModel = waViewModel,
                                                    onBack = { currentScreen = "downloader" }
                                                )
                                            }
                                            "ai_studio" -> {
                                                AiStudioScreen(
                                                    onNavigateBack = { currentScreen = "downloader" }
                                                )
                                            }
                                            "vip_pro" -> {
                                                VipProScreen()
                                            }
                                            else -> {
                                                HomeScreen(
                                                    viewModel = viewModel,
                                                    onNavigateToWhatsAppStatus = { currentScreen = "whatsapp" },
                                                    onNavigateToAiStudio = { currentScreen = "ai_studio" }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        try {
            when (intent?.action) {
                Intent.ACTION_SEND -> {
                    if (intent.type == "text/plain") {
                        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                        if (!sharedText.isNullOrBlank()) {
                            viewModel.pasteFromClipboard(sharedText)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Guard against intent parsing errors
        }
    }
}
