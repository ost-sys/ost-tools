package com.ost.application.presentation
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.theme.OSTToolsTheme
class ShortcutsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                ShortcutsScreen()
            }
        }
    }
}
private fun aliasComponent(context: Context, alias: String) =
    ComponentName(context.packageName, "${context.packageName}.$alias")
private fun isAliasEnabled(context: Context, alias: String): Boolean =
    context.packageManager.getComponentEnabledSetting(aliasComponent(context, alias)) ==
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
private fun setAliasEnabled(context: Context, alias: String, enabled: Boolean) {
    context.packageManager.setComponentEnabledSetting(
        aliasComponent(context, alias),
        if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )
}
@Composable
fun ShortcutsScreen() {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var explorerEnabled by remember { mutableStateOf(isAliasEnabled(context, "ExplorerShortcut")) }
    var musicEnabled by remember { mutableStateOf(isAliasEnabled(context, "MusicShortcut")) }
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                anchorType = ScalingLazyListAnchorType.ItemCenter
            ) {
                item { ListHeader { Text(stringResource(R.string.shortcuts)) } }
                item {
                    Text(
                        text = stringResource(R.string.shortcuts_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    SwitchButton(
                        checked = explorerEnabled,
                        onCheckedChange = { checked ->
                            explorerEnabled = checked
                            setAliasEnabled(context, "ExplorerShortcut", checked)
                        },
                        label = { Text(stringResource(R.string.file_explorer)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_folder_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    SwitchButton(
                        checked = musicEnabled,
                        onCheckedChange = { checked ->
                            musicEnabled = checked
                            setAliasEnabled(context, "MusicShortcut", checked)
                        },
                        label = { Text(stringResource(R.string.music)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_music_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
