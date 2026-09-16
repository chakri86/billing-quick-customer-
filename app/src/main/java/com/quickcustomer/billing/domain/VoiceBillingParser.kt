package com.quickcustomer.billing.domain

import com.quickcustomer.billing.data.ProductEntity
import java.util.Locale

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
        "twenty" to 20
    )
    private val fillerWords = setOf("add", "and", "please", "cup", "cups", "piece", "pieces", "of")

    fun parse(transcript: String, products: List<ProductEntity>): VoiceBillingParseResult {
        val activeProducts = products.filter { it.isActive && !it.isDeleted }
            .sortedWith(compareBy<ProductEntity> { it.sortOrder }.thenBy { it.name })
        val tokens = normalize(transcript).split(' ').filter(String::isNotBlank)
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
            while (index < tokens.size && quantityOf(tokens[index]) == null) {
                if (tokens[index] !in fillerWords) productWords += tokens[index]
                index++
            }
            if (productWords.isEmpty()) {
                notes += "No product was heard after quantity $quantity."
            } else {
                segments += quantity.coerceIn(1, 99) to productWords.joinToString(" ")
            }
        }

        val matched = linkedMapOf<String, VoiceCartItem>()
        segments.forEach { (quantity, spokenName) ->
            val match = findProduct(spokenName, activeProducts)
            if (match == null) {
                notes += "No active product matched '$spokenName'."
            } else {
                val previous = matched[match.id]
                matched[match.id] = VoiceCartItem(
                    productId = match.id,
                    productName = match.name,
                    quantity = ((previous?.quantity ?: 0) + quantity).coerceAtMost(99)
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
        val normalizedSpoken = normalize(spokenName).replace("chai", "tea")
        val exact = products.firstOrNull { normalize(it.name).replace("chai", "tea") == normalizedSpoken }
        if (exact != null) return exact

        val categoryMatch = products.firstOrNull { product ->
            val category = normalize(product.category).removeSuffix("s").replace("chai", "tea")
            normalizedSpoken == category
        }
        if (categoryMatch != null) return categoryMatch

        val startsWith = products.firstOrNull { product ->
            normalize(product.name).replace("chai", "tea").startsWith("$normalizedSpoken ")
        }
        if (startsWith != null) return startsWith

        return products.firstOrNull { product ->
            val normalizedName = normalize(product.name).replace("chai", "tea")
            normalizedName.split(' ').containsAll(normalizedSpoken.split(' '))
        }
    }

    private fun quantityOf(token: String): Int? =
        token.toIntOrNull()?.takeIf { it in 1..99 } ?: numberWords[token]

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
