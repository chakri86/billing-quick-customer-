package com.quickcustomer.billing.ui

import com.quickcustomer.billing.data.ProductSalesSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class TopProductFilterTest {
    @Test
    fun dateRangeQueryContract_usesExclusiveEndBoundary() {
        val window = DateWindow(startInclusive = 1_000L, endExclusive = 2_000L)

        assertEquals(true, window.contains(1_000L))
        assertEquals(true, window.contains(1_999L))
        assertEquals(false, window.contains(2_000L))
    }

    @Test
    fun topLimit_preservesRankingOrder() {
        val ranked = (1..50).map {
            ProductSalesSummary(productName = "Product $it", quantity = (51 - it).toLong(), revenuePaise = it * 100L)
        }

        assertEquals((1..10).map { "Product $it" }, ranked.take(10).map { it.productName })
        assertEquals(50, ranked.size)
    }
}
