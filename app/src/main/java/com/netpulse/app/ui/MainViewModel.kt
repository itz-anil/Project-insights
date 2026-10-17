package com.netpulse.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.netpulse.app.NetPulseApp
import com.netpulse.app.data.ConnectionInfo
import com.netpulse.app.data.ConnectionReader
import com.netpulse.app.data.IconStyle
import com.netpulse.app.data.MeterSettings
import com.netpulse.app.data.NetworkType
import com.netpulse.app.data.SpeedTestResult
import com.netpulse.app.data.SpeedUnit
import com.netpulse.app.data.ThemeMode
import com.netpulse.app.data.SpeedTester
import com.netpulse.app.data.TestPhase
import com.netpulse.app.data.TestProgress
import com.netpulse.app.data.UsageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsageUiState(
    val loading: Boolean = true,
    val hasAccess: Boolean = true,
    val snapshot: UsageSnapshot? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NetPulseApp
    private val reader = ConnectionReader(application)

    val current = app.speed.current
    val history = app.speed.history
    val session = app.speed.session
    val running = app.speed.running
    val today = app.usage.today

    val settings: StateFlow<MeterSettings> = app.settings.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, MeterSettings())

    val connection: StateFlow<ConnectionInfo> = flow {
        while (true) {
            emit(reader.read())
            delay(2_000)
        }
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ConnectionInfo())

    // ---- Data usage ----

    private val _usage = MutableStateFlow(UsageUiState())
    val usage: StateFlow<UsageUiState> = _usage.asStateFlow()

    fun refreshUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            _usage.update { it.copy(loading = true) }
            val access = app.usage.hasAccess()
            val snapshot = if (access) runCatching { app.usage.snapshot(settings.value.billingDay) }.getOrNull() else null
            _usage.value = UsageUiState(loading = false, hasAccess = access, snapshot = snapshot)
        }
    }

    fun refreshToday() {
        viewModelScope.launch(Dispatchers.IO) { runCatching { app.usage.refreshToday(settings.value.billingDay) } }
    }

    // ---- Speed test ----

    val testHistory: StateFlow<List<SpeedTestResult>> = app.speedTests.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _test = MutableStateFlow(TestProgress())
    val test: StateFlow<TestProgress> = _test.asStateFlow()
    private var testJob: Job? = null

    fun startTest() {
        if (testJob?.isActive == true) return
        testJob = viewModelScope.launch {
            val network = reader.currentType()
            if (network == NetworkType.NONE) {
                _test.value = TestProgress(phase = TestPhase.ERROR, error = "No internet connection")
                return@launch
            }
            val result = SpeedTester().run { _test.value = it }
            if (result.phase == TestPhase.DONE) {
                app.speedTests.add(
                    SpeedTestResult(
                        timestampMs = System.currentTimeMillis(),
                        network = network,
                        downBps = result.downBps ?: 0,
                        upBps = result.upBps ?: 0,
                        pingMs = result.pingMs ?: 0,
                        jitterMs = result.jitterMs ?: 0,
                    )
                )
            }
        }
    }

    fun stopTest() {
        testJob?.cancel()
        _test.value = TestProgress()
    }

    fun clearTestHistory() = viewModelScope.launch { app.speedTests.clear() }

    // ---- Settings ----

    fun setIconStyle(style: IconStyle) = viewModelScope.launch { app.settings.setIconStyle(style) }
    fun setShowDetails(value: Boolean) = viewModelScope.launch { app.settings.setShowDetails(value) }
    fun setPauseWhenScreenOff(value: Boolean) = viewModelScope.launch { app.settings.setPauseWhenScreenOff(value) }
    fun setStartOnBoot(value: Boolean) = viewModelScope.launch { app.settings.setStartOnBoot(value) }
    fun setBubble(value: Boolean) = viewModelScope.launch { app.settings.setBubbleEnabled(value) }
    fun setAlerts(value: Boolean) = viewModelScope.launch { app.settings.setAlertsEnabled(value) }
    fun setDailyLimitMb(value: Int) = viewModelScope.launch { app.settings.setDailyLimitMb(value) }
    fun setMonthlyLimitMb(value: Int) = viewModelScope.launch { app.settings.setMonthlyMobileLimitMb(value) }
    fun setUnit(value: SpeedUnit) = viewModelScope.launch { app.settings.setUnit(value) }
    fun setRefreshMs(value: Int) = viewModelScope.launch { app.settings.setRefreshMs(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { app.settings.setThemeMode(value) }
    fun setStatusIconsConfirmed(value: Boolean) = viewModelScope.launch { app.settings.setStatusIconsConfirmed(value) }
    fun setBillingDay(value: Int) = viewModelScope.launch {
        app.settings.setBillingDay(value)
        refreshToday()
    }
}
