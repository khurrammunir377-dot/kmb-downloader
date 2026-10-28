package com.kmb.downloader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.ui.theme.BackgroundDark
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbPrimaryLight
import com.kmb.downloader.ui.theme.KmbSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.75f) }
    val alpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(500))
        scale.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        // Increased view duration as requested (~3.2 seconds total view time)
        delay(2400)
        alpha.animateTo(0f, animationSpec = tween(400))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0C29),
                        Color(0xFF1B1238),
                        BackgroundDark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient background aura
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            KmbPrimary.copy(alpha = 0.22f),
                            KmbSecondary.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
                .padding(horizontal = 24.dp)
        ) {
            // Central Official 3D KMB Logo Card with dynamic glowing border
            Box(
                modifier = Modifier
                    .size(width = 175.dp, height = 102.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(KmbSecondary, KmbPrimaryLight, KmbPrimary)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.kmb.downloader.R.drawable.kmb_logo),
                    contentDescription = "KMB Apps Logo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(26.dp))

            // Main Brand Title
            Text(
                text = "KMB DOWNLOADER",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.8.sp
                ),
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            // Elegant Subtitle
            Text(
                text = "All-in-One Instant Media Grabber",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    fontSize = 15.sp
                ),
                color = KmbSecondary
            )

            Spacer(Modifier.height(20.dp))

            // Clean Feature Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FeaturePill(text = "4K & HD Video")
                FeaturePill(text = "Audio / MP3")
                FeaturePill(text = "WhatsApp Status")
            }

            Spacer(Modifier.height(36.dp))

            // Sleek Loading Progress Indicator
            LinearProgressIndicator(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = KmbSecondary,
                trackColor = Color.White.copy(alpha = 0.12f)
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "v3.6.0 • Fast & Secure Engine",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.8.sp,
                    fontSize = 11.sp
                ),
                color = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun FeaturePill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        KmbPrimary.copy(alpha = 0.4f),
                        KmbSecondary.copy(alpha = 0.4f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
                fontSize = 11.5.sp
            ),
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
