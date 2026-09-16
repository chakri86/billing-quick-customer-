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

    @Test
    fun teluguHindiAndMixedOrdersMatchTheSameCart() {
        val samples = listOf(
            "two tea one coffee", "rendu tea oka coffee", "రెండు టీ ఒక కాఫీ",
            "do chai ek coffee", "दो चाय एक कॉफी", "two టీ ek कॉफी",
            "రెండు tea और one coffee", "౨ టీ ౧ కాఫీ", "२ चाय १ कॉफी",
            "రెండు టీలు ఒక కాఫీ ఇవ్వండి", "दो चाय और एक कॉफी दीजिए",
            "two teas and one coffee please"
        )
        samples.forEach { sentence ->
            val result = VoiceBillingParser.parse(sentence, products)
            assertEquals(sentence, listOf(
                VoiceCartItem("dum-tea", "Dum Tea", 2),
                VoiceCartItem("bru-coffee", "BRU Coffee", 1)
            ), result.items)
        }
    }

    @Test
    fun translatedModifiersPreserveTheSpecificProduct() {
        listOf("రెండు అల్లం టీ", "दो अदरक चाय", "rendu allam tea", "do adrak chai").forEach {
            assertEquals(listOf(VoiceCartItem("ginger-tea", "Ginger Tea", 2)), VoiceBillingParser.parse(it, products).items)
        }
    }

    @Test
    fun nativeCustomProductNamesArePreserved() {
        val native = ProductEntity("native", "Snacks", "మిర్చి బజ్జి", 1000, 0)
        assertEquals(listOf(VoiceCartItem("native", "మిర్చి బజ్జి", 3)),
            VoiceBillingParser.parse("మూడు మిర్చి బజ్జి", products + native).items)
    }

    @Test
    fun unknownNativeWordsAreNotDroppedToProduceFalseMatches() {
        assertTrue(VoiceBillingParser.parse("two తెలియని tea", products).items.isEmpty())
        assertTrue(VoiceBillingParser.parse("दो अज्ञात चाय", products).items.isEmpty())
    }

    @Test
    fun invalidAndOverflowQuantitiesAreNeverClampedIntoOrders() {
        listOf("0 tea", "100 tea", "-2 tea", "1.5 tea", "౦ టీ", "१०० चाय", "99 tea two tea", "twenty one tea", "two three coffee").forEach {
            assertTrue(it, VoiceBillingParser.parse(it, products).items.isEmpty())
        }
    }

    @Test
    fun inactiveAndDeletedProductsAreExcluded() {
        val unavailable = listOf(products[0].copy(isActive = false), products[1].copy(isDeleted = true))
        assertTrue(VoiceBillingParser.parse("రెండు టీ", unavailable).items.isEmpty())
    }

    @Test
    fun aliasesDoNotRewritePartsOfCustomNames() {
        val chair = ProductEntity("chair", "Other", "Chair", 1000, 0)
        assertEquals("chair", VoiceBillingParser.parse("one chair", listOf(chair)).items.single().productId)
    }
}
