package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceBillingParserTest {
    private val products = listOf(
        ProductEntity("dum-tea", "Teas", "Dum Tea", 1_200, 0),
        ProductEntity("ginger-tea", "Teas", "Ginger Tea", 1_500, 1),
        ProductEntity("bru-coffee", "Coffees", "BRU Coffee", 2_000, 0),
        ProductEntity("black-coffee", "Coffees", "Black Coffee", 2_000, 1),
        ProductEntity("samosa", "Snacks", "Samosa (2 pcs)", 1_500, 0),
        ProductEntity("corn-samosa", "Snacks", "Corn Samosa", 1_500, 1)
    )

    @Test
    fun genericTeaAndCoffeeUseFirstActiveCategoryProducts() {
        val result = VoiceBillingParser.parse("two tea two coffee", products)

        assertEquals(
            listOf(
                VoiceCartItem("dum-tea", "Dum Tea", 2),
                VoiceCartItem("bru-coffee", "BRU Coffee", 2)
            ),
            result.items
        )
    }

    @Test
    fun exactAndPrefixNamesAreRecognized() {
        val result = VoiceBillingParser.parse("3 black coffee and one samosa", products)

        assertEquals(3, result.items.first { it.productId == "black-coffee" }.quantity)
        assertEquals(1, result.items.first { it.productId == "samosa" }.quantity)
    }

    @Test
    fun unknownProductsAreReportedAndNeverAdded() {
        val result = VoiceBillingParser.parse("two pizza", products)

        assertTrue(result.items.isEmpty())
        assertTrue(result.notes.any { it.contains("No active product matched") })
    }
}
