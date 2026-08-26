package com.chaiduniya.billing.domain

import java.text.NumberFormat
import java.util.Locale

object Money {
    private val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    fun format(paise: Long): String = formatter.format(paise / 100.0)

    fun cartTotal(lines: Collection<Pair<Long, Int>>): Long =
        lines.sumOf { (unitPricePaise, quantity) -> unitPricePaise * quantity }
}
