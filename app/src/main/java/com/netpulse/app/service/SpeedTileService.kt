package com.netpulse.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.netpulse.app.MainActivity
import com.netpulse.app.R
import com.netpulse.app.NetPulseApp
import com.netpulse.app.data.SpeedFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Quick Settings tile: shows the live speed and toggles the floating bubble. */
class SpeedTileService : TileService() {
    private var scope: CoroutineScope? = null
    private val app get() = application as NetPulseApp

    override fun onStartListening() {
        super.onStartListening()
        scope?.cancel()
        scope = MainScope().also { s ->
            s.launch {
                combine(app.speed.current, app.settings.settings) { sample, settings -> sample to settings }
                    .collect { (sample, settings) ->
                        val tile = qsTile ?: return@collect
                        tile.label = getString(R.string.tile_label)
                        val speed = SpeedFormat.detailed((sample?.downBps ?: 0) + (sample?.upBps ?: 0)).toString()
                        if (Build.VERSION.SDK_INT >= 29) {
                            tile.subtitle = if (settings.bubbleEnabled) "Bubble on · $speed" else speed
                        }
                        tile.state = if (settings.bubbleEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                        tile.updateTile()
                    }
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        runCatching { SpeedMeterService.start(this) }
        if (!Settings.canDrawOverlays(this)) {
            openApp()
            return
        }
        (scope ?: MainScope()).launch {
            val enabled = app.settings.settings.first().bubbleEnabled
            app.settings.setBubbleEnabled(!enabled)
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_OPEN_SETTINGS, true)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 2, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            )
        } else {
            @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
            startActivityAndCollapse(intent)
        }
    }
}
