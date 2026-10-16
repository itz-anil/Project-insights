package com.netpulse.app

import android.app.Application
import com.netpulse.app.data.SettingsRepository
import com.netpulse.app.data.SpeedRepository

class NetPulseApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    val speed = SpeedRepository()

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(this)
    }
}
