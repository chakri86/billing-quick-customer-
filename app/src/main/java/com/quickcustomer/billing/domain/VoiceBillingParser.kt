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
        "brew" to "bru", "dumpty" to "dum tea",
        "టీ" to "tea", "టీలు" to "tea", "చాయ్" to "tea", "చాయ" to "tea", "चाय" to "tea", "टी" to "tea",
        "coffees" to "coffee", "కాఫీ" to "coffee", "కాఫీలు" to "coffee", "कॉफी" to "coffee", "कॉफ़ी" to "coffee", "काफी" to "coffee",
        "samosas" to "samosa", "సమోసా" to "samosa", "సమోసాలు" to "samosa", "समोसा" to "samosa", "समोसे" to "samosa",
        "అల్లం" to "ginger", "అల్లము" to "ginger", "అద్రక్" to "ginger", "अदरक" to "ginger", "adrak" to "ginger", "allam" to "ginger",
        "ఇలాచి" to "elaichi", "యాలకుల" to "elaichi", "इलायची" to "elaichi",
        "మసాలా" to "masala", "मसाला" to "masala",
        "డమ్" to "dum", "दम" to "dum", "బ్రూ" to "bru", "ब्रू" to "bru",
        "బ్లాక్" to "black", "ब्लैक" to "black", "కార్న్" to "corn", "कॉर्न" to "corn"
    )

    private data class HeardItem(val product: ProductEntity, val quantity: Int, val spoken: String)
    private data class NameMatch(val end: Int, val products: List<ProductEntity>, val spoken: String)

    fun parse(transcript: String, products: List<ProductEntity>): VoiceBillingParseResult {
        val activeProducts = products.filter { it.isActive && !it.isDeleted }
            .sortedWith(compareBy<ProductEntity> { it.sortOrder }.thenBy { it.name }.thenBy { it.id })
        fun rejected(message: String) = VoiceBillingParseResult(transcript.trim(), emptyList(), listOf(message))
        val allTokens = normalize(transcript).split(' ').filter(String::isNotBlank)
        if (allTokens.size > 64) return rejected("Please split this into shorter orders.")
        if (Regex("[-−]\\s*\\p{Nd}|\\p{Nd}[.,]\\p{Nd}").containsMatchIn(transcript) ||
            allTokens.any { it.all(Char::isDigit) && quantityOf(it) == null }) {
            return rejected("Use whole quantities from 1 to 99.")
        }
        // Explicit conjunctions keep quantities attached to their own clause.
        val clauses = transcript.split(Regex("[,;\\n]+|\\s+(?:and|aur|और|మరియు|inka|ఇంకా|mariyu)\\s+", RegexOption.IGNORE_CASE))
            .map { clause -> normalize(clause).split(' ').filter { it.isNotBlank() && it !in fillerWords } }
            .filter { it.isNotEmpty() }
        if (clauses.isEmpty()) return rejected("No products were recognized.")
        val parsed = mutableListOf<HeardItem>()
        for (tokens in clauses) {
            val possibilities = parseClause(tokens, activeProducts)
            if (possibilities.isEmpty()) return rejected(
                "No active product matched the complete phrase '${tokens.joinToString(" ")}', or its quantity was unclear. Try 'coffee', 'two tea' or 'tea two'."
            )
            if (possibilities.size > 1) return rejected(
                "More than one product or quantity interpretation is possible. Use the full product name and separate items with 'and', for example 'tea two and coffee one'."
            )
            parsed += possibilities.single()
        }
        val matched = linkedMapOf<String, VoiceCartItem>()
        val notes = mutableListOf<String>()
        parsed.forEach { heard ->
            val product = heard.product
            val total = (matched[product.id]?.quantity ?: 0) + heard.quantity
            if (total > 99) return rejected("Quantity exceeds 99. Please split the order.")
            matched[product.id] = VoiceCartItem(product.id, product.name, total)
            if (normalize(product.name) != heard.spoken) notes += "'${heard.spoken}' matched ${product.name}."
        }
        return VoiceBillingParseResult(transcript.trim(), matched.values.toList(), notes.distinct())
    }

    private fun parseClause(tokens: List<String>, products: List<ProductEntity>): List<List<HeardItem>> {
        val memo = mutableMapOf<Int, List<List<HeardItem>>>()
        val names = mutableMapOf<Int, NameMatch?>()
        fun nameAt(start: Int): NameMatch? = names.getOrPut(start) {
            // Longest matching name prevents 'black coffee' becoming two generic items.
            var end = start
            while (end < tokens.size && !isQuantityToken(tokens[end])) end++
            (end downTo start + 1).firstNotNullOfOrNull { stop ->
                val spoken = tokens.subList(start, stop).joinToString(" ")
                val matches = findProducts(spoken, products, allowPartial = stop == end)
                if (matches.isEmpty()) null else NameMatch(stop, matches, spoken)
            }
        }
        fun signature(items: List<HeardItem>): Map<String, Int> =
            items.groupBy { it.product.id }.mapValues { (_, entries) -> entries.sumOf { it.quantity } }
        fun walk(start: Int): List<List<HeardItem>> {
            if (start == tokens.size) return listOf(emptyList())
            memo[start]?.let { return it }
            val prefix = quantityOf(tokens[start])
            val name = nameAt(if (prefix == null) start else start + 1)
                ?: return emptyList<List<HeardItem>>().also { memo[start] = it }
            val quantities = mutableListOf((prefix ?: 1) to name.end)
            if (prefix == null && name.end < tokens.size) {
                quantityOf(tokens[name.end])?.let { quantities += it to name.end + 1 }
            }
            val results = mutableListOf<List<HeardItem>>()
            for ((quantity, next) in quantities) {
                for (product in name.products) {
                    for (tail in walk(next)) {
                        val candidate = listOf(HeardItem(product, quantity, name.spoken)) + tail
                        if (results.none { signature(it) == signature(candidate) }) results += candidate
                        // Two distinct carts are sufficient to establish ambiguity.
                        if (results.size == 2) return results.also { memo[start] = it }
                    }
                }
            }
            return results.also { memo[start] = it }
        }
        return walk(0)
    }

    private fun findProducts(spokenName: String, products: List<ProductEntity>, allowPartial: Boolean): List<ProductEntity> {
        // A real catalog name wins over a phonetic correction, e.g. an actual Brew Tea.
        products.filter { normalize(it.name) == spokenName }.takeIf { it.isNotEmpty() }?.let { return it }
        val spoken = canonicalName(spokenName)
        if (spoken.isBlank()) return emptyList()
        products.filter { canonicalName(it.name) == spoken }.takeIf { it.isNotEmpty() }?.let { return it }
        if (spokenName.split(' ').any { it == "brew" || it == "dumpty" }) return emptyList()
        // Preserve established generic category defaults; the review names the exact product.
        products.firstOrNull { canonicalName(it.category).removeSuffix("s") == spoken }?.let { return listOf(it) }
        if (!allowPartial) return emptyList()
        products.filter { canonicalName(it.name).startsWith("$spoken ") }.takeIf { it.isNotEmpty() }?.let { return it }
        val requestedCounts = spoken.split(' ').groupingBy { it }.eachCount()
        return products.filter { product ->
            val availableCounts = canonicalName(product.name).split(' ').groupingBy { it }.eachCount()
            requestedCounts.all { (word, count) -> (availableCounts[word] ?: 0) >= count }
        }
    }

    private fun canonicalName(value: String): String = normalize(value).split(' ')
        .joinToString(" ") { productAliases[it] ?: it }

    private fun isQuantityToken(token: String): Boolean =
        token.all(Char::isDigit) || token in numberWords

    internal fun quantityOf(token: String): Int? =
        token.toIntOrNull()?.takeIf { it in 1..99 } ?: numberWords[token]

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFC)
        .lowercase(Locale.ROOT)
        .map { char -> if (char.isDigit()) ('0'.code + Character.digit(char, 10)).toChar() else char }
        .joinToString("")
        .replace(Regex("[\\p{Cf}]"), "")
        .replace(Regex("[^\\p{L}\\p{M}0-9]+"), " ")
        .trim()
}
