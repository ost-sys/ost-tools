package com.ost.application.core.share
import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.ost.application.core.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
@SuppressLint("MissingPermission")
class FileReceiver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val notificationListener: TransferNotificationListener? = null,
    private val statusUpdateCallback: (String) -> Unit
) {
    private var serverSocketRef: ServerSocket? = null
    private var serverJob: Job? = null
    private val clientHandlerJobs = ConcurrentHashMap<String, Job>()
    private val clientSlotBusy = AtomicBoolean(false)
    private val incomingTransferConfirmations = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val _isReceivingActive = MutableStateFlow(false)
    val isReceivingActive: StateFlow<Boolean> = _isReceivingActive.asStateFlow()
    private val _transferProgress = MutableStateFlow<Int?>(null)
    val transferProgress: StateFlow<Int?> = _transferProgress.asStateFlow()
    private val _transferFileStatus = MutableStateFlow<String?>(null)
    val transferFileStatus: StateFlow<String?> = _transferFileStatus.asStateFlow()
    private val _lastReceivedFiles = MutableStateFlow<List<File>>(emptyList())
    val lastReceivedFiles: StateFlow<List<File>> = _lastReceivedFiles.asStateFlow()
    private val _incomingTransferRequestInternal = MutableStateFlow<IncomingTransferRequest?>(null)
    val incomingTransferRequest: StateFlow<IncomingTransferRequest?> = _incomingTransferRequestInternal.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            val dir = getPublicDownloadOstDir()
            if (dir != null) {
                val validEntries = ReceivedFilesLedger.getValidEntries(dir)
                val files = validEntries.mapNotNull {
                    val f = File(dir, it.fileName)
                    if (f.exists()) f else null
                }
                _lastReceivedFiles.value = files
            }
        }
    }
    fun startReceiving() {
        if (_isReceivingActive.value || serverJob?.isActive == true) {
            Log.d(Constants.TAG, "Receiver already active or job running. Exiting.")
            return
        }
        Log.d(Constants.TAG, "FileReceiver: startReceiving called.")
        _isReceivingActive.value = true
        _transferProgress.value = null
        _transferFileStatus.value = null
        statusUpdateCallback(context.getString(R.string.starting_receiver))
        val handler = CoroutineExceptionHandler { _, throwable ->
            Log.e(Constants.TAG, "FileReceiver server job encountered unhandled exception: ${throwable.message}", throwable)
            scope.launch(Dispatchers.Main) {
                statusUpdateCallback(context.getString(R.string.internal_server_error, throwable.message ?: "Unknown error"))
                _isReceivingActive.value = false
            }
            notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_SERVICE_FOREGROUND)
        }
        serverJob = scope.launch(handler) {
            try {
                try {
                    serverSocketRef = ServerSocket(Constants.TRANSFER_PORT).apply { reuseAddress = true }
                    Log.d(Constants.TAG, "FileReceiver: ServerSocket created on port ${Constants.TRANSFER_PORT}.")
                } catch (e: BindException) {
                    Log.e(Constants.TAG, "FileReceiver: Port ${Constants.TRANSFER_PORT} already in use — aborting.")
                    statusUpdateCallback(context.getString(R.string.error_port_in_use, e.message ?: "Unknown"))
                    _isReceivingActive.value = false
                    return@launch
                } catch (e: CancellationException) {
                    Log.d(Constants.TAG, "FileReceiver: serverJob cancelled during bind.")
                    return@launch
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "FileReceiver: Unexpected exception during ServerSocket creation: ${e.message}", e)
                    statusUpdateCallback(context.getString(R.string.fatal_server_error, e.message ?: "Unknown"))
                    _isReceivingActive.value = false
                    return@launch
                }
                if (serverSocketRef == null) {
                    Log.e(Constants.TAG, "FileReceiver: ServerSocket was not successfully created after all attempts.")
                    _isReceivingActive.value = false
                    return@launch
                }
                if (!isActive) {
                    Log.d(Constants.TAG, "FileReceiver: serverJob cancelled after socket creation, exiting.")
                    closeServerSocket()
                    return@launch
                }
                statusUpdateCallback(context.getString(R.string.receiver_active_waiting))
                Log.d(Constants.TAG, "FileReceiver: Server listening for incoming connections.")
                while (isActive && _isReceivingActive.value) {
                    try {
                        val clientSocket = serverSocketRef!!.accept()
                        if (!_isReceivingActive.value) {
                            clientSocket.close()
                            Log.d(Constants.TAG, "FileReceiver: Client connected, but receiver is stopping. Closing client socket.")
                            break
                        }
                        val clientId = clientSocket.remoteSocketAddress.toString()
                        clientHandlerJobs[clientId]?.cancel()
                        val handlerJob = launch { handleClient(clientSocket) }
                        clientHandlerJobs[clientId] = handlerJob
                        handlerJob.invokeOnCompletion { cause ->
                            clientHandlerJobs.remove(clientId)
                            Log.d(Constants.TAG, "FileReceiver: Client handler for $clientId completed or cancelled: $cause")
                        }
                        Log.d(Constants.TAG, "FileReceiver: Client connected: $clientId. Handled by new coroutine.")
                    } catch (e: SocketException) {
                        if (!isActive || serverSocketRef?.isClosed == true) {
                            Log.d(Constants.TAG, "FileReceiver: SocketException: Socket closed or job cancelled. Exiting accept loop. Message: ${e.message}")
                            break
                        }
                        Log.e(Constants.TAG, "FileReceiver: SocketException in server accept loop: ${e.message}", e)
                        delay(500)
                    } catch (e: IOException) {
                        if (!isActive) {
                            Log.d(Constants.TAG, "FileReceiver: IOException: Job cancelled. Exiting accept loop. Message: ${e.message}")
                            break
                        }
                        Log.e(Constants.TAG, "FileReceiver: IOException in server accept loop: ${e.message}", e)
                        delay(500)
                    }
                }
                Log.d(Constants.TAG, "FileReceiver: Server accept loop finished. isActive=$isActive, _isReceivingActive=${_isReceivingActive.value}")
            } finally {
                Log.d(Constants.TAG, "FileReceiver: serverJob finally block entered.")
                closeServerSocket()
                cancelAllClientHandlers()
                Log.d(Constants.TAG, "FileReceiver: serverJob finally block finished.")
            }
        }
    }
    fun stopReceiving() {
        Log.d(Constants.TAG, "FileReceiver: stopReceiving called.")
        if (!_isReceivingActive.value && serverJob?.isActive != true) {
            Log.d(Constants.TAG, "FileReceiver: Already in stopped state. Exiting.")
            return
        }
        _isReceivingActive.value = false
        statusUpdateCallback(context.getString(R.string.stopping_receiver))
        notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_SERVICE_FOREGROUND)
        notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_TRANSFER)
        notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_INCOMING_FILE)
        Log.d(Constants.TAG, "FileReceiver: UI state and notifications updated to stopping.")
        closeServerSocket()
        val jobToStop = serverJob
        serverJob = null
        scope.launch {
            Log.d(Constants.TAG, "FileReceiver: Starting cleanup job for stopReceiving.")
            jobToStop?.cancel(CancellationException("Cancelled by user"))
            jobToStop?.join()
            cancelAllClientHandlers()
            Log.d(Constants.TAG, "FileReceiver: Cleanup job finished.")
        }
    }
    fun respondToIncomingTransfer(requestId: String, accept: Boolean) {
        Log.d(Constants.TAG, "FileReceiver: respondToIncomingTransfer: requestId=$requestId, accept=$accept")
        incomingTransferConfirmations[requestId]?.complete(accept)
        notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_INCOMING_FILE)
        scope.launch(Dispatchers.Main.immediate) {
            if (_incomingTransferRequestInternal.value?.requestId == requestId) {
                _incomingTransferRequestInternal.value = null
            }
        }
    }
    fun cancelIncomingTransfer() {
        if (clientHandlerJobs.isEmpty()) {
            Log.d(Constants.TAG, "FileReceiver: No active incoming transfers to cancel.")
            return
        }
        Log.d(Constants.TAG, "FileReceiver: Cancelling all active incoming transfers.")
        val jobsToCancel = clientHandlerJobs.values.toList()
        jobsToCancel.forEach { job ->
            job.cancel(CancellationException("Cancelled by user"))
        }
        clientHandlerJobs.clear()
        scope.launch(Dispatchers.Main.immediate) {
            _transferProgress.value = null
            _transferFileStatus.value = null
            statusUpdateCallback(context.getString(R.string.cancelling_transfer))
        }
        notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_TRANSFER)
        notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_INCOMING_FILE)
    }
    private fun closeServerSocket() {
        serverSocketRef?.let { socket ->
            if (!socket.isClosed) {
                try {
                    socket.close()
                    Log.d(Constants.TAG, "FileReceiver: ServerSocket closed.")
                } catch (ex: IOException) {
                    Log.e(Constants.TAG, "FileReceiver: Error closing server socket: ${ex.message}")
                }
            } else {
                Log.d(Constants.TAG, "FileReceiver: ServerSocket was already closed by another call or prior logic.")
            }
        }
        serverSocketRef = null
    }
    private suspend fun cancelAllClientHandlers() {
        val jobsToCancel = clientHandlerJobs.values.toList()
        clientHandlerJobs.clear()
        jobsToCancel.forEach { it.cancel(CancellationException("Server stopping")) }
        delay(100)
    }
    private suspend fun handleClient(clientSocket: Socket) {
        if (!clientSlotBusy.compareAndSet(false, true)) {
            Log.d(Constants.TAG, "FileReceiver: Another transfer is pending or active. Rejecting ${clientSocket.remoteSocketAddress}.")
            runCatching {
                PrintWriter(OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8), true).println(Constants.CMD_REJECT)
            }
            runCatching { clientSocket.close() }
            return
        }
        var reader: BufferedReader? = null
        var writer: PrintWriter? = null
        var fileOutputStream: BufferedOutputStream? = null
        var socketInputStream: InputStream? = null
        val receivedFilesInfo = mutableListOf<FileTransferInfo>()
        val successfullySavedFiles = mutableListOf<File>()
        val successfullySavedUris = mutableListOf<Uri>()
        var currentFileReceiving: File? = null
        var totalExpectedSize: Long = 0
        var numberOfFilesExpected: Int = 0
        var requestId: String? = null
        var senderDeviceName: String = ""
        var confirmationDeferred: CompletableDeferred<Boolean>? = null
        try {
            clientSocket.soTimeout = 15000
            socketInputStream = clientSocket.getInputStream()
            reader = BufferedReader(InputStreamReader(socketInputStream, Charsets.UTF_8))
            writer = PrintWriter(OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8), true)
            val requestLine = withTimeoutOrNull(15000) { reader.readLine() }
                ?: throw IOException("Client disconnected or timeout.")
            val parts = requestLine.split(Constants.CMD_SEPARATOR)
            val fileUrisForNotification: List<Uri>
            when (parts.getOrNull(0)) {
                Constants.CMD_REQUEST_SEND_MULTI -> {
                    if (parts.size < 4) throw IllegalArgumentException("Invalid MULTI_REQUEST format: $requestLine")
                    senderDeviceName = parts[1].decodeFromURL()
                    numberOfFilesExpected = parts[2].toIntOrNull() ?: 0
                    totalExpectedSize = parts[3].toLongOrNull() ?: 0L
                    if (numberOfFilesExpected <= 0 || totalExpectedSize < 0) {
                        throw IllegalArgumentException("Invalid count ($numberOfFilesExpected) or size ($totalExpectedSize) in MULTI_REQUEST")
                    }
                    if (!_isReceivingActive.value) throw CancellationException("Receiver stopped before receiving multi meta")
                    scope.launch(Dispatchers.Main.immediate) {
                        _transferProgress.value = 0
                        _transferFileStatus.value = null
                        _lastReceivedFiles.value = emptyList()
                        statusUpdateCallback("Incoming transfer: $numberOfFilesExpected files ($totalExpectedSize bytes)")
                    }
                    repeat(numberOfFilesExpected) { index ->
                        val metaLine = withTimeoutOrNull(10000) { reader.readLine() }
                            ?: throw IOException("Timeout waiting for metadata for file ${index + 1}")
                        val metaParts = metaLine.split(Constants.CMD_SEPARATOR)
                        if (metaParts.size < 3 || metaParts[0] != Constants.FILE_META) {
                            throw IllegalArgumentException("Invalid FILE_META format: $metaLine")
                        }
                        val fileName = File(metaParts[1].decodeFromURL()).name
                        val fileSize = metaParts[2].toLongOrNull() ?: -1L
                        if (fileName.isBlank() || fileSize < 0) {
                            throw IllegalArgumentException("Invalid metadata: name='$fileName', size=$fileSize")
                        }
                        receivedFilesInfo.add(FileTransferInfo(uri = null, name = fileName, size = fileSize))
                    }
                    val calculatedTotalSize = receivedFilesInfo.sumOf { it.size }
                    if (calculatedTotalSize != totalExpectedSize) {
                        totalExpectedSize = calculatedTotalSize
                    }
                }
                Constants.CMD_REQUEST_SEND -> {
                    if (parts.size < 4) throw IllegalArgumentException("Invalid SEND_REQUEST format: $requestLine")
                    senderDeviceName = parts[1].decodeFromURL()
                    val fileName = File(parts[2].decodeFromURL()).name
                    val fileSize = parts[3].toLongOrNull() ?: 0L
                    if (fileName.isBlank() || fileSize <= 0) {
                        throw IllegalArgumentException("Invalid filename or size")
                    }
                    numberOfFilesExpected = 1
                    totalExpectedSize = fileSize
                    receivedFilesInfo.add(FileTransferInfo(uri = null, name = fileName, size = fileSize))
                    if (!_isReceivingActive.value) throw CancellationException("Receiver stopped before receiving single file")
                    scope.launch(Dispatchers.Main.immediate) {
                        _transferProgress.value = 0
                        _transferFileStatus.value = null
                        _lastReceivedFiles.value = emptyList()
                        statusUpdateCallback("Incoming transfer: $fileName")
                    }
                }
                else -> {
                    writer.println(Constants.CMD_REJECT)
                    throw IllegalArgumentException("Unknown command: $requestLine")
                }
            }
            fileUrisForNotification = receivedFilesInfo.map { Uri.parse("file://${it.name}") }
            notificationListener?.onWaitingForAcceptance()
            requestId = UUID.randomUUID().toString()
            confirmationDeferred = CompletableDeferred(parent = currentCoroutineContext().job)
            incomingTransferConfirmations[requestId] = confirmationDeferred
            scope.launch(Dispatchers.Main.immediate) {
                _incomingTransferRequestInternal.value = IncomingTransferRequest(
                    requestId = requestId,
                    senderDeviceName = senderDeviceName,
                    fileNames = receivedFilesInfo.map { it.name },
                    totalSize = totalExpectedSize,
                    deferredConfirmation = confirmationDeferred
                )
                statusUpdateCallback("Incoming request from $senderDeviceName")
            }
            Log.d(Constants.TAG, "FileReceiver: Incoming transfer request from $senderDeviceName, waiting for user confirmation.")
            notificationListener?.onShowIncomingRequest(requestId, senderDeviceName, receivedFilesInfo.map { it.name }, totalExpectedSize)
            val userAccepted = withTimeoutOrNull(Constants.INCOMING_REQUEST_RESPONSE_TIMEOUT_MILLIS) { confirmationDeferred.await() } ?: false
            if (!userAccepted) {
                Log.d(Constants.TAG, "FileReceiver: User rejected or timed out for request $requestId.")
                writer.println(Constants.CMD_REJECT)
                scope.launch(Dispatchers.Main.immediate) {
                    statusUpdateCallback(context.getString(R.string.transfer_rejected))
                    _transferProgress.value = null
                    _transferFileStatus.value = null
                }
                notificationListener?.onCancelled("Rejected by user")
                notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_INCOMING_FILE)
                return
            } else {
                Log.d(Constants.TAG, "FileReceiver: User accepted request $requestId.")
                writer.println(Constants.CMD_ACCEPT)
                notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_INCOMING_FILE)
                scope.launch(Dispatchers.Main.immediate) {
                    _transferProgress.value = 0
                    statusUpdateCallback("${Constants.RECEIVED_PREFIX}: receiving $numberOfFilesExpected files")
                }
                val receiveDir = getPublicDownloadOstDir()
                    ?: throw IOException("Target download directory error")
                clientSocket.soTimeout = 60000
                val buffer = ByteArray(16384)
                var lastOverallProgressUpdate = -1
                var totalBytesReceivedOverall: Long = 0
                for ((index, fileInfo) in receivedFilesInfo.withIndex()) {
                    if (!currentCoroutineContext().isActive) throw CancellationException("Receive cancelled before file ${index + 1}")
                    currentFileReceiving = ensureReceiveFile(receiveDir, fileInfo.name)
                        ?: throw IOException("Cannot create target file: ${fileInfo.name}")
                    val fileToProcess = currentFileReceiving
                    val currentFileStatusText = "Receiving file ${index + 1}/$numberOfFilesExpected: ${fileInfo.name}"
                    if (!_isReceivingActive.value) throw CancellationException("Receiver stopped before receiving file ${index + 1}")
                    scope.launch(Dispatchers.Main.immediate) {
                        _transferFileStatus.value = currentFileStatusText
                    }
                    try {
                        fileOutputStream = BufferedOutputStream(FileOutputStream(fileToProcess))
                    } catch (fosEx: Exception) {
                        scope.launch(Dispatchers.Main.immediate) { statusUpdateCallback("Error creating file ${fileToProcess.name}: ${fosEx.message}") }
                        fileToProcess.delete()
                        throw fosEx
                    }
                    var bytesReceivedForCurrentFile: Long = 0
                    var bytesReadCurrentLoop: Int
                    while (currentCoroutineContext().isActive && bytesReceivedForCurrentFile < fileInfo.size) {
                        try {
                            val bytesToRead = minOf(buffer.size.toLong(), fileInfo.size - bytesReceivedForCurrentFile).toInt()
                            bytesReadCurrentLoop = socketInputStream.read(buffer, 0, bytesToRead)
                        } catch (timeoutEx: SocketTimeoutException) {
                            if (!currentCoroutineContext().isActive || !_isReceivingActive.value) {
                                throw CancellationException("Receive cancelled during read timeout.")
                            } else {
                                throw timeoutEx
                            }
                        }
                        if (bytesReadCurrentLoop == -1) {
                            throw IOException("Client disconnected early during file ${fileInfo.name}")
                        }
                        if (bytesReadCurrentLoop > 0) {
                            try {
                                fileOutputStream.write(buffer, 0, bytesReadCurrentLoop)
                                bytesReceivedForCurrentFile += bytesReadCurrentLoop
                                totalBytesReceivedOverall += bytesReadCurrentLoop
                            } catch (writeEx: IOException) {
                                throw writeEx
                            }
                        }
                        val currentOverallProgress: Int = if (totalExpectedSize > 0) {
                            ((totalBytesReceivedOverall * 100) / totalExpectedSize).toInt()
                        } else 100
                        if (currentOverallProgress > lastOverallProgressUpdate) {
                            scope.launch(Dispatchers.Main.immediate) {
                                if (isActive && _isReceivingActive.value) _transferProgress.value = currentOverallProgress
                            }
                            notificationListener?.onTransferring(currentOverallProgress, fileUrisForNotification, totalExpectedSize, false)
                            lastOverallProgressUpdate = currentOverallProgress
                        }
                    }
                    try {
                        fileOutputStream.flush()
                        fileOutputStream.close()
                        fileOutputStream = null
                    } catch (ex: IOException) { Log.e(Constants.TAG, "FileReceiver: Error closing fileOutputStream: ${ex.message}") }
                    if (!currentCoroutineContext().isActive) throw CancellationException("Receive cancelled after file ${index + 1}")
                    if (bytesReceivedForCurrentFile == fileInfo.size) {
                        successfullySavedFiles.add(fileToProcess)
                        val fileUri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, fileToProcess)
                        successfullySavedUris.add(fileUri)
                        val filePath = fileToProcess.absolutePath
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(filePath),
                            null
                        ) { path, uri ->
                            Log.d("MediaScan", "Scanned $path -> $uri")
                        }
                        currentFileReceiving = null
                    } else {
                        fileToProcess.delete()
                        throw IOException("File size mismatch for ${fileInfo.name}")
                    }
                }
                finalizeSavedFiles(successfullySavedFiles.toList())
                scope.launch(Dispatchers.Main.immediate) {
                    _transferProgress.value = 100
                    _transferFileStatus.value = null
                    statusUpdateCallback("${Constants.RECEIVED_PREFIX}: $numberOfFilesExpected files")
                }
                delay(250)
                notificationListener?.onCompleted(successfullySavedUris, false)
            }
        } catch (e: CancellationException) {
            Log.w(Constants.TAG, "FileReceiver: handleClient cancelled: ${e.message}")
            scope.launch(Dispatchers.Main.immediate) {
                _transferProgress.value = null
                _transferFileStatus.value = null
                statusUpdateCallback("CANCELED: ${e.message}")
            }
            notificationListener?.onCancelled(e.message)
            notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_INCOMING_FILE)
            currentFileReceiving?.delete()
            finalizeSavedFiles(successfullySavedFiles.toList())
        } catch (e: Exception) {
            Log.e(Constants.TAG, "FileReceiver: Error in handleClient: ${e.message}", e)
            val errorMsg = e.message ?: e.javaClass.simpleName
            scope.launch(Dispatchers.Main.immediate) {
                _transferProgress.value = null
                _transferFileStatus.value = null
                statusUpdateCallback("ERROR: $errorMsg")
            }
            notificationListener?.onFailed(errorMsg)
            currentFileReceiving?.delete()
            finalizeSavedFiles(successfullySavedFiles.toList())
        } finally {
            clientSlotBusy.set(false)
            if (requestId != null) {
                incomingTransferConfirmations.remove(requestId)
            }
            scope.launch(Dispatchers.Main.immediate) {
                if (_incomingTransferRequestInternal.value?.requestId == requestId) {
                    _incomingTransferRequestInternal.value = null
                }
            }
            try { fileOutputStream?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "FileReceiver: Error closing fileOutputStream: ${ex.message}") }
            try { socketInputStream?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "FileReceiver: Error closing socketInputStream: ${ex.message}") }
            try { writer?.close() } catch (ex: Exception) { Log.e(Constants.TAG, "FileReceiver: Error closing writer: ${ex.message}") }
            try { reader?.close() } catch (ex: IOException) { Log.e(Constants.TAG, "FileReceiver: Error closing reader: ${ex.message}") }
            try { clientSocket.close() } catch (ex: IOException) { Log.e(Constants.TAG, "FileReceiver: Error closing clientSocket: ${ex.message}") }
            notificationListener?.onCancelNotification(Constants.NOTIFICATION_ID_INCOMING_FILE)
            scope.launch(Dispatchers.Main.immediate) {
                _transferProgress.value = null
                _transferFileStatus.value = null
            }
            Log.d(Constants.TAG, "FileReceiver: handleClient finally block finished.")
        }
    }
    private fun finalizeSavedFiles(savedFiles: List<File>) {
        if (savedFiles.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val dir = getPublicDownloadOstDir()
            if (dir != null) {
                savedFiles.forEach { ReceivedFilesLedger.addEntry(dir, it.name) }
                val validEntries = ReceivedFilesLedger.getValidEntries(dir)
                _lastReceivedFiles.value = validEntries.mapNotNull {
                    val f = File(dir, it.fileName)
                    if (f.exists()) f else null
                }
            } else {
                _lastReceivedFiles.value = savedFiles + _lastReceivedFiles.value
            }
        }
    }
    private fun getPublicDownloadOstDir(): File? {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val baseDir = publicDownloads ?: fallbackDir ?: run {
            statusUpdateCallback("No downloads directory found")
            return null
        }
        val ostDir = File(baseDir, Constants.FILES_DIR)
        return try {
            if (!ostDir.exists()) {
                if (!ostDir.mkdirs()) {
                    statusUpdateCallback("Cannot create directory: ${ostDir.path}")
                    null
                } else {
                    ostDir
                }
            } else if (!ostDir.isDirectory) {
                statusUpdateCallback("Path is not a directory: ${ostDir.path}")
                null
            } else if (!ostDir.canWrite()) {
                statusUpdateCallback("Directory is not writable: ${ostDir.path}")
                null
            } else {
                ostDir
            }
        } catch (e: SecurityException) {
            statusUpdateCallback("Security error accessing directory: ${e.message}")
            null
        } catch (e: Exception) {
            statusUpdateCallback("Error accessing directory: ${e.message}")
            null
        }
    }
    private fun ensureReceiveFile(targetDir: File, originalFileName: String): File? {
        val sanitizedFileName = originalFileName
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\p{Cntrl}"), "_")
            .trim()
            .take(240)
            .ifBlank { "file_${System.currentTimeMillis()}" }
        if (!targetDir.isDirectory || !targetDir.canWrite()) return null
        return try {
            var file = File(targetDir, sanitizedFileName)
            var counter = 0
            val baseName = file.nameWithoutExtension
            val extension = file.extension.let { if (it.isNotEmpty()) ".$it" else "" }
            while (file.exists() && counter < 999) {
                counter++
                val newName = "$baseName($counter)$extension"
                if (newName.length > 250) {
                    Log.e(Constants.TAG, "FileReceiver: Generated file name too long after counter: $newName")
                    return null
                }
                file = File(targetDir, newName)
            }
            if (counter >= 999) {
                Log.e(Constants.TAG, "FileReceiver: Exceeded max attempts to find unique file name for $originalFileName")
                return null
            }
            if (!file.exists()) {
                if (file.createNewFile()) {
                    file
                } else {
                    Log.e(Constants.TAG, "FileReceiver: Failed to create new file: ${file.path}")
                    null
                }
            } else {
                Log.e(Constants.TAG, "FileReceiver: Target file already exists and could not find unique name: ${file.path}")
                null
            }
        } catch (ex: IOException) {
            Log.e(Constants.TAG, "FileReceiver: Error creating new file: ${ex.message}", ex)
            null
        } catch (ex: SecurityException) {
            Log.e(Constants.TAG, "FileReceiver: Security error creating file: ${ex.message}", ex)
            null
        } catch (ex: Exception) {
            Log.e(Constants.TAG, "FileReceiver: Unexpected error creating file: ${ex.message}", ex)
            null
        }
    }
    fun shutdown() {
        Log.d(Constants.TAG, "FileReceiver: Shutting down FileReceiver.")
        stopReceiving()
    }
}
