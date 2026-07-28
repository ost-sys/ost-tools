package com.ost.application.presentation.memory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ost.application.R
import com.ost.application.core.memory.RamInfoProvider
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import java.util.Locale
class RamActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                RamScreen()
            }
        }
    }
}
@Composable
fun RamScreen() {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val provider = remember { RamInfoProvider(context) }
    val info = remember { provider.getRamInfo() }
    fun formatBytes(bytes: Long): String {
        val gb = bytes.toDouble() / (1024 * 1024 * 1024)
        return if (gb >= 1.0) {
            String.format(Locale.getDefault(), "%.2f GB", gb)
        } else {
            val mb = bytes.toDouble() / (1024 * 1024)
            String.format(Locale.getDefault(), "%.1f MB", mb)
        }
    }
    val items = remember(info) {
        listOf(
            ListItem(
                title = context.getString(R.string.used_ram),
                summary = "${formatBytes(info.usedBytes)} (${info.usedPercentage}%)",
                icon = com.ost.application.core.R.drawable.ic_memory_alt_24dp,
                status = true,
                position = CardPosition.TOP
            ),
            ListItem(
                title = context.getString(R.string.avail_ram),
                summary = formatBytes(info.availableBytes),
                icon = R.drawable.ic_check_circle_24dp,
                status = true,
                position = CardPosition.MIDDLE
            ),
            ListItem(
                title = context.getString(R.string.total_ram),
                summary = formatBytes(info.totalBytes),
                icon = com.ost.application.core.R.drawable.ic_memory_alt_24dp,
                status = true,
                position = CardPosition.BOTTOM
            )
        )
    }
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            InfoListScreenContent(
                listState = listState,
                screenTitle = stringResource(R.string.ram_title),
                icon = com.ost.application.core.R.drawable.ic_memory_alt_24dp,
                items = items
            )
        }
    }
}
