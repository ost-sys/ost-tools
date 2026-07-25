package com.ost.application.presentation.memory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ost.application.R
import com.ost.application.core.storage.StorageInfoProvider
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import java.util.Locale
class StorageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                StorageScreen()
            }
        }
    }
}
@Composable
fun StorageScreen() {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    val provider = remember { StorageInfoProvider() }
    val info = remember { provider.getStorageInfo() }
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
                title = context.getString(R.string.used_storage),
                summary = "${formatBytes(info.usedBytes)} (${info.usedPercentage}%)",
                icon = com.ost.application.core.R.drawable.ic_storage_24dp,
                status = true,
                position = CardPosition.TOP
            ),
            ListItem(
                title = context.getString(R.string.free_storage),
                summary = formatBytes(info.freeBytes),
                icon = R.drawable.ic_check_circle_24dp,
                status = true,
                position = CardPosition.MIDDLE
            ),
            ListItem(
                title = context.getString(R.string.total_storage),
                summary = formatBytes(info.totalBytes),
                icon = com.ost.application.core.R.drawable.ic_storage_24dp,
                status = true,
                position = CardPosition.BOTTOM
            )
        )
    }
    InfoListScreenContent(
        listState = listState,
        screenTitle = stringResource(R.string.storage_title),
        icon = com.ost.application.core.R.drawable.ic_storage_24dp,
        items = items
    )
}
