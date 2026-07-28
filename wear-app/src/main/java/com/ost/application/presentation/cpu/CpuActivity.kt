package com.ost.application.presentation.cpu
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import com.ost.application.R
import com.ost.application.core.cpu.CpuCoreInfo
import com.ost.application.core.cpu.CpuInfoProvider
import com.ost.application.core.cpu.CpuStaticInfo
import com.ost.application.theme.OSTToolsTheme
import com.ost.application.util.CardPosition
import com.ost.application.util.InfoListScreenContent
import com.ost.application.util.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.settings.WearTemperatureUnitRepository
import com.ost.application.settings.WearTimingSettingsRepository

class WearCpuViewModel(application: Application) : AndroidViewModel(application) {
    private val timingRepository = WearTimingSettingsRepository(application, viewModelScope)
    private val temperatureRepository = WearTemperatureUnitRepository(application, viewModelScope, timingRepository.syncState)
    val temperatureUnit: StateFlow<TemperatureUnit> = temperatureRepository.unit
    private val _staticInfo = MutableStateFlow<CpuStaticInfo?>(null)
    val staticInfo: StateFlow<CpuStaticInfo?> = _staticInfo.asStateFlow()
    val cores: StateFlow<List<CpuCoreInfo>> = CpuInfoProvider.observeCores(intervalMs = 1000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    init {
        viewModelScope.launch(Dispatchers.IO) {
            _staticInfo.value = CpuInfoProvider.getStaticInfo(getApplication())
        }
    }
}
class CpuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OSTToolsTheme {
                CpuScreen()
            }
        }
    }
}
@Composable
fun CpuScreen(viewModel: WearCpuViewModel = viewModel()) {
    val listState = rememberScalingLazyListState()
    val staticInfo by viewModel.staticInfo.collectAsStateWithLifecycle()
    val cores by viewModel.cores.collectAsStateWithLifecycle()
    val tempUnit by viewModel.temperatureUnit.collectAsStateWithLifecycle()
    val notAvailable = stringResource(R.string.not_available)
    val staticItems = staticInfo?.let { info ->
        buildList {
            add(ListItem(stringResource(R.string.cpu_cores), info.coreCount.toString(), null, true, CardPosition.TOP, null))
            add(ListItem(stringResource(R.string.cpu_clusters), formatClusters(info), null, true, CardPosition.MIDDLE, null))
            add(ListItem(stringResource(R.string.cpu_abis), info.abis, null, true, CardPosition.MIDDLE, null))
            info.tempCelsius?.let { temp ->
                add(
                    ListItem(
                        stringResource(R.string.temperature),
                        com.ost.application.core.settings.formatTemperatureFloat(temp, tempUnit),
                        null, true, CardPosition.MIDDLE, null
                    )
                )
            }
            add(ListItem(stringResource(R.string.cpu_governor), info.governor ?: notAvailable, null, true, CardPosition.MIDDLE, null))
            add(ListItem(stringResource(R.string.gles_version), info.glEsVersion ?: notAvailable, null, true, CardPosition.MIDDLE, null))
            add(
                ListItem(
                    stringResource(R.string.vulkan_version), info.vulkanVersion ?: notAvailable, null, true,
                    if (cores.isEmpty()) CardPosition.BOTTOM else CardPosition.MIDDLE, null
                )
            )
        }
    } ?: emptyList()
    val coreItems = cores.mapIndexed { index, core ->
        ListItem(
            "CPU${core.index}",
            core.curFreqKhz?.let { "${it / 1000} MHz" } ?: notAvailable,
            null, true,
            if (index == cores.lastIndex) CardPosition.BOTTOM else CardPosition.MIDDLE,
            null
        )
    }
    AppScaffold(timeText = { TimeText() }) {
        ScreenScaffold(
            scrollState = listState,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            InfoListScreenContent(
                listState = listState,
                screenTitle = staticInfo?.socName ?: stringResource(R.string.cpu),
                icon = R.drawable.ic_developer_board_24dp,
                items = staticItems + coreItems
            )
        }
    }
}
private fun formatClusters(info: CpuStaticInfo): String {
    return info.clusters.joinToString(" + ") { cluster ->
        val ghz = cluster.maxFreqKhz?.let {
            String.format(Locale.US, "%.2f GHz", it / 1_000_000.0)
        } ?: "?"
        "${cluster.coreIndices.size} × $ghz"
    }
}
