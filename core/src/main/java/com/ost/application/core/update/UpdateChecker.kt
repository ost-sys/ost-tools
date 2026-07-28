package com.ost.application.core.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
data class LatestRelease(
    val tagName: String,
    val isPrerelease: Boolean,
    val body: String,
    val phoneVersion: String?,
    val wearVersion: String?,
    val phoneApkUrl: String?,
    val wearApkUrl: String?
)
object UpdateChecker {
    private const val RELEASES_URL = "https://api.github.com/repos/ost-sys/ost-tools/releases?per_page=15"
    const val PHONE_APK_ASSET_NAME = "app-release.apk"
    const val WEAR_APK_ASSET_NAME = "wear-app-release.apk"
    private val PHONE_VERSION_REGEX = Regex("""\*{0,2}Latest Phone Version:\*{0,2}\s*([^\s*]+)""")
    private val WEAR_VERSION_REGEX = Regex("""\*{0,2}Latest Wear OS Version:\*{0,2}\s*([^\s*]+)""")
    suspend fun fetchLatestRelease(includePrereleases: Boolean): LatestRelease? = withContext(Dispatchers.IO) {
        val connection = URL(RELEASES_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "OST-Tools-App")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub API returned HTTP ${connection.responseCode}")
            }
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            val releases = JSONArray(response)
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                if (release.optBoolean("draft", false)) continue
                if (!includePrereleases && release.optBoolean("prerelease", false)) continue
                val body = release.optString("body", "")
                var phoneApkUrl: String? = null
                var wearApkUrl: String? = null
                val assets = release.optJSONArray("assets")
                if (assets != null) {
                    for (j in 0 until assets.length()) {
                        val asset = assets.getJSONObject(j)
                        when (asset.optString("name")) {
                            PHONE_APK_ASSET_NAME -> phoneApkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                            WEAR_APK_ASSET_NAME -> wearApkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                        }
                    }
                }
                return@withContext LatestRelease(
                    tagName = release.optString("tag_name"),
                    isPrerelease = release.optBoolean("prerelease", false),
                    body = body,
                    phoneVersion = PHONE_VERSION_REGEX.find(body)?.groupValues?.get(1),
                    wearVersion = WEAR_VERSION_REGEX.find(body)?.groupValues?.get(1),
                    phoneApkUrl = phoneApkUrl,
                    wearApkUrl = wearApkUrl
                )
            }
            null
        } finally {
            connection.disconnect()
        }
    }
    fun isPrereleaseVersion(version: String?): Boolean {
        return version != null && splitVersion(version).second != null
    }
    fun isNewerVersion(candidate: String?, current: String?): Boolean = compareVersions(candidate, current) > 0
    fun compareVersions(v1: String?, v2: String?): Int {
        if (v1 == null && v2 == null) return 0
        if (v1 == null) return -1
        if (v2 == null) return 1
        val (nums1, pre1) = splitVersion(v1)
        val (nums2, pre2) = splitVersion(v2)
        val maxLen = maxOf(nums1.size, nums2.size)
        for (i in 0 until maxLen) {
            val p1 = nums1.getOrElse(i) { 0 }
            val p2 = nums2.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return when {
            pre1 == null && pre2 == null -> 0
            pre1 == null -> 1
            pre2 == null -> -1
            else -> comparePrerelease(pre1, pre2)
        }
    }
    private fun splitVersion(raw: String): Pair<List<Int>, String?> {
        val cleaned = raw.trim().removePrefix("v").removePrefix("V")
        val sepIdx = cleaned.indexOfFirst { it == '-' || it == '_' }
        val numPart = if (sepIdx >= 0) cleaned.substring(0, sepIdx) else cleaned
        val pre = if (sepIdx >= 0) cleaned.substring(sepIdx + 1).ifBlank { null } else null
        return numPart.split('.').map { it.toIntOrNull() ?: 0 } to pre
    }
    private fun comparePrerelease(pre1: String, pre2: String): Int {
        val (label1, num1) = splitPrerelease(pre1)
        val (label2, num2) = splitPrerelease(pre2)
        val labelCmp = label1.compareTo(label2, ignoreCase = true)
        if (labelCmp != 0) return labelCmp
        return num1.compareTo(num2)
    }
    private fun splitPrerelease(pre: String): Pair<String, Int> {
        val digitsIdx = pre.indexOfFirst { it.isDigit() }
        return if (digitsIdx >= 0) {
            pre.substring(0, digitsIdx) to (pre.substring(digitsIdx).toIntOrNull() ?: 0)
        } else {
            pre to 0
        }
    }
}
