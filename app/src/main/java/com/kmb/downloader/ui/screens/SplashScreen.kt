package com.kmb.downloader.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.ui.theme.BackgroundDark
import com.kmb.downloader.ui.theme.KmbGlow
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbPrimaryLight
import com.kmb.downloader.ui.theme.KmbSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    val ringExpand = remember { Animatable(0.4f) }

    val infiniteTransition = rememberInfiniteTransition(label = "splashAnimations")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRotation"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(500))
        scale.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
        ringExpand.animateTo(1.2f, animationSpec = tween(1200, easing = FastOutSlowInEasing))
        delay(1600)
        alpha.animateTo(0f, animationSpec = tween(400))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1B1842),
                        Color(0xFF0F0E26),
                        BackgroundDark
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // High-Tech Cyber Canvas Background Rings
        Canvas(modifier = Modifier.size(340.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Outer expanding ambient halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        KmbPrimary.copy(alpha = 0.35f * glowPulse),
                        KmbSecondary.copy(alpha = 0.15f * glowPulse),
                        Color.Transparent
                    ),
                    center = center,
                    radius = (size.minDimension / 2f) * ringExpand.value
                ),
                center = center,
                radius = (size.minDimension / 2f) * ringExpand.value
            )

            // Revolving neon segmented arcs
            rotate(degrees = rotation, pivot = center) {
                val arcRadius = 135.dp.toPx()
                for (i in 0..2) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(KmbSecondary, KmbPrimaryLight, KmbPrimary)
                        ),
                        startAngle = i * 120f,
                        sweepAngle = 70f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                        size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)
                    )
                }
            }

            // Counter revolving dotted ring
            rotate(degrees = counterRotation, pivot = center) {
                val arcRadius = 110.dp.toPx()
                for (i in 0..3) {
                    drawArc(
                        color = KmbGlow.copy(alpha = 0.7f * glowPulse),
                        startAngle = i * 90f,
                        sweepAngle = 45f,
                        useCenter = false,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
                        size = androidx.compose.ui.geometry.Size(arcRadius * 2, arcRadius * 2)
                    )
                }
            }
        }

        // Animated Foreground Core & Typography
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Central Logo Card with Glow
            Box(
                modifier = Modifier
                    .size(width = 160.dp, height = 92.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(KmbSecondary, KmbPrimaryLight, KmbGlow.copy(alpha = 0.9f * glowPulse))
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.kmb.downloader.R.drawable.kmb_logo),
                    contentDescription = "KMB Logo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.height(30.dp))

            // Main Brand Title
            Text(
                text = "KMB DOWNLOADER",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.5.sp
                ),
                color = Color.White
            )

            Spacer(Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Instant Media Grabber  •  Save Now, Download Later",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                ),
                color = KmbSecondary.copy(alpha = 0.95f)
            )

            Spacer(Modifier.height(24.dp))

            // Futuristic Pill Badge
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = KmbPrimaryLight.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "POWERED BY YT-DLP • 1000+ SITES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = KmbGlow
                )
            }
        }
    }
}
