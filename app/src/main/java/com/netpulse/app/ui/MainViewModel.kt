package com.netpulse.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netpulse.app.NetPulseApp
import com.netpulse.app.data.ConnectionInfo
import com.netpulse.app.data.ConnectionReader
import com.netpulse.app.data.IconStyle
import com.netpulse.app.data.MeterSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NetPulseApp
    private val reader = ConnectionReader(application)

    val current = app.speed.current
    val history = app.speed.history
    val session = app.speed.session
    val running = app.speed.running

    val settings: StateFlow<MeterSettings> = app.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, MeterSettings())

    val connection: StateFlow<ConnectionInfo> = flow {
        while (true) {
            emit(reader.read())
            delay(2_000)
        }
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionInfo())

    fun setIconStyle(style: IconStyle) = viewModelScope.launch { app.settings.setIconStyle(style) }
    fun setShowDetails(value: Boolean) = viewModelScope.launch { app.settings.setShowDetails(value) }
    fun setPauseWhenScreenOff(value: Boolean) = viewModelScope.launch { app.settings.setPauseWhenScreenOff(value) }
    fun setStartOnBoot(value: Boolean) = viewModelScope.launch { app.settings.setStartOnBoot(value) }
}
