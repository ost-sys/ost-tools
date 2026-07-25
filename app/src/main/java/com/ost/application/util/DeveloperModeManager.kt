package com.ost.application.util
import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.widget.Toast
import com.ost.application.R
object DeveloperModeManager {
    private const val PREFS_NAME = "developer_mode_prefs"
    private const val KEY_DEV_MODE_ENABLED = "developer_mode_enabled"
    private const val REQUIRED_TAPS = 7
    private const val TAP_TIMEOUT_MS = 2500L
    private var tapCount = 0
    private var lastTapTime = 0L
    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    fun isDeveloperModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DEV_MODE_ENABLED, false)
    }
    fun setDeveloperModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DEV_MODE_ENABLED, enabled).apply()
    }
    fun onLogoTapped(context: Context, onStateChanged: (Boolean) -> Unit = {}) {
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastTapTime > TAP_TIMEOUT_MS) {
            tapCount = 0
        }
        lastTapTime = currentTime
        tapCount++
        val currentlyEnabled = isDeveloperModeEnabled(context)
        if (currentlyEnabled) {
            Toast.makeText(context, context.getString(R.string.dev_mode_already_active), Toast.LENGTH_SHORT).show()
            return
        }
        val remaining = REQUIRED_TAPS - tapCount
        if (remaining <= 0) {
            setDeveloperModeEnabled(context, true)
            tapCount = 0
            Toast.makeText(context, context.getString(R.string.dev_mode_unlocked), Toast.LENGTH_LONG).show()
            onStateChanged(true)
        } else if (remaining <= 3) {
            Toast.makeText(
                context,
                context.getString(R.string.dev_mode_taps_remaining, remaining),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
