package com.quickcustomer.billing.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceProductInputTest {
    @Test fun nativeDigitsAndCurrencySuffixesAreWholeRupees() {
        listOf("20", "₹20", "౨౦", "२०", "20 rupees", "ఇరవై రూపాయలు", "बीस रुपये").forEach {
            assertEquals(it, 20L, VoiceProductInput.priceRupees(it))
        }
    }

    @Test fun commonWordsAcrossLanguagesHaveTheSameValue() {
        listOf("twenty", "iravai", "ఇరవై", "bees", "बीस").forEach {
            assertEquals(it, 20L, VoiceProductInput.priceRupees(it))
        }
        listOf("thirty five", "ముప్పై ఐదు", "muppai aidu", "पैंतीस").forEach {
            assertEquals(it, 35L, VoiceProductInput.priceRupees(it))
        }
        assertEquals(25L, VoiceProductInput.priceRupees("पच्चीस"))
    }

    @Test fun hundredsAndThousandsPreserveAmounts() {
        listOf("one hundred twenty", "one hundred and twenty", "వంద ఇరవై", "एक सौ बीस").forEach {
            assertEquals(it, 120L, VoiceProductInput.priceRupees(it))
        }
        assertEquals(1250L, VoiceProductInput.priceRupees("one thousand two hundred fifty"))
        assertEquals(200L, VoiceProductInput.priceRupees("రెండు వందల"))
        assertEquals(2000L, VoiceProductInput.priceRupees("दो हजार"))
    }

    @Test fun invalidOrAmbiguousSpeechNeverBecomesAPrice() {
        listOf("", "zero", "0", "-20", "−20", "twenty five fifty", "twenty thirty",
            "20 tea", "20 or 30", "twenty rupees fifty paise", "20.50", "1,200",
            "one hundred and", "one hundred hundred", "two hundred one hundred",
            "price twenty", "20 30", "one thousand thousand", "999999999999999999999").forEach {
            assertNull(it, VoiceProductInput.priceRupees(it))
        }
    }

    @Test fun rupeeToPaiseMultiplicationCannotOverflow() {
        assertEquals(Long.MAX_VALUE / 100, VoiceProductInput.priceRupees((Long.MAX_VALUE / 100).toString()))
        assertNull(VoiceProductInput.priceRupees((Long.MAX_VALUE / 100 + 1).toString()))
    }
}
