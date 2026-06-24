package com.ost.application.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardListItem
import com.ost.application.util.CardPosition
import com.ost.application.util.startActivity

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                SettingsScreen()
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()

    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(scrollState = listState) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                state = listState,
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item {
                    CardListItem(
                        title = "Language",
                        summary = "Coming soon",
                        icon = R.drawable.ic_language_24dp,
                        status = true,
                        position = CardPosition.TOP,
                        onClick = { /* nothing for now */ }
                    )
                }

                item {
                    CardListItem(
                        title = "Check Updates",
                        summary = null,
                        icon = R.drawable.ic_update_24dp,
                        status = true,
                        position = CardPosition.MIDDLE,
                        onClick = { startActivity(context, UpdateActivity::class.java) }
                    )
                }

                item {
                    CardListItem(
                        title = "About",
                        summary = null,
                        icon = R.drawable.ic_info_24dp,
                        status = true,
                        position = CardPosition.BOTTOM,
                        onClick = { startActivity(context, AboutActivity::class.java) }
                    )
                }
            }
        }
    }
}