package com.ost.application.presentation.powermenu
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.ost.application.R
import com.ost.application.core.service.OstAccessibilityService
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardListItem
import com.ost.application.util.CardPosition
class PowerMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                PowerMenuScreen()
            }
        }
    }
}
@Composable
fun PowerMenuScreen() {
    val context = LocalContext.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    if (showAccessibilityDialog) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text(stringResource(R.string.accessibility_required_title)) },
            text = { Text(stringResource(R.string.accessibility_required_msg)) },
            confirmButton = {
                Button(onClick = {
                    showAccessibilityDialog = false
                    OstAccessibilityService.openAccessibilitySettings(context)
                }) {
                    Text(stringResource(R.string.open_settings))
                }
            }
        )
    }
    AppScaffold {
        val listState = rememberScalingLazyListState()
        ScreenScaffold(scrollState = listState) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item { ListHeader { Text(stringResource(R.string.power_menu)) } }
                item {
                    CardListItem(
                        title = stringResource(R.string.turn_off),
                        summary = null,
                        icon = com.ost.application.core.R.drawable.ic_power_new_24dp,
                        status = true,
                        position = CardPosition.TOP,
                        onClick = {
                            val success = OstAccessibilityService.performPowerDialog()
                            if (!success) {
                                showAccessibilityDialog = true
                            }
                        }
                    )
                }
                item {
                    CardListItem(
                        title = stringResource(R.string.lock_screen),
                        summary = null,
                        icon = com.ost.application.core.R.drawable.ic_lock_24dp,
                        status = true,
                        position = CardPosition.BOTTOM,
                        onClick = {
                            val success = OstAccessibilityService.performLockScreen()
                            if (!success) {
                                showAccessibilityDialog = true
                            }
                        }
                    )
                }
            }
        }
    }
}
