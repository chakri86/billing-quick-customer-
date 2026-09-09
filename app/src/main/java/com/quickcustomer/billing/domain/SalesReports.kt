package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.ProductSalesSummary
import com.quickcustomer.billing.data.SaleEntity
import com.quickcustomer.billing.data.SaleItemEntity

/**
 * Produces a quantity-first product ranking for the supplied report-period sales.
 * Cancelled sales are always excluded. Product names and revenue use the immutable
 * snapshots stored with each bill so later product edits do not alter old reports.
 */
fun rankProductSales(
    sales: List<SaleEntity>,
    saleItems: List<SaleItemEntity>,
    limit: Int? = null
): List<ProductSalesSummary> {
    val includedSaleIds = sales.asSequence()
        .filterNot(SaleEntity::isCancelled)
        .map(SaleEntity::id)
        .toHashSet()

    val ranked = saleItems.asSequence()
        .filter { it.saleId in includedSaleIds }
        .groupBy(SaleItemEntity::productNameSnapshot)
        .map { (productName, lines) ->
            ProductSalesSummary(
                productName = productName,
                quantity = lines.sumOf { it.quantity.toLong() },
                revenuePaise = lines.sumOf(SaleItemEntity::lineTotalPaise)
            )
        }
        .sortedWith(compareByDescending<ProductSalesSummary> { it.quantity }.thenBy { it.productName })

    return limit?.let(ranked::take) ?: ranked
}
