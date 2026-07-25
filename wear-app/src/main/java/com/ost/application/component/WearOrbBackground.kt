package com.ost.application.component
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.material3.MaterialTheme
@Composable
fun WearOrbBackground(
    modifier: Modifier = Modifier,
    animateEntrance: Boolean = true
) {
    val entryAlpha by animateFloatAsState(
        targetValue = if (animateEntrance) 1f else 0f,
        animationSpec = tween(durationMillis = 1500, easing = EaseOutCubic),
        label = "entryAlpha"
    )
    val entryRotation by animateFloatAsState(
        targetValue = if (animateEntrance) 0f else -540f,
        animationSpec = tween(durationMillis = 3000, easing = EaseOutCubic),
        label = "entryRotation"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "orb_gradient")
    val x1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x1"
    )
    val y1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y1"
    )
    val x2 by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x2"
    )
    val y2 by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(11000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y2"
    )
    val color1 = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val color2 = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = entryAlpha
                    rotationZ = entryRotation
                }
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1, Color.Transparent),
                    center = Offset(size.width * x1, size.height * y1),
                    radius = size.minDimension * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2, Color.Transparent),
                    center = Offset(size.width * x2, size.height * y2),
                    radius = size.minDimension * 0.65f
                )
            )
        }
    }
}
