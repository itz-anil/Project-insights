package com.netpulse.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.netpulse.app.NetPulseApp
import com.netpulse.app.data.MeterSettings
import com.netpulse.app.data.SpeedFormat
import com.netpulse.app.data.SpeedSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The core of the app: a foreground service that measures speed and redraws the
 * status bar icon. It is always on (the status bar meter is mandatory).
 *
 * Battery notes:
 * - With the screen off (and "Pause when screen is off" on) the loop fully sleeps
 *   until the screen comes back; no timers fire.
 * - The notification, bubble and widget are only touched when what they show changes,
 *   so an idle connection costs almost nothing.
 * - The network type comes from a callback, not a system query every tick.
 */
class SpeedMeterService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val main = Handler(Looper.getMainLooper())
    private val app get() = application as NetPulseApp

    private lateinit var notifications: MeterNotifications
    private lateinit var renderer: SpeedIconRenderer
    private lateinit var network: NetworkTypeMonitor
    private lateinit var bubble: FloatingBubble
    private lateinit var alerts: DataAlerts
    private lateinit var settings: StateFlow<MeterSettings>
    private val sampler = TrafficSampler()
    private val screenOn = MutableStateFlow(true)

    @Volatile private var lastNotificationKey: String? = null
    private var lastBubbleKey: String? = null
    private var lastWidgetKey: String? = null
    private var lastWidgetAt = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> screenOn.value = false
                Intent.ACTION_SCREEN_ON -> screenOn.value = true
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications = MeterNotifications(this).also { it.ensureChannel() }
        renderer = SpeedIconRenderer(this)
        network = NetworkTypeMonitor(this).also { it.start() }
        bubble = FloatingBubble(this)
        alerts = DataAlerts(this, app.settings).also { it.ensureChannel() }
        settings = app.settings.settings.stateIn(scope, SharingStarted.Eagerly, MeterSettings())

        if (!goForeground()) return

        screenOn.value = getSystemService(PowerManager::class.java)?.isInteractive ?: true
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
        scope.launch {
            settings.collect {
                // Any settings change should show up right away on the next tick.
                lastNotificationKey = null
                if (!it.bubbleEnabled) main.post { bubble.hide() }
            }
        }
        scope.launch { meterLoop() }
        scope.launch { usageLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        scope.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        network.stop()
        main.post { bubble.hide() }
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
        sampler.sample() // baseline
        var nextTick = SystemClock.elapsedRealtime()
        while (scope.isActive) {
            if (!(screenOn.value || !settings.value.pauseWhenScreenOff)) {
                // Sleep with no timers at all until the screen is back on.
                combine(screenOn, settings) { on, s -> on || !s.pauseWhenScreenOff }.first { it }
                sampler.reset()
                sampler.sample()
                lastNotificationKey = null
                nextTick = SystemClock.elapsedRealtime()
            }
            nextTick += settings.value.refreshSeconds * 1_000L
            val wait = nextTick - SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait) else nextTick = SystemClock.elapsedRealtime()
            sampler.sample()?.let { publish(it, settings.value) }
        }
    }

    /**
     * System usage numbers (today / this cycle) change slowly and Android itself only
     * updates them every few minutes, so refresh every 5 minutes while the screen is on.
     */
    private suspend fun usageLoop() {
        while (scope.isActive) {
            screenOn.first { it }
            val current = settings.value
            val today = runCatching { app.usage.refreshToday(current.billingDay) }.getOrNull()
            if (today != null) runCatching { alerts.check(today, current) }
            lastNotificationKey = null
            delay(USAGE_INTERVAL_MS)
        }
    }

    private fun publish(reading: TrafficSampler.Reading, current: MeterSettings) {
        val type = network.type
        app.speed.publish(SpeedSample(reading.downBps, reading.upBps, type, System.currentTimeMillis()), reading.elapsedMs)

        val down = SpeedFormat.compact(reading.downBps, current.unit)
        val up = SpeedFormat.compact(reading.upBps, current.unit)
        val total = SpeedFormat.compact(reading.downBps + reading.upBps, current.unit)
        val today = app.usage.today.value

        // Only redraw and re-post when something the user can see has changed.
        val key = buildString {
            append(current.iconStyle).append('|').append(total).append('|').append(down).append('|').append(up)
            if (current.showDetails) {
                append('|').append(SpeedFormat.detailed(reading.downBps, current.unit))
                append('|').append(SpeedFormat.detailed(reading.upBps, current.unit))
                append('|').append(type).append('|').append(today?.wifi).append('|').append(today?.mobile)
                if (today == null) append('|').append(app.speed.session.value.downBytes / 1_000_000)
            }
        }
        if (key != lastNotificationKey) {
            lastNotificationKey = key
            val icon = renderer.render(current.iconStyle, reading.downBps, reading.upBps, current.unit)
            notifications.notify(
                notifications.build(
                    icon = icon,
                    downBps = reading.downBps,
                    upBps = reading.upBps,
                    network = type,
                    session = app.speed.session.value,
                    today = today,
                    showDetails = current.showDetails,
                )
            )
        }

        if (current.bubbleEnabled) {
            val bubbleKey = "$down|$up"
            // Until the overlay permission is granted, keep trying on every tick.
            if (bubbleKey != lastBubbleKey || !android.provider.Settings.canDrawOverlays(this)) {
                lastBubbleKey = bubbleKey
                main.post { bubble.sync(true, reading.downBps, reading.upBps) }
            }
        } else {
            lastBubbleKey = null
        }

        val now = SystemClock.elapsedRealtime()
        val widgetKey = "${SpeedFormat.detailed(reading.downBps)}|${SpeedFormat.detailed(reading.upBps)}|$type|${today?.today}"
        if (widgetKey != lastWidgetKey && now - lastWidgetAt >= WIDGET_MIN_INTERVAL_MS) {
            lastWidgetKey = widgetKey
            lastWidgetAt = now
            runCatching { SpeedWidget.push(this, reading.downBps, reading.upBps, type, today?.today) }
        }
    }

    companion object {
        private const val TAG = "SpeedMeterService"
        private const val USAGE_INTERVAL_MS = 5 * 60_000L
        private const val WIDGET_MIN_INTERVAL_MS = 3_000L

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SpeedMeterService::class.java),
            )
        }
    }
}
