package com.netpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatTest {

    @Test
    fun compactUsesKbBelowOneMegabyte() {
        assertEquals(SpeedFormat.Parts("0", "KB/s"), SpeedFormat.compact(0, SpeedUnit.MBYTES))
        assertEquals(SpeedFormat.Parts("512", "KB/s"), SpeedFormat.compact(512_000, SpeedUnit.MBYTES))
    }

    @Test
    fun compactKeepsOneDecimalBelowTenMegabytes() {
        assertEquals(SpeedFormat.Parts("2.4", "MB/s"), SpeedFormat.compact(2_400_000, SpeedUnit.MBYTES))
        assertEquals(SpeedFormat.Parts("12", "MB/s"), SpeedFormat.compact(12_300_000, SpeedUnit.MBYTES))
    }

    @Test
    fun detailedShowsTwoDecimals() {
        assertEquals("1.92 MB/s", SpeedFormat.detailed(1_920_000, SpeedUnit.MBYTES).toString())
        assertEquals("480 KB/s", SpeedFormat.detailed(480_000, SpeedUnit.MBYTES).toString())
    }

    @Test
    fun bytesScalesUpToGigabytes() {
        assertEquals("318 MB", SpeedFormat.bytes(318_000_000).toString())
        assertEquals("1.56 GB", SpeedFormat.bytes(1_560_000_000).toString())
        assertEquals("4.5 MB", SpeedFormat.bytes(4_500_000).toString())
    }
}

class SpeedFormatBitsTest {

    @Test
    fun megabitsMultiplyByEight() {
        assertEquals(SpeedFormat.Parts("19", "Mbps"), SpeedFormat.compact(2_400_000, SpeedUnit.MBITS))
        assertEquals(SpeedFormat.Parts("4.1", "Mbps"), SpeedFormat.compact(512_000, SpeedUnit.MBITS))
        assertEquals("15.4 Mbps", SpeedFormat.detailed(1_920_000, SpeedUnit.MBITS).toString())
        assertEquals("800 Kbps", SpeedFormat.detailed(100_000, SpeedUnit.MBITS).toString())
    }
}
