package com.ost.application.core.memory
import android.app.ActivityManager
import android.content.Context
data class RamInfo(
    val totalBytes: Long,
    val availableBytes: Long,
    val usedBytes: Long,
    val usedPercentage: Int,
    val isLowMemory: Boolean,
    val thresholdBytes: Long,
    val jvmMaxMemoryBytes: Long,
    val jvmTotalMemoryBytes: Long,
    val jvmFreeMemoryBytes: Long
)
class RamInfoProvider(private val context: Context) {
    fun getRamInfo(): RamInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalBytes = memoryInfo.totalMem
        val availableBytes = memoryInfo.availMem
        val usedBytes = (totalBytes - availableBytes).coerceAtLeast(0L)
        val usedPercentage = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0
        val runtime = Runtime.getRuntime()
        val jvmMaxMemoryBytes = runtime.maxMemory()
        val jvmTotalMemoryBytes = runtime.totalMemory()
        val jvmFreeMemoryBytes = runtime.freeMemory()
        return RamInfo(
            totalBytes = totalBytes,
            availableBytes = availableBytes,
            usedBytes = usedBytes,
            usedPercentage = usedPercentage,
            isLowMemory = memoryInfo.lowMemory,
            thresholdBytes = memoryInfo.threshold,
            jvmMaxMemoryBytes = jvmMaxMemoryBytes,
            jvmTotalMemoryBytes = jvmTotalMemoryBytes,
            jvmFreeMemoryBytes = jvmFreeMemoryBytes
        )
    }
}
