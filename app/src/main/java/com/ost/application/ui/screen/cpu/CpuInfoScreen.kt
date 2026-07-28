@file:OptIn(ExperimentalMaterial3Api::class)
package com.ost.application.ui.screen.cpu
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ost.application.LocalBottomSpacing
import com.ost.application.R
import com.ost.application.core.cpu.CpuCoreInfo
import com.ost.application.core.cpu.CpuStaticInfo
import com.ost.application.ui.components.ExpressiveShapeBackground
import com.ost.application.util.CardPosition
import com.ost.application.util.CustomCardItem
import java.util.Locale
private data class CpuInfoRow(
    val icon: Int,
    val titleRes: Int,
    val summary: String?
)
@ExperimentalMaterial3ExpressiveApi
@Composable
fun CpuInfoScreen(
    modifier: Modifier = Modifier,
    viewModel: CpuInfoViewModel = viewModel()
) {
    val bottomSpacing = LocalBottomSpacing.current
    val staticInfo by viewModel.staticInfo.collectAsStateWithLifecycle()
    val cores by viewModel.cores.collectAsStateWithLifecycle()
    val tempUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()
    val notAvailable = stringResource(R.string.not_available)
    val infoRows = staticInfo?.let { info ->
        buildList {
            add(CpuInfoRow(R.drawable.ic_memory_alt_24dp, R.string.cpu_cores, info.coreCount.toString()))
            add(CpuInfoRow(R.drawable.ic_layers_24dp, R.string.cpu_clusters, formatClusters(info)))
            add(CpuInfoRow(R.drawable.ic_developer_board_24dp, R.string.cpu_abis, info.abis))
            add(
                CpuInfoRow(
                    R.drawable.ic_adb_24dp,
                    R.string.cpu_64bit_process,
                    stringResource(if (info.is64BitProcess) R.string.yes else R.string.no)
                )
            )
            info.tempCelsius?.let { temp ->
                add(
                    CpuInfoRow(
                        R.drawable.ic_offline_bolt_24dp,
                        R.string.temperature,
                        com.ost.application.core.settings.formatTemperatureFloat(temp, tempUnit)
                    )
                )
            }
            add(CpuInfoRow(R.drawable.ic_offline_bolt_24dp, R.string.cpu_governor, info.governor ?: notAvailable))
            add(CpuInfoRow(R.drawable.ic_screen_24dp, R.string.gles_version, info.glEsVersion ?: notAvailable))
            add(CpuInfoRow(R.drawable.ic_screen_24dp, R.string.vulkan_version, info.vulkanVersion ?: notAvailable))
        }
    } ?: emptyList()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + bottomSpacing),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 5.dp)
                ) {
                    ExpressiveShapeBackground(
                        iconSize = 120.dp,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        onClick = {}
                    )
                    Image(
                        painter = painterResource(R.drawable.ic_developer_board_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(8.dp)) {
                    Text(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        text = staticInfo?.socName ?: stringResource(R.string.cpu),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (cores.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        cores.forEach { core -> CoreFrequencyBar(core) }
                    }
                }
            }
        }
        itemsIndexed(infoRows) { index, item ->
            val position = when {
                infoRows.size == 1 -> CardPosition.SINGLE
                index == 0 -> CardPosition.TOP
                index == infoRows.lastIndex -> CardPosition.BOTTOM
                else -> CardPosition.MIDDLE
            }
            CustomCardItem(
                title = stringResource(item.titleRes),
                summary = item.summary,
                icon = item.icon,
                position = position
            )
        }
    }
}
@Composable
private fun CoreFrequencyBar(core: CpuCoreInfo) {
    val cur = core.curFreqKhz
    val max = core.maxFreqKhz
    val fraction = if (cur != null && max != null && max > 0) cur.toFloat() / max else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400),
        label = "coreFreq${core.index}"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "CPU${core.index}",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(48.dp)
        )
        LinearProgressIndicator(
            progress = { animatedFraction },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = formatFreq(cur),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.width(76.dp)
        )
    }
}
private fun formatFreq(khz: Long?): String {
    if (khz == null) return "—"
    return "${khz / 1000} MHz"
}
private fun formatClusters(info: CpuStaticInfo): String {
    return info.clusters.joinToString(" + ") { cluster ->
        val ghz = cluster.maxFreqKhz?.let {
            String.format(Locale.US, "%.2f GHz", it / 1_000_000.0)
        } ?: "?"
        "${cluster.coreIndices.size} × $ghz"
    }
}
