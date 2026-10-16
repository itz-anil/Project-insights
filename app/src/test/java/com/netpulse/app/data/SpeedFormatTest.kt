package com.netpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatTest {

    @Test
    fun compactUsesKbBelowOneMegabyte() {
        assertEquals(SpeedFormat.Parts("0", "KB/s"), SpeedFormat.compact(0))
        assertEquals(SpeedFormat.Parts("512", "KB/s"), SpeedFormat.compact(512_000))
    }

    @Test
    fun compactKeepsOneDecimalBelowTenMegabytes() {
        assertEquals(SpeedFormat.Parts("2.4", "MB/s"), SpeedFormat.compact(2_400_000))
        assertEquals(SpeedFormat.Parts("12", "MB/s"), SpeedFormat.compact(12_300_000))
    }

    @Test
    fun detailedShowsTwoDecimals() {
        assertEquals("1.92 MB/s", SpeedFormat.detailed(1_920_000).toString())
        assertEquals("480 KB/s", SpeedFormat.detailed(480_000).toString())
    }

    @Test
    fun bytesScalesUpToGigabytes() {
        assertEquals("318 MB", SpeedFormat.bytes(318_000_000).toString())
        assertEquals("1.56 GB", SpeedFormat.bytes(1_560_000_000).toString())
        assertEquals("4.5 MB", SpeedFormat.bytes(4_500_000).toString())
    }
}
