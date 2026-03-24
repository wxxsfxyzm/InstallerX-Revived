package com.rosan.installer.ui.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.rosan.installer.R
import com.rosan.installer.ui.activity.SettingsActivity

class SettingsTileService : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            label = getString(R.string.quick_settings_tile_label)
            state = Tile.STATE_ACTIVE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = getString(R.string.quick_settings_tile_subtitle)
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun(::openSettings)
    }

    private fun openSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(SettingsActivity.createLaunchPendingIntent(this))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(SettingsActivity.createLaunchIntent(this))
        }
    }
}
