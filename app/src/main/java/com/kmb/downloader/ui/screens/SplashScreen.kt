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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kmb.downloader.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.85f) }
    val alpha = remember { Animatable(0f) }

    // Breathing pulse for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Smooth circular rotation for loader
    val loaderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loaderRotation"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(600))
        scale.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        // View duration so user can see and enjoy the new look
        delay(2600)
        alpha.animateTo(0f, animationSpec = tween(400))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040D1A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onFinished()
            }
    ) {
        // High-Resolution Background Art with Fluid Ribbons
        Image(
            painter = painterResource(id = R.drawable.bg_splash_art),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Radial ambient glow behind central logo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(340.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.12f),
                            Color(0xFF0091EA).copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Content Column matching exact layout in reference design
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

            // 3D Ribbon Logo
            Image(
                painter = painterResource(id = R.drawable.kmb_ribbon_logo),
                contentDescription = "KMB Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(width = 175.dp, height = 160.dp)
                    .scale(scale.value * pulseScale)
            )

            Spacer(Modifier.height(18.dp))

            // "KMB" Brand Text (Bold White)
            Text(
                text = "KMB",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.5.sp,
                color = Color.White
            )

            Spacer(Modifier.height(2.dp))

            // "DOWNLOADER" Text (Electric Cyan with wide tracking)
            Text(
                text = "DOWNLOADER",
                fontSize = 17.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 7.sp,
                color = Color(0xFF00E5FF)
            )

            Spacer(Modifier.height(18.dp))

            // Glowing Light Beam Divider
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            ) {
                // Radial soft glow flare at the center
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 10.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF00E5FF).copy(alpha = 0.7f),
                                    Color(0xFF00E5FF).copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                // Crisp horizontal beam tapering to transparent
                Box(
                    modifier = Modifier
                        .width(220.dp)
                        .height(2.dp)
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

            Spacer(Modifier.height(14.dp))

            // "Download • Save • Enjoy" Subtitle
            Text(
                text = "Download  •  Save  •  Enjoy",
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.2.sp,
                color = Color(0xFFCFD8DC)
            )

            Spacer(Modifier.height(72.dp))

            // Glowing Circular Ring Loader
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(46.dp)
            ) {
                // Ambient glow around ring
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                Canvas(modifier = Modifier.size(36.dp)) {
                    val strokeWidth = 3.6.dp.toPx()
                    // Dark navy circular track
                    drawCircle(
                        color = Color(0xFF022B48),
                        style = Stroke(width = strokeWidth)
                    )
                    // Rotating glowing cyan arc
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.15f),
                                Color(0xFF00E5FF)
                            )
                        ),
                        startAngle = loaderRotation,
                        sweepAngle = 135f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Footer Version Text
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
