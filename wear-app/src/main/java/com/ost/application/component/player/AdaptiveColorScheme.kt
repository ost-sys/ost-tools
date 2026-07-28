package com.ost.application.component.player

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun dynamicColorSchemeFromSeed(seedColor: Color, fallback: ColorScheme): ColorScheme {
    val seedInt = seedColor.toArgb()
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seedInt, hsl)

    val hue = hsl[0]
    val sat = hsl[1]

    fun colorFromHsl(h: Float, s: Float, l: Float): Color {
        val outHsl = floatArrayOf((h % 360f + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
        return Color(ColorUtils.HSLToColor(outHsl))
    }

    val primarySat = sat.coerceAtLeast(0.35f).coerceAtMost(0.85f)
    val secondarySat = (sat * 0.5f).coerceAtMost(0.45f)
    val tertiaryHue = hue + 60f

    val primary = colorFromHsl(hue, primarySat, 0.78f)
    val primaryDim = colorFromHsl(hue, primarySat, 0.62f)
    val primaryContainer = colorFromHsl(hue, primarySat, 0.28f)
    val onPrimary = colorFromHsl(hue, primarySat, 0.12f)
    val onPrimaryContainer = colorFromHsl(hue, primarySat, 0.90f)

    val secondary = colorFromHsl(hue, secondarySat, 0.78f)
    val secondaryDim = colorFromHsl(hue, secondarySat, 0.62f)
    val secondaryContainer = colorFromHsl(hue, secondarySat, 0.25f)
    val onSecondary = colorFromHsl(hue, secondarySat, 0.12f)
    val onSecondaryContainer = colorFromHsl(hue, secondarySat, 0.90f)

    val tertiary = colorFromHsl(tertiaryHue, secondarySat, 0.78f)
    val tertiaryDim = colorFromHsl(tertiaryHue, secondarySat, 0.62f)
    val tertiaryContainer = colorFromHsl(tertiaryHue, secondarySat, 0.25f)
    val onTertiary = colorFromHsl(tertiaryHue, secondarySat, 0.12f)
    val onTertiaryContainer = colorFromHsl(tertiaryHue, secondarySat, 0.90f)

    val surfaceContainerLow = colorFromHsl(hue, (sat * 0.2f).coerceAtMost(0.15f), 0.08f)
    val surfaceContainer = colorFromHsl(hue, (sat * 0.2f).coerceAtMost(0.15f), 0.12f)
    val surfaceContainerHigh = colorFromHsl(hue, (sat * 0.2f).coerceAtMost(0.15f), 0.16f)

    return ColorScheme(
        primary = primary,
        primaryDim = primaryDim,
        primaryContainer = primaryContainer,
        onPrimary = onPrimary,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryDim = secondaryDim,
        secondaryContainer = secondaryContainer,
        onSecondary = onSecondary,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryDim = tertiaryDim,
        tertiaryContainer = tertiaryContainer,
        onTertiary = onTertiary,
        onTertiaryContainer = onTertiaryContainer,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        onSurface = Color(0xFFE2E2E9),
        onSurfaceVariant = Color(0xFFC4C6D0),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474E),
        background = Color.Black,
        onBackground = Color(0xFFE2E2E9),
        error = fallback.error,
        errorDim = fallback.errorDim,
        errorContainer = fallback.errorContainer,
        onError = fallback.onError,
        onErrorContainer = fallback.onErrorContainer
    )
}

@Composable
fun rememberAdaptiveColorScheme(artBitmap: Bitmap?): ColorScheme {
    val fallbackScheme = MaterialTheme.colorScheme
    var scheme by remember { mutableStateOf(fallbackScheme) }

    LaunchedEffect(artBitmap) {
        if (artBitmap != null) {
            withContext(Dispatchers.Default) {
                val palette = Palette.from(artBitmap).generate()
                val seedInt = palette.vibrantSwatch?.rgb
                    ?: palette.dominantSwatch?.rgb
                    ?: palette.mutedSwatch?.rgb
                if (seedInt != null) {
                    scheme = dynamicColorSchemeFromSeed(Color(seedInt), fallbackScheme)
                } else {
                    scheme = fallbackScheme
                }
            }
        } else {
            scheme = fallbackScheme
        }
    }

    return scheme
}
