package com.ost.application.ui.screen.cpu
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.core.cpu.CpuCoreInfo
import com.ost.application.core.cpu.CpuInfoProvider
import com.ost.application.core.cpu.CpuStaticInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.settings.PhoneTemperatureUnitRepository

class CpuInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val temperatureRepository = PhoneTemperatureUnitRepository(application, viewModelScope)
    val temperatureUnit: StateFlow<TemperatureUnit> = temperatureRepository.unit
    private val _staticInfo = MutableStateFlow<CpuStaticInfo?>(null)
    val staticInfo: StateFlow<CpuStaticInfo?> = _staticInfo.asStateFlow()
    val cores: StateFlow<List<CpuCoreInfo>> = CpuInfoProvider.observeCores()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    init {
        viewModelScope.launch(Dispatchers.IO) {
            _staticInfo.value = CpuInfoProvider.getStaticInfo(getApplication())
        }
    }
}
