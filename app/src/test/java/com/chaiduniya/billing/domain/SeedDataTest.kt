package com.chaiduniya.billing.domain

import com.chaiduniya.billing.data.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedDataTest {
    @Test
    fun menuContainsAllApprovedProducts() {
        assertEquals(72, SeedData.menu.size)
        assertEquals(12, SeedData.menu.map { it.category }.distinct().size)
        assertTrue(SeedData.menu.all { it.name.isNotBlank() && it.rupees > 0 })
    }
}
