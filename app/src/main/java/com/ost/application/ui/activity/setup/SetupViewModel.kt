package com.ost.application.ui.activity.setup
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.ui.screen.settings.SettingsUiState
import com.topjohnwu.superuser.Shell
import com.ost.application.core.settings.TemperatureUnit
import com.ost.application.settings.GithubTokenRepository
import com.ost.application.settings.PhoneTemperatureUnitRepository
import com.ost.application.settings.PhoneTimingSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ost.application.core.locale.LocaleHelper
import com.ost.application.core.locale.SupportedLocalesLoader
import com.ost.application.core.settings.sync.SettingsSyncClient
class SetupViewModel(application: Application) : AndroidViewModel(application) {
    private val timingRepository = PhoneTimingSettingsRepository(application, viewModelScope)
    private val temperatureRepository = PhoneTemperatureUnitRepository(application, viewModelScope)
    private val tokenRepository = GithubTokenRepository(application, viewModelScope)
    private val syncClient = SettingsSyncClient(application)
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val _isRootGranted = MutableStateFlow(false)
    val isRootGranted: StateFlow<Boolean> = _isRootGranted.asStateFlow()
    init {
        _uiState.update { it.copy(githubToken = tokenRepository.token.value) }
        loadSupportedLocales()
        checkInitialRootStatus()
        viewModelScope.launch {
            timingRepository.settings.collect { timing ->
                _uiState.update {
                    it.copy(
                        totalDuration = timing.totalDuration,
                        noiseDuration = timing.noiseDuration,
                        blackWhiteNoiseDuration = timing.blackWhiteNoiseDuration,
                        horizontalDuration = timing.horizontalDuration,
                        verticalDuration = timing.verticalDuration
                    )
                }
            }
        }
        viewModelScope.launch {
            temperatureRepository.unit.collect { unit ->
                _uiState.update { it.copy(temperatureUnit = unit) }
            }
        }
    }
    fun updateTemperatureUnit(unit: TemperatureUnit) {
        temperatureRepository.updateUnit(unit)
    }
    private fun checkInitialRootStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            val isRoot = Shell.isAppGrantedRoot() == true
            _isRootGranted.update { isRoot }
        }
    }
    fun requestRootAccess() {
        viewModelScope.launch(Dispatchers.IO) {
            val isRoot = Shell.getShell().isRoot
            _isRootGranted.update { isRoot }
        }
    }
    private fun loadSupportedLocales() {
        val locales = SupportedLocalesLoader.load(getApplication(), R.xml.locales_config)
        _uiState.update { it.copy(supportedLocales = locales) }
    }
    fun updateTotalDuration(newValue: Int) = timingRepository.updateTotalDuration(newValue)
    fun updateNoiseDuration(newValue: Int) = timingRepository.updateNoiseDuration(newValue)
    fun updateBlackWhiteNoiseDuration(newValue: Int) = timingRepository.updateBlackWhiteNoiseDuration(newValue)
    fun updateHorizontalDuration(newValue: Int) = timingRepository.updateHorizontalDuration(newValue)
    fun updateVerticalDuration(newValue: Int) = timingRepository.updateVerticalDuration(newValue)
    fun onLanguagePreferenceClick() {
        _uiState.update {
            it.copy(
                isLanguageDialogVisible = true,
                selectedLanguageInDialog = it.currentAppliedLocale
            )
        }
    }
    fun onLanguageSelectedInDialog(locale: java.util.Locale?) {
        _uiState.update { it.copy(selectedLanguageInDialog = locale) }
    }
    fun onLanguageDialogDismiss() {
        _uiState.update { it.copy(isLanguageDialogVisible = false) }
    }
    fun onLanguageDialogConfirm() {
        val selectedLocale = _uiState.value.selectedLanguageInDialog
        LocaleHelper.setLocale(selectedLocale)
        _uiState.update {
            it.copy(
                isLanguageDialogVisible = false,
                currentAppliedLocale = selectedLocale ?: LocaleHelper.getSystemLocale()
            )
        }
        viewModelScope.launch {
            syncClient.pushLanguageTag(LocaleHelper.getCurrentLanguageTag())
        }
    }
    fun updateGithubToken(token: String) {
        _uiState.update { it.copy(githubToken = token) }
    }
    fun saveAllSettings() {
        tokenRepository.setToken(_uiState.value.githubToken)
    }
}
