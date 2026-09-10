package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {
    @Test
    fun menuContainsAllApprovedProducts() {
        assertEquals(72, SeedData.menu.size)
        assertEquals(12, SeedData.menu.map { it.category }.distinct().size)
        assertTrue(SeedData.menu.all { it.name.isNotBlank() && it.rupees > 0 })
        assertTrue(SeedData.menu.any { it.name == "Quick Customer Spl Tea" })
        assertTrue(SeedData.menu.any { it.category == "Special Shakes" })
        assertTrue(SeedData.menu.none { it.category == "Quick Customer Special Shakes" })
    }
}
