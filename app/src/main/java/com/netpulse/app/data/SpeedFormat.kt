package com.netpulse.app.data

import java.util.Locale

/**
 * Speed and size formatting. Speeds follow the user's unit choice:
 * MB/s (decimal megabytes, default) or Mbps (megabits). Sizes are always decimal bytes.
 */
object SpeedFormat {

    /** Current speed unit; kept in sync with settings by [com.netpulse.app.NetPulseApp]. */
    @Volatile
    var unit: SpeedUnit = SpeedUnit.MBYTES

    data class Parts(val value: String, val unit: String) {
        override fun toString() = "$value $unit"
    }

    /** Compact form for the tiny status bar icon: at most 3–4 characters. */
    fun compact(bytesPerSec: Long, unit: SpeedUnit = this.unit): Parts = when (unit) {
        SpeedUnit.MBYTES -> {
            val kb = bytesPerSec / 1_000.0
            val mb = bytesPerSec / 1_000_000.0
            when {
                kb < 1 -> Parts("0", "KB/s")
                kb < 1_000 -> Parts(fmt("%.0f", kb), "KB/s")
                mb < 10 -> Parts(fmt("%.1f", mb), "MB/s")
                else -> Parts(fmt("%.0f", mb), "MB/s")
            }
        }
        SpeedUnit.MBITS -> {
            val kbit = bytesPerSec * 8 / 1_000.0
            val mbit = bytesPerSec * 8 / 1_000_000.0
            when {
                kbit < 1 -> Parts("0", "Kbps")
                kbit < 1_000 -> Parts(fmt("%.0f", kbit), "Kbps")
                mbit < 10 -> Parts(fmt("%.1f", mbit), "Mbps")
                mbit < 1_000 -> Parts(fmt("%.0f", mbit), "Mbps")
                else -> Parts(fmt("%.1f", mbit / 1_000), "Gbps")
            }
        }
    }

    /** Roomier form for the app and notification. */
    fun detailed(bytesPerSec: Long, unit: SpeedUnit = this.unit): Parts = when (unit) {
        SpeedUnit.MBYTES -> {
            val kb = bytesPerSec / 1_000.0
            val mb = bytesPerSec / 1_000_000.0
            when {
                kb < 1_000 -> Parts(fmt("%.0f", kb), "KB/s")
                mb < 100 -> Parts(fmt("%.2f", mb), "MB/s")
                else -> Parts(fmt("%.1f", mb), "MB/s")
            }
        }
        SpeedUnit.MBITS -> {
            val kbit = bytesPerSec * 8 / 1_000.0
            val mbit = bytesPerSec * 8 / 1_000_000.0
            when {
                kbit < 1_000 -> Parts(fmt("%.0f", kbit), "Kbps")
                mbit < 100 -> Parts(fmt("%.1f", mbit), "Mbps")
                else -> Parts(fmt("%.0f", mbit), "Mbps")
            }
        }
    }

    fun bytes(count: Long): Parts {
        val kb = count / 1_000.0
        val mb = count / 1_000_000.0
        val gb = count / 1_000_000_000.0
        return when {
            kb < 1_000 -> Parts(fmt("%.0f", kb), "KB")
            mb < 1_000 -> Parts(fmt(if (mb < 10) "%.1f" else "%.0f", mb), "MB")
            else -> Parts(fmt("%.2f", gb), "GB")
        }
    }

    private fun fmt(pattern: String, value: Double) = String.format(Locale.US, pattern, value)
}
