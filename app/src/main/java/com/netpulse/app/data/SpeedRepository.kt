package com.netpulse.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class NetworkType { WIFI, MOBILE, ETHERNET, OTHER, NONE }

/** One reading of the meter. Speeds are bytes per second. */
data class SpeedSample(
    val downBps: Long,
    val upBps: Long,
    val network: NetworkType,
    val timestampMs: Long,
) {
    val totalBps: Long get() = downBps + upBps
}

data class SessionTotals(val downBytes: Long = 0, val upBytes: Long = 0)

/** In-memory bridge between the meter service and the UI. */
class SpeedRepository {
    private val _current = MutableStateFlow<SpeedSample?>(null)
    val current: StateFlow<SpeedSample?> = _current.asStateFlow()

    private val _history = MutableStateFlow<List<SpeedSample>>(emptyList())
    val history: StateFlow<List<SpeedSample>> = _history.asStateFlow()

    private val _session = MutableStateFlow(SessionTotals())
    val session: StateFlow<SessionTotals> = _session.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    fun publish(sample: SpeedSample, elapsedMs: Long) {
        _current.value = sample
        _history.update { (it + sample).takeLast(HISTORY_SIZE) }
        _session.update {
            SessionTotals(
                downBytes = it.downBytes + sample.downBps * elapsedMs / 1000,
                upBytes = it.upBytes + sample.upBps * elapsedMs / 1000,
            )
        }
    }

    fun setRunning(value: Boolean) {
        _running.value = value
    }

    companion object {
        const val HISTORY_SIZE = 60
    }
}
