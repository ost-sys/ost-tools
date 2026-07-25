package com.ost.application.core.device
import android.content.Context
import android.os.Build
interface DeviceRepository {
    fun getAndroidVersion(): String
    fun getBrand(): String
    fun getBoard(): String
    fun getBuildNumber(): String
    fun getSdkInt(): String
    fun getBuildFingerprint(): String
    fun getDeviceModel(): String
    fun getCodename(): String
    fun getDeviceFormFactor(): DeviceFormFactor
}
class DeviceRepositoryImpl(private val context: Context) : DeviceRepository {
    override fun getAndroidVersion(): String {
        return "${Build.VERSION.RELEASE} (${getLatestCodename()})"
    }
    override fun getBrand(): String = Build.BRAND
    override fun getBoard(): String = Build.BOARD
    override fun getSdkInt(): String = Build.VERSION.SDK_INT.toString()
    override fun getBuildFingerprint(): String = Build.FINGERPRINT
    override fun getBuildNumber(): String {
        return getSystemProperty("ro.system.build.id")
            ?: getSystemProperty("ro.build.display.id")
            ?: Build.DISPLAY
    }
    override fun getDeviceModel(): String {
        return getSystemProperty("ro.product.system.model")
            ?: getSystemProperty("ro.product.model")
            ?: Build.MODEL
    }
    override fun getCodename(): String = Build.DEVICE
    override fun getDeviceFormFactor(): DeviceFormFactor {
        val deviceChar = getSystemProperty("ro.build.characteristics") ?: "unknown"
        return when (deviceChar) {
            "phone" -> DeviceFormFactor.PHONE
            "tablet" -> DeviceFormFactor.TABLET
            else -> DeviceFormFactor.UNKNOWN
        }
    }
    private fun getLatestCodename(): String {
        val allCodenames = getSystemProperty("ro.build.version.known_codenames") ?: ""
        if (allCodenames.isEmpty()) return "Unknown"
        return allCodenames.substringAfterLast(",").trim()
    }
    private fun getSystemProperty(key: String): String? {
        return try {
            Class.forName("android.os.SystemProperties")
                .getMethod("get", String::class.java)
                .invoke(null, key) as? String
        } catch (e: Exception) {
            null
        }
    }
}