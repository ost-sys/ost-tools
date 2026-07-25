package com.ost.application.service
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.ost.application.R
import com.ost.application.core.service.OstAccessibilityService
class LockTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val supported = OstAccessibilityService.isLockScreenSupported()
        if (!supported) {
            tile.state = Tile.STATE_UNAVAILABLE
        } else {
            val enabled = OstAccessibilityService.isAccessibilityServiceEnabled(this)
            tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        }
        tile.updateTile()
    }
    override fun onClick() {
        super.onClick()
        if (!OstAccessibilityService.isLockScreenSupported()) {
            Toast.makeText(this, getString(R.string.requires_android_9), Toast.LENGTH_SHORT).show()
            return
        }
        val success = OstAccessibilityService.performLockScreen()
        if (!success) {
            OstAccessibilityService.openAccessibilitySettings(this)
        }
    }
}
