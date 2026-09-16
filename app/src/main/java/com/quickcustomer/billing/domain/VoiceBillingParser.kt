package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.ProductEntity
import java.util.Locale
import java.text.Normalizer

data class VoiceCartItem(
    val productId: String,
    val productName: String,
    val quantity: Int
)

data class VoiceBillingParseResult(
    val transcript: String,
    val items: List<VoiceCartItem>,
    val notes: List<String>
)

object VoiceBillingParser {
    private val numberWords = mapOf(
        "a" to 1,
        "an" to 1,
        "one" to 1,
        "two" to 2,
        "three" to 3,
        "four" to 4,
        "five" to 5,
        "six" to 6,
        "seven" to 7,
        "eight" to 8,
        "nine" to 9,
        "ten" to 10,
        "eleven" to 11,
        "twelve" to 12,
        "thirteen" to 13,
        "fourteen" to 14,
        "fifteen" to 15,
        "sixteen" to 16,
        "seventeen" to 17,
        "eighteen" to 18,
        "nineteen" to 19,
        "twenty" to 20,
        "oka" to 1, "okati" to 1, "ఒక" to 1, "ఒకటి" to 1,
        "rendu" to 2, "రెండు" to 2,
        "moodu" to 3, "mudu" to 3, "మూడు" to 3,
        "nalugu" to 4, "నాలుగు" to 4,
        "aidu" to 5, "ఐదు" to 5,
        "aaru" to 6, "ఆరు" to 6,
        "edu" to 7, "yedu" to 7, "ఏడు" to 7,
        "enimidi" to 8, "ఎనిమిది" to 8,
        "tommidi" to 9, "తొమ్మిది" to 9,
        "padi" to 10, "పది" to 10,
        "padakondu" to 11, "పదకొండు" to 11,
        "pannendu" to 12, "పన్నెండు" to 12,
        "padamoodu" to 13, "పదమూడు" to 13,
        "padnalugu" to 14, "పద్నాలుగు" to 14,
        "padihenu" to 15, "పదిహేను" to 15,
        "padaharu" to 16, "పదహారు" to 16,
        "padihedu" to 17, "పదిహేడు" to 17,
        "paddenimidi" to 18, "పద్దెనిమిది" to 18,
        "pantommidi" to 19, "పంతొమ్మిది" to 19,
        "iravai" to 20, "iravayi" to 20, "ఇరవై" to 20,
        "ek" to 1, "एक" to 1, "do" to 2, "दो" to 2,
        "teen" to 3, "तीन" to 3, "char" to 4, "chaar" to 4, "चार" to 4,
        "panch" to 5, "paanch" to 5, "पांच" to 5, "पाँच" to 5,
        "chhe" to 6, "chhah" to 6, "छह" to 6,
        "saat" to 7, "सात" to 7, "aath" to 8, "आठ" to 8,
        "nau" to 9, "नौ" to 9, "das" to 10, "दस" to 10,
        "gyarah" to 11, "ग्यारह" to 11, "barah" to 12, "बारह" to 12,
        "terah" to 13, "तेरह" to 13, "chaudah" to 14, "चौदह" to 14,
        "pandrah" to 15, "पंद्रह" to 15, "पन्द्रह" to 15,
        "solah" to 16, "सोलह" to 16, "satrah" to 17, "सत्रह" to 17,
        "atharah" to 18, "अठारह" to 18, "unnis" to 19, "उन्नीस" to 19,
        "bees" to 20, "बीस" to 20
    )
    private val fillerWords = setOf(
        "add", "and", "please", "cup", "cups", "piece", "pieces", "of",
        "aur", "और", "ఇంకా", "మరియు", "inka", "mariyu",
        "ivvandi", "ఇవ్వండి", "kavali", "కావాలి", "chahiye", "चाहिए",
        "dijiye", "दीजिए", "కప్పు", "కప్పులు", "कप"
    )
    // Whole-token aliases preserve modifiers (e.g. ginger tea vs plain tea).
    private val productAliases = mapOf(
        "chai" to "tea", "chaay" to "tea", "teas" to "tea",
        "టీ" to "tea", "టీలు" to "tea", "చాయ్" to "tea", "చాయ" to "tea", "चाय" to "tea", "टी" to "tea",
        "coffees" to "coffee", "కాఫీ" to "coffee", "కాఫీలు" to "coffee", "कॉफी" to "coffee", "कॉफ़ी" to "coffee", "काफी" to "coffee",
        "samosas" to "samosa", "సమోసా" to "samosa", "సమోసాలు" to "samosa", "समोसा" to "samosa", "समोसे" to "samosa",
        "అల్లం" to "ginger", "అల్లము" to "ginger", "అద్రక్" to "ginger", "अदरक" to "ginger", "adrak" to "ginger", "allam" to "ginger",
        "ఇలాచి" to "elaichi", "యాలకుల" to "elaichi", "इलायची" to "elaichi",
        "మసాలా" to "masala", "मसाला" to "masala",
        "డమ్" to "dum", "दम" to "dum", "బ్రూ" to "bru", "ब्रू" to "bru",
        "బ్లాక్" to "black", "ब्लैक" to "black", "కార్న్" to "corn", "कॉर्न" to "corn"
    )

