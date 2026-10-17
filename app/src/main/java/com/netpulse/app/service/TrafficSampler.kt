package com.netpulse.app.service

import android.net.TrafficStats
import android.os.SystemClock

/**
 * Speed over a sliding time window, from raw byte counters.
 * Sampling often but averaging over [windowMs] gives a live-looking value that is still
 * accurate: a single 100 ms slice of traffic is too bursty to mean anything on its own.
 */
class RateWindow(var windowMs: Long = 1_000) {
    private class Point(val timeMs: Long, val rx: Long, val tx: Long)

    private val points = ArrayDeque<Point>()

    data class Reading(
        val downBps: Long,
        val upBps: Long,
        /** Exact bytes since the previous call, for usage totals. */
        val deltaDown: Long,
        val deltaUp: Long,
    )

    fun reset() = points.clear()

    /** Returns null until there are two points to compare. */
    fun add(timeMs: Long, rx: Long, tx: Long): Reading? {
        val last = points.lastOrNull()
        if (last != null && (rx < last.rx || tx < last.tx)) {
            // A counter went backwards (interface removed / reset): start over.
            points.clear()
        }
        val previous = points.lastOrNull()
        points.addLast(Point(timeMs, rx, tx))
        // Keep exactly one point at or before the window start, so the span covers the window.
        while (points.size > 2 && timeMs - points[1].timeMs >= windowMs) points.removeFirst()
        if (previous == null) return null

        val oldest = points.first()
        val span = timeMs - oldest.timeMs
        if (span <= 0) return null
        return Reading(
            downBps = (rx - oldest.rx) * 1000 / span,
            upBps = (tx - oldest.tx) * 1000 / span,
            deltaDown = rx - previous.rx,
            deltaUp = tx - previous.tx,
        )
    }
}

/** Reads the device-wide counters into a [RateWindow]. */
class TrafficSampler {
    val window = RateWindow()

    fun reset() = window.reset()

    fun sample(): RateWindow.Reading? {
        val rx = TrafficStats.getTotalRxBytes()
        val tx = TrafficStats.getTotalTxBytes()
        if (rx == TrafficStats.UNSUPPORTED.toLong() || tx == TrafficStats.UNSUPPORTED.toLong()) return null
        return window.add(SystemClock.elapsedRealtime(), rx, tx)
    }
}
