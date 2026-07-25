package com.ost.application.core.share
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import java.io.File
data class FileTransferInfo(
    val uri: Uri?,
    val name: String,
    val size: Long
)
data class IncomingTransferRequest(
    val requestId: String,
    val senderDeviceName: String,
    val fileNames: List<String>,
    val totalSize: Long,
    val deferredConfirmation: CompletableDeferred<Boolean>
)
data class UiConfirmationState(
    val isSuccess: Boolean,
    val message: String,
    val iconRes: Int
)
interface TransferNotificationListener {
    fun onPreparing() {}
    fun onConnecting() {}
    fun onWaitingForAcceptance() {}
    fun onTransferring(progress: Int, fileUris: List<Uri>, totalSize: Long, isSending: Boolean) {}
    fun onCompleted(fileUris: List<Uri>, isSending: Boolean) {}
    fun onCancelled(reason: String?) {}
    fun onFailed(reason: String?) {}
    fun onShowIncomingRequest(requestId: String, senderName: String, fileNames: List<String>, totalSize: Long) {}
    fun onCancelNotification(notificationId: Int) {}
}
sealed class NsdRegistrationStatus {
    object Idle : NsdRegistrationStatus()
    object Registering : NsdRegistrationStatus()
    data class Registered(val serviceName: String) : NsdRegistrationStatus()
    data class Failed(val errorMessage: String) : NsdRegistrationStatus()
}
sealed class NsdDiscoveryStatus {
    object Idle : NsdDiscoveryStatus()
    object Discovering : NsdDiscoveryStatus()
    data class Failed(val errorMessage: String) : NsdDiscoveryStatus()
}
sealed class ServiceOverallStatus(val statusText: String) {
    class Idle(statusText: String) : ServiceOverallStatus(statusText)
    class Discovering(statusText: String) : ServiceOverallStatus(statusText)
    class Receiving(statusText: String) : ServiceOverallStatus(statusText)
    class Sending(statusText: String) : ServiceOverallStatus(statusText)
    class ReceivingRequest(statusText: String) : ServiceOverallStatus(statusText)
    class CleaningUp(statusText: String) : ServiceOverallStatus(statusText)
}
