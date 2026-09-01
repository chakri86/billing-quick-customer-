package com.quickcustomer.billing.domain

data class BillTotals(
    val subtotalPaise: Long,
    val discountPaise: Long,
    val taxPaise: Long,
    val totalPaise: Long
)

object BillingCalculator {
    fun calculate(
        subtotalPaise: Long,
        requestedDiscountPaise: Long,
        taxEnabled: Boolean,
        taxRateBps: Int,
        pricesIncludeTax: Boolean
    ): BillTotals {
        require(subtotalPaise >= 0)
        val discount = requestedDiscountPaise.coerceIn(0, subtotalPaise)
        val discountedAmount = subtotalPaise - discount
        val safeRate = taxRateBps.coerceIn(0, 10_000)
        val tax = when {
            !taxEnabled || safeRate == 0 -> 0
            pricesIncludeTax -> discountedAmount * safeRate / (10_000 + safeRate)
            else -> discountedAmount * safeRate / 10_000
        }
        val total = if (taxEnabled && !pricesIncludeTax) discountedAmount + tax else discountedAmount
        return BillTotals(subtotalPaise, discount, tax, total)
    }
}
