package com.netpulse.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.netpulse.app.NetPulseApp
import com.netpulse.app.data.ConnectionReader
import com.netpulse.app.data.MeterSettings
import com.netpulse.app.data.SpeedSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The core of the app: a foreground service that measures speed every second
 * and redraws the status bar icon. It is always on (the status bar meter is mandatory).
 */
class SpeedMeterService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val app get() = application as NetPulseApp

    private lateinit var notifications: MeterNotifications
    private lateinit var renderer: SpeedIconRenderer
    private lateinit var connection: ConnectionReader
    private val sampler = TrafficSampler()

    @Volatile private var settings = MeterSettings()
    @Volatile private var screenOn = true

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> screenOn = false
                Intent.ACTION_SCREEN_ON -> {
                    screenOn = true
                    sampler.reset()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications = MeterNotifications(this).also { it.ensureChannel() }
        renderer = SpeedIconRenderer(this)
        connection = ConnectionReader(this)

        if (!goForeground()) return

        screenOn = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        app.speed.setRunning(true)
        scope.launch { app.settings.settings.collectLatest { settings = it } }
        scope.launch { meterLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        app.speed.setRunning(false)
        super.onDestroy()
    }

    private fun goForeground(): Boolean = try {
        val placeholder = notifications.placeholder()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                MeterNotifications.NOTIFICATION_ID,
                placeholder,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(MeterNotifications.NOTIFICATION_ID, placeholder)
        }
        true
    } catch (e: Exception) {
        // E.g. ForegroundServiceStartNotAllowedException when started from the background.
        Log.w(TAG, "Could not enter foreground", e)
        stopSelf()
        false
    }

    private suspend fun meterLoop() {
        var nextTick = SystemClock.elapsedRealtime()
        while (scope.isActive) {
            val current = settings
            if (screenOn || !current.pauseWhenScreenOff) {
                sampler.sample()?.let { publish(it, current) }
            }
            nextTick += INTERVAL_MS
            val wait = nextTick - SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait) else nextTick = SystemClock.elapsedRealtime()
        }
    }

    private fun publish(reading: TrafficSampler.Reading, current: MeterSettings) {
        val network = connection.currentType()
        val sample = SpeedSample(reading.downBps, reading.upBps, network, System.currentTimeMillis())
        app.speed.publish(sample, reading.elapsedMs)

        val icon = renderer.render(current.iconStyle, reading.downBps, reading.upBps)
        notifications.notify(
            notifications.build(
                icon = icon,
                downBps = reading.downBps,
                upBps = reading.upBps,
                network = network,
                session = app.speed.session.value,
                showDetails = current.showDetails,
            )
        )
    }

    companion object {
        private const val TAG = "SpeedMeterService"
        private const val INTERVAL_MS = 1_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SpeedMeterService::class.java),
            )
        }
    }
}
