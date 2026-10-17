package com.netpulse.app.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RateWindowTest {

    @Test
    fun firstPointHasNoReading() {
        assertNull(RateWindow().add(0, 0, 0))
    }

    @Test
    fun steadyTrafficGivesExactRate() {
        val w = RateWindow(windowMs = 1_000)
        w.add(0, 0, 0)
        var r: RateWindow.Reading? = null
        // 100 ms ticks, 200 KB down + 50 KB up per tick = 2 MB/s down, 0.5 MB/s up.
        for (i in 1..20) r = w.add(i * 100L, i * 200_000L, i * 50_000L)
        assertEquals(2_000_000L, r!!.downBps)
        assertEquals(500_000L, r.upBps)
        assertEquals(200_000L, r.deltaDown)
    }

    @Test
    fun windowForgetsOldBursts() {
        val w = RateWindow(windowMs = 1_000)
        w.add(0, 0, 0)
        w.add(100, 5_000_000, 0) // one big burst
        var r: RateWindow.Reading? = null
        for (i in 2..15) r = w.add(i * 100L, 5_000_000, 0) // then nothing
        assertEquals(0L, r!!.downBps)
    }

    @Test
    fun counterResetStartsOver() {
        val w = RateWindow()
        w.add(0, 1_000, 1_000)
        w.add(100, 2_000, 2_000)
        assertNull(w.add(200, 10, 10))
    }
}
