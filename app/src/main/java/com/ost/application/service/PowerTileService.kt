package com.ost.application.service
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.ost.application.core.service.OstAccessibilityService
class PowerTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        val enabled = OstAccessibilityService.isAccessibilityServiceEnabled(this)
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
    override fun onClick() {
        super.onClick()
        val success = OstAccessibilityService.performPowerDialog()
        if (!success) {
            OstAccessibilityService.openAccessibilitySettings(this)
        }
    }
}
