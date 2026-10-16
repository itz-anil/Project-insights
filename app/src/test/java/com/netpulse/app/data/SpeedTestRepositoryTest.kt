package com.netpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedTestRepositoryTest {

    @Test
    fun encodeDecodeRoundTrip() {
        val result = SpeedTestResult(1_760_000_000_000, NetworkType.WIFI, 6_100_000, 2_300_000, 24, 4)
        assertEquals(result, SpeedTestRepository.decode(SpeedTestRepository.encode(result)))
    }

    @Test
    fun decodeRejectsBrokenLines() {
        assertNull(SpeedTestRepository.decode(""))
        assertNull(SpeedTestRepository.decode("1,WIFI,2"))
        assertNull(SpeedTestRepository.decode("x,WIFI,1,2,3,4"))
    }
}
