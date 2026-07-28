package com.ost.application.theme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
val DefaultBrandColors = ColorScheme(
    primary = Color(0xFFAAC7FF),
    primaryDim = Color(0xFF7693C8),
    primaryContainer = Color(0xFF284777),
    onPrimary = Color(0xFF0A305F),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBEC6DC),
    secondaryDim = Color(0xFF8A93A7),
    secondaryContainer = Color(0xFF3E4759),
    onSecondary = Color(0xFF283141),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDDBCE0),
    tertiaryDim = Color(0xFFA789AB),
    tertiaryContainer = Color(0xFF573E5C),
    onTertiary = Color(0xFF3F2844),
    onTertiaryContainer = Color(0xFFFAD8FD),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    onSurface = Color(0xFFE2E2E9),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    background = Color.Black,
    onBackground = Color(0xFFE2E2E9),
    error = Color(0xFFFFB4AB),
    errorDim = Color(0xFFDD6763),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6)
)
@Composable
fun OSTToolsTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = if (dynamicColor) {
        dynamicColorScheme(context) ?: DefaultBrandColors
    } else {
        DefaultBrandColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
