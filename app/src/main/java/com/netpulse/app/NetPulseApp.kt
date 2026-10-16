package com.netpulse.app

import android.app.Application
import com.netpulse.app.data.SettingsRepository
import com.netpulse.app.data.SpeedFormat
import com.netpulse.app.data.SpeedRepository
import com.netpulse.app.data.SpeedTestRepository
import com.netpulse.app.data.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NetPulseApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var usage: UsageRepository
        private set
    lateinit var speedTests: SpeedTestRepository
        private set
    val speed = SpeedRepository()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        usage = UsageRepository(this)
        speedTests = SpeedTestRepository(this)
        // Every screen, the notification, bubble, tile and widget read the unit from here.
        appScope.launch { settings.settings.collect { SpeedFormat.unit = it.unit } }
    }
}
