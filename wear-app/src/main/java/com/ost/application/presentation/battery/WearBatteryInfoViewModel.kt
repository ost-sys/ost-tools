package com.ost.application.presentation.battery
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.battery.BatteryDisplayMode
import com.ost.application.core.battery.BatteryInfoProvider
import com.ost.application.core.battery.BatteryInfoProvider.toChargingSourceText
import com.ost.application.core.battery.BatteryInfoProvider.toDisplayString
import com.ost.application.core.battery.BatteryInfoStrings
import com.ost.application.core.battery.BatteryStatusIcon
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
data class WearBatteryInfoUiState(
    val levelText: String = "...",
    val iconResId: Int = R.drawable.ic_battery_full_24dp,
    val health: String = "...",
    val status: String = "...",
    val temperature: String = "...",
    val voltage: String = "...",
    val technology: String = "...",
    val capacity: String = "...",
    val isLoadingCapacity: Boolean = true,
    val displayMode: BatteryDisplayMode = BatteryDisplayMode.NORMAL,
    val cycleCount: String = "..."
)
class WearBatteryInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(WearBatteryInfoUiState())
    val uiState: StateFlow<WearBatteryInfoUiState> = _uiState.asStateFlow()
    private var batteryUpdateJob: Job? = null
    private var capacityJob: Job? = null
    private val strings = BatteryInfoStrings(
        charging = getString(R.string.charging),
        chargingAc = getString(R.string.charging_ac),
        chargingUsb = getString(R.string.charging_via_usb),
        chargingWireless = getString(R.string.wireless_charging),
        discharging = getString(R.string.discharging),
        batteryLevel = getString(R.string.battery_level),
        good = getString(R.string.good),
        overheat = getString(R.string.overheat),
        dead = getString(R.string.dead),
        overVoltage = getString(R.string.over_voltage),
        unspecifiedFailure = getString(R.string.fail),
        cold = getString(R.string.cold),
        unknown = getString(R.string.unknown),
        mv = getString(R.string.mv),
        mah = getString(R.string.mah),
        cycleCount = getString(R.string.cycle_count),
        notAvailable = getString(R.string.not_available)
    )
    init {
        loadBatteryCapacity()
        startObservingBatteryUpdates()
    }
    private fun startObservingBatteryUpdates() {
        batteryUpdateJob?.cancel()
        batteryUpdateJob = BatteryInfoProvider.observeBatteryInfo(getApplication())
            .onEach { info ->
                _uiState.update {
                    it.copy(
                        levelText = if (info.levelPercent >= 0) {
                            if (info.isCharging) "${info.toChargingSourceText(strings)}: ${info.levelPercent}%"
                            else "${info.levelPercent}%"
                        } else "...",
                        iconResId = info.statusIcon.toResId(),
                        health = info.health.toDisplayString(strings),
                        status = info.toChargingSourceText(strings),
                        temperature = info.temperatureCelsius?.let {
                            String.format(Locale.getDefault(), "%.1f°C", it)
                        } ?: strings.unknown,
                        voltage = info.voltageVolts?.let {
                            String.format(Locale.getDefault(), "%.2fV", it)
                        } ?: strings.unknown,
                        technology = info.technology ?: strings.unknown,
                        displayMode = info.displayMode,
                        cycleCount = info.cycleCount?.toString() ?: getString(R.string.unknown),
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    private fun loadBatteryCapacity() {
        capacityJob?.cancel()
        capacityJob = viewModelScope.launch {
            val capacity = BatteryInfoProvider.getBatteryCapacityMah(getApplication())
            _uiState.update {
                it.copy(
                    capacity = if (capacity != null && capacity > 0) {
                        "${Math.round(capacity)} ${strings.mah}"
                    } else strings.unknown,
                    isLoadingCapacity = false
                )
            }
        }
    }
    private fun getString(resId: Int): String = getApplication<Application>().getString(resId)
    override fun onCleared() {
        super.onCleared()
        batteryUpdateJob?.cancel()
        capacityJob?.cancel()
    }
}
private fun BatteryStatusIcon.toResId(): Int = when (this) {
    BatteryStatusIcon.CHARGER -> R.drawable.ic_charger_24dp
    BatteryStatusIcon.POWER_SAVE -> R.drawable.ic_energy_program_saving_24dp
    BatteryStatusIcon.FULL -> R.drawable.ic_battery_full_alt_24dp
    BatteryStatusIcon.HORIZ_075 -> R.drawable.ic_battery_horiz_075_24dp
    BatteryStatusIcon.HORIZ_050 -> R.drawable.ic_battery_horiz_050_24dp
    BatteryStatusIcon.LOW -> R.drawable.ic_battery_low_24dp
    BatteryStatusIcon.VERY_LOW -> R.drawable.ic_battery_very_low_24dp
    BatteryStatusIcon.HORIZ_000 -> R.drawable.ic_battery_horiz_000_24dp
}