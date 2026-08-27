package com.chaiduniya.billing.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BillingCalculatorTest {
    @Test
    fun discountIsAppliedWithoutTax() {
        val totals = BillingCalculator.calculate(
            subtotalPaise = 10_000,
            requestedDiscountPaise = 1_500,
            taxEnabled = false,
            taxRateBps = 1_800,
            pricesIncludeTax = true
        )

        assertEquals(10_000, totals.subtotalPaise)
        assertEquals(1_500, totals.discountPaise)
        assertEquals(0, totals.taxPaise)
        assertEquals(8_500, totals.totalPaise)
    }

    @Test
    fun exclusiveTaxIsAddedAfterDiscount() {
        val totals = BillingCalculator.calculate(
            subtotalPaise = 10_000,
            requestedDiscountPaise = 0,
            taxEnabled = true,
            taxRateBps = 1_800,
            pricesIncludeTax = false
        )

        assertEquals(1_800, totals.taxPaise)
        assertEquals(11_800, totals.totalPaise)
    }

    @Test
    fun inclusiveTaxIsExtractedFromDisplayedPrice() {
        val totals = BillingCalculator.calculate(
            subtotalPaise = 11_800,
            requestedDiscountPaise = 0,
            taxEnabled = true,
            taxRateBps = 1_800,
            pricesIncludeTax = true
        )

        assertEquals(1_800, totals.taxPaise)
        assertEquals(11_800, totals.totalPaise)
    }

    @Test
    fun discountCannotMakeTotalNegative() {
        val totals = BillingCalculator.calculate(
            subtotalPaise = 2_000,
            requestedDiscountPaise = 9_999,
            taxEnabled = false,
            taxRateBps = 0,
            pricesIncludeTax = true
        )

        assertEquals(2_000, totals.discountPaise)
        assertEquals(0, totals.totalPaise)
    }
}
