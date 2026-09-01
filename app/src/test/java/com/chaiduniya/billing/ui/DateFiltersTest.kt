package com.chaiduniya.billing.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateFiltersTest {
    @Test
    fun todayWindowIncludesNowAndExcludesTomorrow() {
        val now = System.currentTimeMillis()
        val window = todayWindow()

        assertTrue(window.contains(now))
        assertFalse(window.contains(window.endExclusive))
    }

    @Test
    fun customWindowIncludesBothSelectedCalendarDates() {
        val start = java.time.LocalDate.of(2026, 8, 20)
        val end = java.time.LocalDate.of(2026, 8, 22)
        val window = dateWindow(start, end)

        assertTrue(window.contains(expenseTimestampFor(start)))
        assertTrue(window.contains(expenseTimestampFor(end)))
        assertFalse(window.contains(expenseTimestampFor(end.plusDays(1))))
    }
}
