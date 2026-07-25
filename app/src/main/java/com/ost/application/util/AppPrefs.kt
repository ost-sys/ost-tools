package com.ost.application.util
import android.content.Context
import android.preference.PreferenceManager
object AppPrefs {
    private const val SETUP_COMPLETE_KEY = "is_setup_complete"
    private const val TOOLS_TOOLTIP_SHOWN_KEY = "is_tools_tooltip_shown"
    fun isSetupComplete(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(SETUP_COMPLETE_KEY, false)
    }
    fun setSetupComplete(context: Context, isComplete: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(SETUP_COMPLETE_KEY, isComplete).apply()
    }
    fun isToolsTooltipShown(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(TOOLS_TOOLTIP_SHOWN_KEY, false)
    }
    fun setToolsTooltipShown(context: Context, isShown: Boolean) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(TOOLS_TOOLTIP_SHOWN_KEY, isShown).apply()
    }
}