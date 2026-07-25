package com.ost.application.ui.components
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
@Composable
fun MeshGradientBackground(modifier: Modifier = Modifier, animateEntrance: Boolean = false) {
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (animateEntrance) isLaunched = true
    }
    val entryAlpha by animateFloatAsState(
        targetValue = if (animateEntrance && !isLaunched) 0f else 1f,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "entryAlpha"
    )
    val entryScale by animateFloatAsState(
        targetValue = if (animateEntrance && !isLaunched) 0.5f else 1.5f,
        animationSpec = tween(durationMillis = 4000, easing = EaseOutCubic),
        label = "entryScale"
    )
    val entryRotation by animateFloatAsState(
        targetValue = if (animateEntrance && !isLaunched) -540f else 0f,
        animationSpec = tween(durationMillis = 4000, easing = EaseOutCubic),
        label = "entryRotation"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_gradient")
    val x1 by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x1"
    )
    val y1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y1"
    )
    val x2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x2"
    )
    val y2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y2"
    )
    val isDark = isSystemInDarkTheme()
    val circle1Alpha = if (isDark) 0.35f else 0.22f
    val circle2Alpha = if (isDark) 0.25f else 0.15f
    val baseBg1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.5f else 0.6f)
    val baseBg2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (isDark) 0.5f else 0.6f)
    val baseBg3 = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isDark) 0.3f else 0.4f)
    val baseBg4 = MaterialTheme.colorScheme.surface
    val colorCircle1 = MaterialTheme.colorScheme.primary
    val colorCircle2 = MaterialTheme.colorScheme.tertiary
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(baseBg1, baseBg2, baseBg3, baseBg4)
                )
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (animateEntrance) {
                        alpha = entryAlpha
                        scaleX = entryScale
                        scaleY = entryScale
                        rotationZ = entryRotation
                    }
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorCircle1.copy(alpha = circle1Alpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width * x1, size.height * y1),
                    radius = size.minDimension * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorCircle2.copy(alpha = circle2Alpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width * x2, size.height * y2),
                    radius = size.minDimension * 0.5f
                )
            )
        }
    }
}
