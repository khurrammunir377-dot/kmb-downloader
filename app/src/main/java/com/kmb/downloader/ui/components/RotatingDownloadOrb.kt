package com.kmb.downloader.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kmb.downloader.ui.theme.KmbGlow
import com.kmb.downloader.ui.theme.KmbPrimary
import com.kmb.downloader.ui.theme.KmbPrimaryLight
import com.kmb.downloader.ui.theme.KmbSecondary

@Composable
fun RotatingDownloadOrb(
    isDownloading: Boolean,
    downloadProgress: Float,
    hasReadyLink: Boolean,
    platformText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(120),
        label = "pressScale"
    )

    // Animation speeds: spins faster when downloading!
    val rotationSpeed = if (isDownloading) 1200 else 6000
    val innerSpeed = if (isDownloading) 900 else 4500

    val infiniteTransition = rememberInfiniteTransition(label = "orbAnimations")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotationSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(innerSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isDownloading) 800 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val coreScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreScale"
    )

    val outerSweepBrush = remember {
        Brush.sweepGradient(listOf(KmbSecondary, KmbPrimaryLight, KmbPrimary))
    }

    val ambientBrushReady = remember {
        Brush.radialGradient(listOf(KmbSecondary, KmbGlow, Color.Transparent))
    }
    val ambientBrushDefault = remember {
        Brush.radialGradient(listOf(KmbPrimary, KmbGlow, Color.Transparent))
    }

    val downloadingCenterBrush = remember {
        Brush.radialGradient(listOf(Color(0xFF00CEC9), Color(0xFF0984E3), Color(0xFF1E1E36)))
    }
    val readyCenterBrush = remember {
        Brush.radialGradient(listOf(Color(0xFF6C5CE7), Color(0xFF00CEC9), Color(0xFF0F0C29)))
    }
    val defaultCenterBrush = remember {
        Brush.radialGradient(listOf(Color(0xFF5E35B1), Color(0xFF311B92), Color(0xFF0B0B1A)))
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(pressScale * coreScale)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Background Canvas drawing the multi-layer rotating rings
            Canvas(modifier = Modifier.size(240.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2f - 12.dp.toPx()
                val middleRadius = outerRadius - 16.dp.toPx()
                val innerRadius = middleRadius - 14.dp.toPx()

                // 1. Soft radial ambient backlight (zero per-frame shader allocations)
                drawCircle(
                    brush = if (hasReadyLink) ambientBrushReady else ambientBrushDefault,
                    center = center,
                    radius = size.minDimension / 2f,
                    alpha = 0.35f * pulseGlow
                )

                // 2. Outer Orbit: 3 segmented rotating arcs
                rotate(degrees = outerRotation, pivot = center) {
                    val arcAngle = 75f
                    val gap = 45f
                    for (i in 0..2) {
                        val startAngle = i * (arcAngle + gap)
                        drawArc(
                            brush = outerSweepBrush,
                            startAngle = startAngle,
                            sweepAngle = arcAngle,
                            useCenter = false,
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
                            size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2)
                        )
                    }
                }

                // 3. Middle Orbit: 4 counter-rotating dotted/segmented neon arcs
                rotate(degrees = innerRotation, pivot = center) {
                    val arcAngle = 55f
                    val gap = 35f
                    for (i in 0..3) {
                        val startAngle = i * (arcAngle + gap)
                        drawArc(
                            color = (if (hasReadyLink) KmbSecondary else KmbGlow).copy(alpha = 0.75f * pulseGlow),
                            startAngle = startAngle,
                            sweepAngle = arcAngle,
                            useCenter = false,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                            topLeft = Offset(center.x - middleRadius, center.y - middleRadius),
                            size = androidx.compose.ui.geometry.Size(middleRadius * 2, middleRadius * 2)
                        )
                    }
                }

                // 4. Central Solid Gradient Orb
                drawCircle(
                    brush = when {
                        isDownloading -> downloadingCenterBrush
                        hasReadyLink -> readyCenterBrush
                        else -> defaultCenterBrush
                    },
                    center = center,
                    radius = innerRadius
                )

                // 5. Glowing border for the central orb
                drawCircle(
                    color = (if (hasReadyLink) KmbSecondary else KmbPrimaryLight).copy(alpha = 0.8f * pulseGlow),
                    center = center,
                    radius = innerRadius,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Foreground Content inside the Core
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isDownloading) {
                    Icon(
                        imageVector = Icons.Rounded.Downloading,
                        contentDescription = "Downloading",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (downloadProgress <= 0f) "CONNECTING" else "${downloadProgress.toInt()}%",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (downloadProgress <= 0f) 14.sp else 22.sp,
                            letterSpacing = if (downloadProgress <= 0f) 1.sp else 0.sp
                        ),
                        color = Color.White
                    )
                } else if (hasReadyLink) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Download ready",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "DOWNLOAD",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = Color.White
                    )
                    if (!platformText.isNullOrBlank()) {
                        Text(
                            text = platformText,
                            style = MaterialTheme.typography.labelSmall,
                            color = KmbSecondary
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Tap to Download",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "TAP TO GET",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
