package com.ost.application.core.service
import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
class OstAccessibilityService : AccessibilityService() {
    companion object {
        @Volatile
        private var instance: OstAccessibilityService? = null
        fun getInstance(): OstAccessibilityService? = instance
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            val targetService = ComponentName(context, OstAccessibilityService::class.java).flattenToString()
            val shortTargetService = ComponentName(context, OstAccessibilityService::class.java).flattenToShortString()
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(targetService, ignoreCase = true) ||
                    componentName.equals(shortTargetService, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }
        fun isLockScreenSupported(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        }
        fun performPowerDialog(): Boolean {
            val activeInstance = getInstance()
            if (activeInstance == null) {
                Log.w("OstAccessibility", "performPowerDialog failed: OstAccessibilityService instance is null")
                return false
            }
            val result = activeInstance.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            Log.d("OstAccessibility", "performGlobalAction(GLOBAL_ACTION_POWER_DIALOG) result: $result")
            return result
        }
        fun performLockScreen(): Boolean {
            if (isLockScreenSupported()) {
                val activeInstance = getInstance()
                if (activeInstance == null) {
                    Log.w("OstAccessibility", "performLockScreen failed: OstAccessibilityService instance is null")
                    return false
                }
                val result = activeInstance.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                Log.d("OstAccessibility", "performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) result: $result")
                return result
            }
            return false
        }
        fun openAccessibilitySettings(context: Context) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("OstAccessibility", "OstAccessibilityService onServiceConnected")
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }
    override fun onInterrupt() {
    }
    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.d("OstAccessibility", "OstAccessibilityService onDestroy")
    }
}
