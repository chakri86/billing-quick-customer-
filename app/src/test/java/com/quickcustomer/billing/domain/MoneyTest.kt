package com.quickcustomer.billing.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun cartTotalUsesIntegerPaiseWithoutRoundingErrors() {
        val total = Money.cartTotal(listOf(1_500L to 2, 2_000L to 1, 5900L to 3))
        assertEquals(22_700L, total)
    }
}
