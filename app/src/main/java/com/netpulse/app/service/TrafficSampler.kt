package com.netpulse.app.service

import android.net.TrafficStats
import android.os.SystemClock

/** Turns the device-wide byte counters into bytes-per-second readings. */
class TrafficSampler {
    private var lastRx = -1L
    private var lastTx = -1L
    private var lastTimeMs = 0L

    data class Reading(val downBps: Long, val upBps: Long, val elapsedMs: Long)

    /** Forget the baseline, e.g. after the meter was paused. */
    fun reset() {
        lastRx = -1L
        lastTx = -1L
    }

    /** Returns null on the first call (baseline only) or when counters are unsupported. */
    fun sample(): Reading? {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        val now = SystemClock.elapsedRealtime()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) {
            return null
        }
        val hadBaseline = lastRx >= 0
        val elapsed = now - lastTimeMs
        val dRx = rx - lastRx
        val dTx = tx - lastTx
        lastRx = rx
        lastTx = tx
        lastTimeMs = now
        if (!hadBaseline || elapsed <= 0) return null
        // Counters can drop when an interface goes away; treat that as zero traffic.
        return Reading(
            downBps = dRx.coerceAtLeast(0) * 1000 / elapsed,
            upBps = dTx.coerceAtLeast(0) * 1000 / elapsed,
            elapsedMs = elapsed,
        )
    }
}
