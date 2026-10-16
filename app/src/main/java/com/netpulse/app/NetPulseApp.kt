package com.netpulse.app

import android.app.Application
import com.netpulse.app.data.SettingsRepository
import com.netpulse.app.data.SpeedRepository
import com.netpulse.app.data.SpeedTestRepository
import com.netpulse.app.data.UsageRepository

class NetPulseApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var usage: UsageRepository
        private set
    lateinit var speedTests: SpeedTestRepository
        private set
    val speed = SpeedRepository()

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
        usage = UsageRepository(this)
        speedTests = SpeedTestRepository(this)
    }
}
