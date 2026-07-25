package com.ost.application.core.share
import android.net.nsd.NsdServiceInfo
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.net.InetAddress

@Parcelize
data class DiscoveredDevice(
    val id: String,
    var name: String,
    var type: String = Constants.VALUE_DEVICE_UNKNOWN,
    var isResolved: Boolean = false,
    var ipAddress: InetAddress? = null,
    var port: Int = -1,
    var isResolving: Boolean = false
) : Parcelable {
    @IgnoredOnParcel
    var serviceInfo: NsdServiceInfo? = null

    constructor(serviceInfo: NsdServiceInfo) : this(
        id = serviceInfo.serviceName,
        name = extractDeviceName(serviceInfo.serviceName),
        type = serviceInfo.attributes?.get(Constants.KEY_DEVICE_TYPE)?.let { String(it, Charsets.UTF_8) } ?: Constants.VALUE_DEVICE_UNKNOWN,
        isResolved = (serviceInfo.host != null && serviceInfo.port > 0),
        ipAddress = serviceInfo.host,
        port = serviceInfo.port,
        isResolving = false
    ) {
        this.serviceInfo = serviceInfo
    }
    val host: String? get() = if (isResolved) ipAddress?.hostAddress else null
    val deviceType: String get() = type
    companion object {
        fun extractDeviceName(serviceName: String): String {
            return serviceName
                .removePrefix(Constants.SERVICE_NAME_PREFIX)
                .substringBeforeLast('_')
                .replace("_", " ")
                .trim()
                .ifBlank { serviceName }
        }
    }
}
