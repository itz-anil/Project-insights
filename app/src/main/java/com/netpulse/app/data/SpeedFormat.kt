package com.netpulse.app.data

import java.util.Locale

/** Formatting in MB/s (decimal units, 1 MB = 1,000,000 bytes), falling back to KB/s. */
object SpeedFormat {

    data class Parts(val value: String, val unit: String) {
        override fun toString() = "$value $unit"
    }

    /** Compact form for the tiny status bar icon: at most 3–4 characters. */
    fun compact(bytesPerSec: Long): Parts {
        val kb = bytesPerSec / 1_000.0
        val mb = bytesPerSec / 1_000_000.0
        return when {
            kb < 1 -> Parts("0", "KB/s")
            kb < 1_000 -> Parts(fmt("%.0f", kb), "KB/s")
            mb < 10 -> Parts(fmt("%.1f", mb), "MB/s")
            else -> Parts(fmt("%.0f", mb), "MB/s")
        }
    }

    /** Roomier form for the app and notification. */
    fun detailed(bytesPerSec: Long): Parts {
        val kb = bytesPerSec / 1_000.0
        val mb = bytesPerSec / 1_000_000.0
        return when {
            kb < 1_000 -> Parts(fmt("%.0f", kb), "KB/s")
            mb < 100 -> Parts(fmt("%.2f", mb), "MB/s")
            else -> Parts(fmt("%.1f", mb), "MB/s")
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
