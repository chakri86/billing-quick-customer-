package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.ProductEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceBillingParserTest {
    private val roseProducts = listOf(
        ProductEntity("rose-milk", "Milk", "Rose Milk", 1500, 0),
        ProductEntity("rose-flavoured", "Flavoured Milk", "Rose Milk", 5500, 1),
        ProductEntity("rose-large", "Large Drinks", "Rose Milk", 7500, 2)
    )

    @Test
    fun duplicateNamesOfferEveryProductAndRequireAnExplicitSelection() {
        listOf("rose milk", "two rose milk", "rose milk two").forEach { phrase ->
            val result = VoiceBillingParser.parse(phrase, roseProducts)
            assertTrue(result.items.isEmpty())
            assertEquals(3, result.choices.single().products.size)
            assertEquals(null, VoiceBillingParser.resolve(result, emptyMap()))
            assertEquals(null, VoiceBillingParser.resolve(result, mapOf(0 to "not-a-product")))
            val resolved = VoiceBillingParser.resolve(result, mapOf(0 to "rose-flavoured"))!!
            assertEquals("rose-flavoured", resolved.single().productId)
            assertEquals(if (phrase == "rose milk") 1 else 2, resolved.single().quantity)
            assertEquals(5500L, result.choices.single().products[1].pricePaise)
        }
    }

    @Test
    fun clearItemsAreRetainedButCannotConfirmUntilAllChoicesAreResolved() {
        val result = VoiceBillingParser.parse("two coffee and rose milk and rose milk three", products + roseProducts)
        assertEquals(2, result.items.single().quantity)
        assertEquals(2, result.choices.size)
        assertEquals(null, VoiceBillingParser.resolve(result, mapOf(0 to "rose-milk")))
        val resolved = VoiceBillingParser.resolve(result, mapOf(0 to "rose-milk", 1 to "rose-milk"))!!
        assertEquals(4, resolved.single { it.productId == "rose-milk" }.quantity)
        assertEquals(2, resolved.single { it.productId == "bru-coffee" }.quantity)
    }

    @Test
    fun duplicateSelectionCannotExceedQuantityLimitOrIncludeInactiveProducts() {
        val result = VoiceBillingParser.parse("99 rose milk and rose milk", roseProducts)
        assertEquals(null, VoiceBillingParser.resolve(result, mapOf(0 to "rose-milk", 1 to "rose-milk")))
        val activeOnly = VoiceBillingParser.parse("rose milk", listOf(roseProducts[0],
            roseProducts[1].copy(isActive = false), roseProducts[2].copy(isDeleted = true)))
        assertTrue(activeOnly.choices.isEmpty())
        assertEquals("rose-milk", activeOnly.items.single().productId)
    }

    @Test
    fun quantityAmbiguityStillBlocksEvenWithDuplicateNames() {
        val result = VoiceBillingParser.parse("rose milk two coffee", products + roseProducts)
        assertTrue(result.items.isEmpty())
        assertTrue(result.choices.isEmpty())
        assertEquals(null, VoiceBillingParser.resolve(result, emptyMap()))
    }

    @Test
    fun NearbySpellingRequiresTapEvenWhenOnlyOneSuggestionExists() {
        val result = VoiceBillingParser.parse("rose mil two", roseProducts.take(1))
        assertTrue(result.items.isEmpty())
        assertEquals(2, result.choices.single().quantity)
        assertEquals(null, VoiceBillingParser.resolve(result, emptyMap()))
        assertEquals(2, VoiceBillingParser.resolve(result, mapOf(0 to "rose-milk"))!!.single().quantity)
        assertTrue(VoiceBillingParser.parse("pizza", roseProducts).choices.isEmpty())
    }

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

    @Test
    fun productAloneDefaultsToOne() {
        listOf("coffee", "కాఫీ", "कॉफी", "please add coffee").forEach {
            assertEquals(it, listOf(VoiceCartItem("bru-coffee", "BRU Coffee", 1)), VoiceBillingParser.parse(it, products).items)
        }
    }

    @Test
    fun quantitiesCanBeBeforeOrAfterTheNameInAllSupportedLanguages() {
        listOf("two tea", "tea two", "tea 2", "రెండు టీ", "టీ రెండు", "టీ ౨", "दो चाय", "चाय दो", "tea rendu", "చాయ్ २").forEach {
            assertEquals(it, listOf(VoiceCartItem("dum-tea", "Dum Tea", 2)), VoiceBillingParser.parse(it, products).items)
        }
    }

    @Test
    fun multipleItemsCanMixDefaultPrefixAndSuffixQuantities() {
        listOf("two tea and coffee", "tea two and coffee", "two tea coffee", "tea two coffee one", "tea two, coffee").forEach {
            assertEquals(it, listOf(VoiceCartItem("dum-tea", "Dum Tea", 2), VoiceCartItem("bru-coffee", "BRU Coffee", 1)),
                VoiceBillingParser.parse(it, products).items)
        }
        assertEquals(listOf(VoiceCartItem("dum-tea", "Dum Tea", 2), VoiceCartItem("bru-coffee", "BRU Coffee", 3)),
            VoiceBillingParser.parse("tea two coffee three", products).items)
        assertEquals(2, VoiceBillingParser.parse("coffee coffee", products).items.single().quantity)
    }

    @Test
    fun ambiguousQuantityAttachmentIsRejected() {
        val result = VoiceBillingParser.parse("tea two coffee", products)
        assertTrue(result.items.isEmpty())
        assertTrue(result.notes.any { it.contains("More than one") })
        listOf("two tea three", "tea zero", "tea 0", "tea 100", "tea -2", "tea 1.5").forEach {
            assertTrue(it, VoiceBillingParser.parse(it, products).items.isEmpty())
        }
    }

    @Test
    fun reportedBrewAndDumptyTranscriptionsMatchActualProducts() {
        val menu = products + ProductEntity("bru-tea", "Teas", "BRU Tea", 1500, 2)
        listOf("brew tea", "two brew tea", "brew tea two").forEach {
            val result = VoiceBillingParser.parse(it, menu)
            assertEquals("bru-tea", result.items.single().productId)
            assertEquals(if (it == "brew tea") 1 else 2, result.items.single().quantity)
            assertTrue(result.notes.any { note -> note.contains("BRU Tea") })
        }
        listOf("dumpty", "two dumpty", "dumpty two").forEach {
            val result = VoiceBillingParser.parse(it, menu)
            assertEquals("dum-tea", result.items.single().productId)
            assertEquals(if (it == "dumpty") 1 else 2, result.items.single().quantity)
        }
    }

    @Test
    fun correctionsRequireTheirTargetAndNeverCreateProducts() {
        assertTrue(VoiceBillingParser.parse("brew tea", products).items.isEmpty())
        assertTrue(VoiceBillingParser.parse("dumpty", products.filter { it.id != "dum-tea" }).items.isEmpty())
        assertTrue(VoiceBillingParser.parse("dumpty", products.map { it.copy(isActive = false) }).items.isEmpty())
    }

    @Test
    fun realCatalogNamesTakePrecedenceOverCorrections() {
        val menu = products + listOf(
            ProductEntity("bru-tea", "Teas", "BRU Tea", 1500, 2),
            ProductEntity("brew-tea", "Teas", "Brew Tea", 2500, 3),
            ProductEntity("dumpty-snack", "Snacks", "Dumpty", 3000, 4)
        )
        assertEquals("brew-tea", VoiceBillingParser.parse("brew tea", menu).items.single().productId)
        assertEquals("dumpty-snack", VoiceBillingParser.parse("dumpty", menu).items.single().productId)
        assertTrue(VoiceBillingParser.parse("bru", menu).items.isEmpty())
    }

    @Test
    fun unknownWordsCannotBeDroppedToMakeAPartialOrder() {
        listOf("coffee and pizza", "coffee pizza", "two unknown tea", "remove coffee", "brew unknown tea").forEach {
            assertTrue(it, VoiceBillingParser.parse(it, products).items.isEmpty())
        }
    }

    @Test
    fun longerCatalogNamesStayOneItem() {
        assertEquals(listOf(VoiceCartItem("black-coffee", "Black Coffee", 2)), VoiceBillingParser.parse("black coffee two", products).items)
        assertEquals(listOf(VoiceCartItem("corn-samosa", "Corn Samosa", 1)), VoiceBillingParser.parse("corn samosa", products).items)
    }

    @Test
    fun longUtterancesAreBounded() {
        assertTrue(VoiceBillingParser.parse(List(65) { "coffee" }.joinToString(" "), products).items.isEmpty())
    }
}
