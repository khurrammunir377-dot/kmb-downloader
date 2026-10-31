package com.kmb.downloader.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Universal background that applies the exact midnight-navy and electric cyan
 * curved ribbon wave graphics and atmospheric lighting across all app screens.
 */
@Composable
fun KmbScreenBackground(
    modifier: Modifier = Modifier,
    showAccentWaves: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030914),
                        Color(0xFF07162E),
                        Color(0xFF020710)
                    )
                )
            )
    ) {
        if (showAccentWaves) {
            // Native Canvas drawing the fluid ribbon curves in top-left & bottom-right
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Top-Left Fluid Ribbon Waves
                val wave1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.75f, 0f)
                    cubicTo(
                        w * 0.45f, h * 0.12f,
                        w * 0.15f, h * 0.22f,
                        0f, h * 0.38f
                    )
                    close()
                }
                drawPath(
                    path = wave1,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.22f),
                            Color(0xFF0091EA).copy(alpha = 0.14f),
                            Color(0xFF031A33).copy(alpha = 0.05f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.5f, h * 0.3f)
                    )
                )

                val wave2 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w * 0.45f, 0f)
                    cubicTo(
                        w * 0.28f, h * 0.09f,
                        w * 0.08f, h * 0.18f,
                        0f, h * 0.26f
                    )
                    close()
                }
                drawPath(
                    path = wave2,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                            Color(0xFF00B0FF).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w * 0.35f, h * 0.22f)
                    )
                )

                // Bottom-Right Fluid Ribbon Waves
                val wave3 = Path().apply {
                    moveTo(w, h)
                    lineTo(w * 0.20f, h)
                    cubicTo(
                        w * 0.52f, h * 0.88f,
                        w * 0.82f, h * 0.78f,
                        w, h * 0.62f
                    )
                    close()
                }
                drawPath(
                    path = wave3,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0091EA).copy(alpha = 0.20f),
                            Color(0xFF00E5FF).copy(alpha = 0.28f),
                            Color(0xFF031A33).copy(alpha = 0.05f)
                        ),
                        start = Offset(w * 0.3f, h),
                        end = Offset(w, h * 0.65f)
                    )
                )

                val wave4 = Path().apply {
                    moveTo(w, h)
                    lineTo(w * 0.48f, h)
                    cubicTo(
                        w * 0.72f, h * 0.92f,
                        w * 0.90f, h * 0.84f,
                        w, h * 0.74f
                    )
                    close()
                }
                drawPath(
                    path = wave4,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.38f),
                            Color(0xFF0091EA).copy(alpha = 0.20f),
                            Color.Transparent
                        ),
                        start = Offset(w * 0.5f, h),
                        end = Offset(w, h * 0.75f)
                    )
                )
            }

            // Top-left ambient cyan spotlight
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(240.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Bottom-right ambient cyan spotlight
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(280.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF0091EA).copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Screen content layer
        content()
    }
}

/**
 * Reusable horizontal neon cyan flare divider beam from the brand design.
 */
@Composable
fun KmbGlowDivider(
    modifier: Modifier = Modifier,
    width: Dp = 220.dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
    ) {
        // Soft radial glow bloom
        Box(
            modifier = Modifier
                .size(width = width * 0.5f, height = 8.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.65f),
                            Color(0xFF00E5FF).copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )
        // Crisp horizontal light beam
        Box(
            modifier = Modifier
                .width(width)
                .height(1.8.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                            Color(0xFF00E5FF),
                            Color(0xFF00E5FF).copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

/**
 * Reusable glowing circular loader ring from the brand design.
 */
@Composable
fun KmbCircularLoader(
    modifier: Modifier = Modifier,
    size: Dp = 38.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "kmbLoader")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "kmbLoaderRotation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size + 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(size + 6.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = 3.5.dp.toPx()
            // Track
            drawCircle(
                color = Color(0xFF022B48),
                style = Stroke(width = strokeWidth)
            )
            // Glowing cyan spinning arc
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF00E5FF).copy(alpha = 0.15f),
                        Color(0xFF00E5FF)
                    )
                ),
                startAngle = rotation,
                sweepAngle = 130f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}
