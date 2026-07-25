package com.ost.application.core.storage
import android.os.Environment
import android.os.StatFs
data class StorageInfo(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usedPercentage: Int,
    val storagePath: String,
    val state: String
)
class StorageInfoProvider {
    fun getStorageInfo(): StorageInfo {
        val path = Environment.getDataDirectory().path
        val stat = StatFs(path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
        val usedPercentage = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0
        val state = Environment.getExternalStorageState()
        return StorageInfo(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            usedPercentage = usedPercentage,
            storagePath = path,
            state = state
        )
    }
}
