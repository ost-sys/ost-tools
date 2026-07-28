package com.ost.application.util
import android.content.Context
import android.content.SharedPreferences
object AppPrefs {
    private const val SETUP_COMPLETE_KEY = "is_setup_complete"
    private const val TOOLS_TOOLTIP_SHOWN_KEY = "is_tools_tooltip_shown"
    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
    fun isSetupComplete(context: Context): Boolean {
        return prefs(context).getBoolean(SETUP_COMPLETE_KEY, false)
    }
    fun setSetupComplete(context: Context, isComplete: Boolean) {
        prefs(context).edit().putBoolean(SETUP_COMPLETE_KEY, isComplete).apply()
    }
    fun isToolsTooltipShown(context: Context): Boolean {
        return prefs(context).getBoolean(TOOLS_TOOLTIP_SHOWN_KEY, false)
    }
    fun setToolsTooltipShown(context: Context, isShown: Boolean) {
        prefs(context).edit().putBoolean(TOOLS_TOOLTIP_SHOWN_KEY, isShown).apply()
    }
}
