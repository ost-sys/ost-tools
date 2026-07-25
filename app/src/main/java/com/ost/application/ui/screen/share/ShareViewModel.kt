package com.ost.application.ui.screen.share
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ost.application.R
import com.ost.application.core.share.Constants
import com.ost.application.core.share.DiscoveredDevice
import com.ost.application.core.share.IncomingTransferRequest
import com.ost.application.core.share.ServiceOverallStatus
import com.ost.application.core.share.ReceivedFilesLedger
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
sealed class UiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEvent()
}
class ShareViewModel(application: Application) : AndroidViewModel(application) {
    private var shareService: ShareService? = null
    private val _isServiceRunning   = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    private val _isServiceBound     = MutableStateFlow(false)
    private val _isReceivingActive  = MutableStateFlow(false)
    val isReceivingActive: StateFlow<Boolean> = _isReceivingActive.asStateFlow()
    private val _isDiscovering      = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    private val _statusText         = MutableStateFlow(application.getString(R.string.idle_status))
    val statusText: StateFlow<String> = _statusText.asStateFlow()
    private val _transferProgress   = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()
    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()
    private val _discoveredDevices  = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()
    private val _lastReceivedFiles  = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()
    private val _incomingTransferRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingTransferRequest: StateFlow<IncomingTransferRequest?> = _incomingTransferRequest.asStateFlow()
    private val _isCleaningUp       = MutableStateFlow(false)
    val isCleaningUp: StateFlow<Boolean> = _isCleaningUp.asStateFlow()
    private val _isServiceStuck     = MutableStateFlow(false)
    val isServiceStuck: StateFlow<Boolean> = _isServiceStuck.asStateFlow()
    val isTransferActive: StateFlow<Boolean> = _transferProgress
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val _stagedUris = MutableStateFlow<List<Uri>>(emptyList())
    val stagedUris: StateFlow<List<Uri>> = _stagedUris.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()
    private var selectedDevice: DiscoveredDevice? = null
    private var observationJobs: List<Job> = emptyList()
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ShareService.ServiceBinder
            shareService = binder.getService()
            _isServiceBound.value = true
            observationJobs.forEach { it.cancel() }
            observeServiceState()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            shareService = null
            _isServiceBound.value = false
            _isServiceRunning.value = false
            observationJobs.forEach { it.cancel() }
            observationJobs = emptyList()
            resetStateToDisconnected()
        }
    }
    init {
        viewModelScope.launch {
            _isCleaningUp.collect { cleaning ->
                if (cleaning) {
                    delay(8_000)
                    if (_isCleaningUp.value) _isServiceStuck.value = true
                } else {
                    _isServiceStuck.value = false
                }
            }
        }
        loadReceivedFiles()
        bindToService()
    }
    
    fun loadReceivedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fallbackDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val baseDir = publicDownloads ?: fallbackDir ?: return@launch
            val ostDir = File(baseDir, Constants.FILES_DIR)
            
            val validEntries = ReceivedFilesLedger.getValidEntries(ostDir)
            val files = validEntries.mapNotNull {
                val f = File(ostDir, it.fileName)
                if (f.exists()) f else null
            }
            _lastReceivedFiles.value = files
        }
    }
    
    fun deleteReceivedFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.exists()) {
                file.delete()
            }
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fallbackDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val baseDir = publicDownloads ?: fallbackDir ?: return@launch
            val ostDir = File(baseDir, Constants.FILES_DIR)
            ReceivedFilesLedger.removeEntry(ostDir, file.name)
            loadReceivedFiles()
        }
    }
    private fun bindToService() {
        Intent(getApplication(), ShareService::class.java).also { intent ->
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }
    private fun observeServiceState() {
        shareService?.let { service ->
            observationJobs = listOf(
                viewModelScope.launch {
                    service.isServiceActive.collect { _isServiceRunning.value = it }
                },
                viewModelScope.launch {
                    service.serviceStatus.collect { overallStatus ->
                        _statusText.value = overallStatus.statusText
                    }
                },
                viewModelScope.launch {
                    service.isReceivingActive.collect { _isReceivingActive.value = it }
                },
                viewModelScope.launch {
                    service.isDiscovering.collect { _isDiscovering.value = it }
                },
                viewModelScope.launch {
                    service.transferProgress.collect { _transferProgress.value = it }
                },
                viewModelScope.launch {
                    service.transferFileStatus.collect { _transferFileStatus.value = it }
                },
                viewModelScope.launch {
                    service.discoveredDevices.collect { _discoveredDevices.value = it }
                },
                viewModelScope.launch {
                    service.lastReceivedFiles.collect { _lastReceivedFiles.value = it }
                },
                viewModelScope.launch {
                    service.incomingTransferRequestService.collect { _incomingTransferRequest.value = it }
                },
                viewModelScope.launch {
                    service.isCleaningUp.collect { _isCleaningUp.value = it }
                }
            )
        }
    }
    private fun resetStateToDisconnected() {
        _isServiceRunning.value = false
        _isReceivingActive.value = false
        _isDiscovering.value = false
        _statusText.value = getApplication<Application>().getString(R.string.idle_status)
        _transferProgress.value = null
        _transferFileStatus.value = null
        _discoveredDevices.value = emptyList()
        _incomingTransferRequest.value = null
        _isCleaningUp.value = false
        _isServiceStuck.value = false
    }
    fun handlePermissionsGranted() {
    }
    fun setReceivingActive(active: Boolean) {
        val actionToPerform = if (active) Constants.ACTION_START_RECEIVING else Constants.ACTION_STOP_RECEIVING
        sendActionToService(actionToPerform)
    }
    fun startService() {
        sendActionToService(Constants.ACTION_LAUNCH_SERVICE)
    }
    fun stopService() {
        sendActionToService(Constants.ACTION_STOP_SERVICE)
    }
    fun restartService() {
        stopService()
        viewModelScope.launch {
            delay(300)
            startService()
        }
    }
    fun startDiscovery() {
        toggleDiscovery(true)
    }
    fun stopDiscovery() {
        toggleDiscovery(false)
    }
    fun setSelectedDevice(device: DiscoveredDevice) {
        selectedDevice = device
    }
    fun addStagedUris(uris: List<Uri>) {
        _stagedUris.value = (_stagedUris.value + uris).distinct()
    }
    fun removeStagedUri(uri: Uri) {
        _stagedUris.value = _stagedUris.value.filter { it != uri }
    }
    fun clearStagedUris() {
        _stagedUris.value = emptyList()
    }
    fun clearSelectedDevice() {
        selectedDevice = null
    }
    fun acceptIncomingTransfer(requestId: String) {
        respondToIncomingTransfer(requestId, true)
    }
    fun rejectIncomingTransfer(requestId: String) {
        respondToIncomingTransfer(requestId, false)
    }
    fun toggleDiscovery(active: Boolean) {
        val actionToPerform = if (active) Constants.ACTION_START_DISCOVERY else Constants.ACTION_STOP_DISCOVERY
        sendActionToService(actionToPerform)
    }
    fun prepareToSendToDevice(device: DiscoveredDevice) {
        selectedDevice = device
    }
    fun sendFilesToSelectedDevice(uris: List<Uri>) {
        val target = selectedDevice
        if (target == null) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.error_no_device_selected), isError = true))
            }
            return
        }
        if (uris.isEmpty()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowSnackbar(getApplication<Application>().getString(R.string.no_files_selected), isError = true))
            }
            return
        }
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_SEND_FILES
            putParcelableArrayListExtra(Constants.EXTRA_FILE_URIS, ArrayList(uris))
            putExtra(Constants.EXTRA_TARGET_DEVICE, target)
        }
        try {
            getApplication<Application>().startForegroundService(intent)
        } catch (e: Exception) {
            getApplication<Application>().startService(intent)
        }
    }
    fun cancelTransfer() {
        sendActionToService(Constants.ACTION_CANCEL_TRANSFER)
    }
    fun respondToIncomingTransfer(requestId: String, accept: Boolean) {
        val actionToPerform = if (accept) Constants.ACTION_ACCEPT_RECEIVE else Constants.ACTION_REJECT_RECEIVE
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = actionToPerform
            putExtra(Constants.EXTRA_REQUEST_ID, requestId)
        }
        try {
            getApplication<Application>().startForegroundService(intent)
        } catch (e: Exception) {
            getApplication<Application>().startService(intent)
        }
    }
    fun forceStopService() {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            action = Constants.ACTION_SHUTDOWN_SERVICE
        }
        try { getApplication<Application>().startService(intent) } catch (_: Exception) {}
        viewModelScope.launch {
            delay(1000)
            if (_isServiceBound.value) {
                runCatching { getApplication<Application>().unbindService(serviceConnection) }
                resetStateToDisconnected()
            }
        }
    }
    private fun sendActionToService(action: String) {
        val intent = Intent(getApplication(), ShareService::class.java).apply {
            this.action = action
        }
        try {
            getApplication<Application>().startForegroundService(intent)
        } catch (e: Exception) {
            getApplication<Application>().startService(intent)
        }
    }
    override fun onCleared() {
        super.onCleared()
        if (_isServiceBound.value) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
            }
            _isServiceBound.value = false
        }
        observationJobs.forEach { it.cancel() }
    }
}