package com.ost.application.presentation
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.ost.application.component.WearOrbBackground
import com.ost.application.presentation.setup.SetupActivity
import com.ost.application.theme.OSTToolsTheme
import kotlinx.coroutines.delay
class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                WelcomeScreen(
                    onGetStartedClick = {
                        startActivity(Intent(this, SetupActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}
@Composable
private fun WelcomeScreen(onGetStartedClick: () -> Unit) {
    var showBackground by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showButton by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showBackground = true
        delay(600)
        showText = true
        delay(600)
        showButton = true
    }
    AppScaffold {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = listState,
            edgeButton = {
                AnimatedVisibility(
                    visible = showButton,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(durationMillis = 500, easing = EaseOutCubic)
                    ) + fadeIn(tween(500))
                ) {
                    EdgeButton(
                        onClick = onGetStartedClick,
                        buttonSize = EdgeButtonSize.Large
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Get started"
                        )
                    }
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                WearOrbBackground(animateEntrance = showBackground)
                ScalingLazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    anchorType = ScalingLazyListAnchorType.ItemCenter
                ) {
                    item {
                        AnimatedVisibility(
                            visible = showText,
                            enter = fadeIn(tween(700, easing = EaseOutCubic))
                        ) {
                            Text(
                                text = "Hello!",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}
