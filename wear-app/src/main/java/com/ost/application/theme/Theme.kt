package com.ost.application.theme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme
val DefaultBrandColors = ColorScheme()
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