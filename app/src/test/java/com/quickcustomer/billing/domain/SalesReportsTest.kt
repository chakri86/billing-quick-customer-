package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.PaymentMethod
import com.quickcustomer.billing.data.SaleEntity
import com.quickcustomer.billing.data.SaleItemEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class SalesReportsTest {
    @Test
    fun ranksOnlyItemsFromIncludedNonCancelledSales() {
        val included = sale("included")
        val cancelled = sale("cancelled", isCancelled = true)
        val outsidePeriod = sale("outside")
        val items = listOf(
            item("1", included.id, "Tea", 3, 6_000),
            item("2", included.id, "Coffee", 2, 5_000),
            item("3", cancelled.id, "Coffee", 50, 125_000),
            item("4", outsidePeriod.id, "Shake", 40, 240_000)
        )

        val result = rankProductSales(listOf(included, cancelled), items)

        assertEquals(listOf("Tea", "Coffee"), result.map { it.productName })
        assertEquals(listOf(3L, 2L), result.map { it.quantity })
        assertEquals(listOf(6_000L, 5_000L), result.map { it.revenuePaise })
    }

    @Test
    fun combinesSameProductAndAppliesRequestedLimit() {
        val first = sale("first")
        val second = sale("second")
        val items = listOf(
            item("1", first.id, "Tea", 2, 4_000),
            item("2", second.id, "Tea", 3, 6_000),
            item("3", second.id, "Coffee", 4, 10_000)
        )

        val result = rankProductSales(listOf(first, second), items, limit = 1)

        assertEquals(1, result.size)
        assertEquals("Tea", result.single().productName)
        assertEquals(5L, result.single().quantity)
        assertEquals(10_000L, result.single().revenuePaise)
    }

    private fun sale(id: String, isCancelled: Boolean = false) = SaleEntity(
        id = id,
        invoiceNumber = "QC-$id",
        createdAt = 1L,
        cashierId = "cashier",
        cashierName = "Cashier",
        subtotalPaise = 1_000,
        totalPaise = 1_000,
        paymentMethod = PaymentMethod.CASH,
        isCancelled = isCancelled
    )

    private fun item(
        id: String,
        saleId: String,
        name: String,
        quantity: Int,
        totalPaise: Long
    ) = SaleItemEntity(
        id = id,
        saleId = saleId,
        productId = name,
        productNameSnapshot = name,
        unitPricePaise = totalPaise / quantity,
        quantity = quantity,
        lineTotalPaise = totalPaise
    )
}