    fun parse(transcript: String, products: List<ProductEntity>): VoiceBillingParseResult {
        val activeProducts = products.filter { it.isActive && !it.isDeleted }
            .sortedWith(compareBy<ProductEntity> { it.sortOrder }.thenBy { it.name })
        val tokens = normalize(transcript).split(' ').filter(String::isNotBlank)
        if (Regex("[-−]\\s*\\p{Nd}|\\p{Nd}[.,]\\p{Nd}").containsMatchIn(transcript) ||
            tokens.any { it.all(Char::isDigit) && quantityOf(it) == null }) {
            return VoiceBillingParseResult(transcript.trim(), emptyList(), listOf("Use whole quantities from 1 to 99."))
        }
        val segments = mutableListOf<Pair<Int, String>>()
        val notes = mutableListOf<String>()
        var index = 0

        while (index < tokens.size) {
            while (index < tokens.size && tokens[index] in fillerWords) index++
            if (index >= tokens.size) break

            val quantity = quantityOf(tokens[index])
            if (quantity == null) {
                notes += "Could not understand '${tokens[index]}'. Say a quantity before each product."
                index++
                continue
            }
            index++
            val productWords = mutableListOf<String>()
            while (index < tokens.size && !isQuantityToken(tokens[index])) {
                if (tokens[index] !in fillerWords) productWords += tokens[index]
                index++
            }
            if (productWords.isEmpty()) {
                notes += "No product was heard after quantity $quantity."
            } else {
                segments += quantity to productWords.joinToString(" ")
            }
        }

        val matched = linkedMapOf<String, VoiceCartItem>()
        segments.forEach { (quantity, spokenName) ->
            val match = findProduct(spokenName, activeProducts)
            if (match == null) {
                notes += "No active product matched '$spokenName'."
            } else {
                val previous = matched[match.id]
                val total = (previous?.quantity ?: 0) + quantity
                if (total > 99) {
                    return VoiceBillingParseResult(transcript.trim(), emptyList(), listOf("Quantity exceeds 99. Please split the order."))
                }
                matched[match.id] = VoiceCartItem(
                    productId = match.id,
                    productName = match.name,
                    quantity = total
                )
                if (normalize(match.name) != spokenName) {
                    notes += "'$spokenName' matched ${match.name}."
                }
            }
        }

        if (segments.isEmpty() && notes.isEmpty()) notes += "No products were recognized."
        return VoiceBillingParseResult(transcript.trim(), matched.values.toList(), notes.distinct())
    }

    private fun findProduct(spokenName: String, products: List<ProductEntity>): ProductEntity? {
        // Exact catalog names take precedence over translations and category defaults.
        products.firstOrNull { normalize(it.name) == spokenName }?.let { return it }
        val spoken = canonicalName(spokenName)
        if (spoken.isBlank()) return null
        products.firstOrNull { canonicalName(it.name) == spoken }?.let { return it }
        products.firstOrNull { canonicalName(it.category).removeSuffix("s") == spoken }?.let { return it }
        products.firstOrNull { canonicalName(it.name).startsWith("$spoken ") }?.let { return it }
        return products.firstOrNull { product ->
            canonicalName(product.name).split(' ').containsAll(spoken.split(' '))
        }
    }

    private fun canonicalName(value: String): String = normalize(value).split(' ')
        .joinToString(" ") { productAliases[it] ?: it }

    private fun isQuantityToken(token: String): Boolean =
        token.all(Char::isDigit) || token in numberWords

    private fun quantityOf(token: String): Int? =
        token.toIntOrNull()?.takeIf { it in 1..99 } ?: numberWords[token]

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
        .map { char -> if (char.isDigit()) ('0'.code + Character.digit(char, 10)).toChar() else char }
        .joinToString("")
        .replace(Regex("[\\p{Cf}]"), "")
        .replace(Regex("[^\\p{L}\\p{M}0-9]+"), " ")
        .trim()
}
