@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class, ExperimentalSharedTransitionApi::class)
package com.ost.application.ui.activity.setup
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ost.application.R
import com.ost.application.core.locale.LocaleHelper
import com.ost.application.ui.components.LanguagePickerDialog
import kotlinx.coroutines.delay
import org.xmlpull.v1.XmlPullParser
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.animation.animateColorAsState
@Composable
fun SharedTransitionScope.WelcomeScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    isLargeScreen: Boolean,
    isTransitioning: Boolean,
    onGetStartedClick: () -> Unit,
    onLanguageSelected: (Locale?) -> Unit
) {
    val context = LocalContext.current
    val showLanguageDialog = remember { mutableStateOf(false) }
    val supportedLocales = remember { parseSupportedLocales(context) }
    var isLaunched by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isLaunched = true
    }
    val entryAlpha by animateFloatAsState(
        targetValue = if (isLaunched && !isTransitioning) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "textAlpha"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            Spacer(modifier = Modifier.padding(top = 32.dp).size(32.dp))
            Spacer(modifier = Modifier.weight(1f))
            val headerFontFamily = remember {
                FontFamily(
                    Font(
                        resId = R.font.google_sans_flex,
                        variationSettings = FontVariation.Settings(
                            FontVariation.weight(500),
                            FontVariation.width(110f)
                        )
                    )
                )
            }
            Text(
                text = stringResource(R.string.hi_there),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = headerFontFamily,
                    fontSize = 56.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .graphicsLayer { alpha = entryAlpha }
            )
            val currentLocale = LocaleHelper.getCurrentLocale()
            val langName = currentLocale.displayLanguage.replaceFirstChar { it.titlecase() }
            FilledTonalButton(
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                onClick = { showLanguageDialog.value = true },
                modifier = Modifier.graphicsLayer { alpha = entryAlpha }
            ) {
                Icon(painterResource(R.drawable.ic_public_24dp), null)
                Spacer(modifier = Modifier.size(8.dp))
                Text(text = langName)
            }
            Spacer(modifier = Modifier.height(48.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentAlignment = Alignment.Center
            ) {
                MorphingStartButton(
                    sharedTransitionScope = this@WelcomeScreen,
                    animatedVisibilityScope = animatedVisibilityScope,
                    isLargeScreen = isLargeScreen,
                    isTransitioning = isTransitioning,
                    isLaunched = isLaunched,
                    onClick = onGetStartedClick
                )
            }
        }
        if (showLanguageDialog.value) {
            LanguagePickerDialog(
                supportedLocales = supportedLocales,
                selectedLocale = LocaleHelper.getCurrentLocale(),
                onLanguageSelected = {
                    onLanguageSelected(it)
                    showLanguageDialog.value = false
                },
                onConfirm = { showLanguageDialog.value = false },
                onDismiss = { showLanguageDialog.value = false }
            )
        }
    }
}
fun parseSupportedLocales(context: Context): List<Locale> {
    val locales = mutableListOf<Locale>()
    try {
        val parser = context.resources.getXml(R.xml.locales_config)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                val langTag = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                if (langTag != null) {
                    locales.add(Locale.forLanguageTag(langTag))
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        Log.e("LocaleParser", "Error parsing locales_config", e)
        return listOf(Locale.ENGLISH)
    }
    val current = LocaleHelper.getCurrentLocale()
    return locales.sortedWith(compareByDescending<Locale> { it.language == current.language }
        .thenByDescending { it.language == "en" })
}
@ExperimentalTextApi
@Composable
fun MorphingStartButton(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isLargeScreen: Boolean,
    isTransitioning: Boolean,
    isLaunched: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val targetWeight = when {
        isTransitioning -> 1000f
        isPressed -> 100f
        else -> 700f
    }
    val animatedWeight by animateFloatAsState(
        targetValue = targetWeight,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "fontWeight"
    )
    val targetWidth = if (isTransitioning) 150f else 100f
    val animatedWidth by animateFloatAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "fontWidth"
    )
    val buttonWidthFraction by animateFloatAsState(
        targetValue = if (!isLaunched) 0f else if (isTransitioning && isLargeScreen) 0.15f else 1f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
        label = "buttonWidth"
    )
    val targetCorner = if (isTransitioning) 100.dp else if (isPressed) 16.dp else 32.dp
    val animatedCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = tween(durationMillis = 400),
        label = "corner"
    )
    val flexFontFamily = remember(animatedWeight, animatedWidth) {
        FontFamily(
            Font(
                resId = R.font.google_sans_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(animatedWeight.toInt().coerceIn(1, 1000)),
                    FontVariation.width(animatedWidth)
                )
            )
        )
    }
    val animatedColor by animateColorAsState(
        targetValue = if (isTransitioning) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 400),
        label = "buttonColor"
    )
    val animatedContentColor by animateColorAsState(
        targetValue = if (isTransitioning) Color.Transparent else MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(durationMillis = 300),
        label = "buttonContentColor"
    )
    with(sharedTransitionScope) {
        Button(
            onClick = {
                if (!isTransitioning) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            },
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth(buttonWidthFraction)
                .height(64.dp)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "setup_container"),
                    animatedVisibilityScope = animatedVisibilityScope
                ),
            shape = RoundedCornerShape(animatedCorner),
            colors = ButtonDefaults.buttonColors(
                containerColor = animatedColor,
                contentColor = animatedContentColor
            )
        ) {
            Text(
                text = stringResource(R.string.lets_go).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = flexFontFamily,
                    fontSize = 24.sp
                ),
                modifier = Modifier.graphicsLayer { alpha = if (isTransitioning) 0f else 1f }
            )
        }
    }
}
