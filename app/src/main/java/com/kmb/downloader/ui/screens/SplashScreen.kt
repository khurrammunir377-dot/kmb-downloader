package com.kmb.downloader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.R
import com.kmb.downloader.ui.components.KmbCircularLoader
import com.kmb.downloader.ui.components.KmbGlowDivider
import com.kmb.downloader.ui.components.KmbScreenBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(500))
        scale.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
        delay(2500)
        alpha.animateTo(0f, animationSpec = tween(350))
        onFinished()
    }

    // Pure Native Compose Background (No pasted background images)
    KmbScreenBackground(
        showAccentWaves = true,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onFinished()
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .alpha(alpha.value)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.weight(1f))

            // Official 3D Ribbon Brand Logo
            Image(
                painter = painterResource(id = R.drawable.kmb_ribbon_logo),
                contentDescription = "KMB Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = 175.dp, height = 160.dp)
                    .scale(scale.value * pulseScale)
            )

            Spacer(Modifier.height(18.dp))

            // Brand Typography
            Text(
                text = "KMB",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.5.sp,
                color = Color.White
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "DOWNLOADER",
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 7.sp,
                color = Color(0xFF00E5FF)
            )

            Spacer(Modifier.height(18.dp))

            // Native Cyan Beam Divider
            KmbGlowDivider(width = 220.dp)

            Spacer(Modifier.height(14.dp))

            // Subtitle
            Text(
                text = "Download  •  Save  •  Enjoy",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.2.sp,
                color = Color(0xFFCFD8DC)
            )

            Spacer(Modifier.height(64.dp))

            // Native Cyan Animated Circular Ring Loader
            KmbCircularLoader(size = 38.dp)

            Spacer(Modifier.weight(1f))

            // Version Footer
            Text(
                text = "v4.0.0  •  Fast & Secure Engine",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp,
                color = Color(0xFF78909C),
                modifier = Modifier.padding(bottom = 20.dp)
            )
        }
    }
}
