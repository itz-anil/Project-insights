package com.netpulse.app.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class BillingCycleTest {

    private fun date(y: Int, m: Int, d: Int, h: Int = 12) = Calendar.getInstance().apply {
        clear()
        set(y, m, d, h, 0)
    }.timeInMillis

    @Test
    fun cycleStartsThisMonthOnOrAfterBillingDay() {
        assertEquals(date(2026, Calendar.OCTOBER, 5, 0), UsageRepository.cycleStart(date(2026, Calendar.OCTOBER, 16), 5))
        assertEquals(date(2026, Calendar.OCTOBER, 16, 0), UsageRepository.cycleStart(date(2026, Calendar.OCTOBER, 16), 16))
    }

    @Test
    fun cycleStartsLastMonthBeforeBillingDay() {
        assertEquals(date(2026, Calendar.SEPTEMBER, 20, 0), UsageRepository.cycleStart(date(2026, Calendar.OCTOBER, 16), 20))
        assertEquals(date(2025, Calendar.DECEMBER, 28, 0), UsageRepository.cycleStart(date(2026, Calendar.JANUARY, 3), 28))
    }
}
