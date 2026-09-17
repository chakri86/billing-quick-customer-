package com.quickcustomer.billing.domain

import java.text.Normalizer
import java.util.Locale

/** Strict whole-rupee dictation for the existing product form. Never removes arbitrary words. */
object VoiceProductInput {
    private val tens = mapOf(
        "twenty" to 20L, "thirty" to 30L, "forty" to 40L, "fifty" to 50L,
        "sixty" to 60L, "seventy" to 70L, "eighty" to 80L, "ninety" to 90L,
        "ఇరవై" to 20L, "ముప్పై" to 30L, "నలభై" to 40L, "యాభై" to 50L,
        "అరవై" to 60L, "డెబ్బై" to 70L, "ఎనభై" to 80L, "తొంభై" to 90L,
        "iravai" to 20L, "muppai" to 30L, "nalabhai" to 40L, "yabhai" to 50L,
        "బీస్" to 20L, "बीस" to 20L, "तीस" to 30L, "चालीस" to 40L,
        "पचास" to 50L, "साठ" to 60L, "सत्तर" to 70L, "अस्सी" to 80L, "नब्बे" to 90L,
        "tees" to 30L, "chalis" to 40L, "chaalis" to 40L, "pachas" to 50L,
        "पच्चीस" to 25L, "pachis" to 25L, "pachees" to 25L,
        "पैंतीस" to 35L, "पैंतालीस" to 45L, "पचपन" to 55L,
        "पचहत्तर" to 75L, "pachhattar" to 75L
    )
    private val hundreds = setOf("hundred", "వంద", "వందల", "vanda", "vandala", "सौ", "sau")
    private val thousands = setOf("thousand", "వెయ్యి", "వేలు", "veyi", "velu", "हजार", "हज़ार", "hazar")

    fun priceRupees(transcript: String): Long? {
        val clean = Normalizer.normalize(transcript, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .map { if (it.isDigit()) ('0'.code + Character.digit(it, 10)).toChar() else it }
            .joinToString("").trim().removePrefix("₹").trim()
            .replace(Regex("\\s+(rupees?|rs|రూపాయలు|రూపాయల|రూపాయి|रुपये|रुपए|रुपया)$"), "")
            .trim()
        val value = if (Regex("[0-9]+").matches(clean)) clean.toLongOrNull()
            else parseWords(clean.split(Regex("\\s+")))
        return value?.takeIf { it > 0 && it <= Long.MAX_VALUE / 100 }
    }

    private fun small(tokens: List<String>): Long? {
        if (tokens.size == 1) return tens[tokens[0]] ?: VoiceBillingParser.quantityOf(tokens[0])?.toLong()
        if (tokens.size == 2) {
            val ten = tens[tokens[0]] ?: return null
            val unit = VoiceBillingParser.quantityOf(tokens[1]) ?: return null
            if (ten % 10 == 0L && unit in 1..9) return ten + unit
        }
        return null
    }

    private fun underThousand(tokens: List<String>): Long? {
        val index = tokens.indexOfFirst { it in hundreds }
        if (index < 0) return small(tokens)
        val leading = if (index == 0) 1 else small(tokens.take(index)) ?: return null
        if (leading !in 1..9 || tokens.drop(index + 1).any { it in hundreds }) return null
        val rest = tokens.drop(index + 1).let { if (it.firstOrNull() == "and") it.drop(1) else it }
        if (tokens.lastOrNull() == "and") return null
        return leading * 100 + if (rest.isEmpty()) 0 else small(rest)?.takeIf { it < 100 } ?: return null
    }

    private fun parseWords(tokens: List<String>): Long? {
        if (tokens.isEmpty() || tokens.any { it.isEmpty() || it.any(Char::isDigit) }) return null
        val index = tokens.indexOfFirst { it in thousands }
        if (index < 0) return underThousand(tokens)
        val leading = if (index == 0) 1 else small(tokens.take(index)) ?: return null
        if (leading !in 1..99 || tokens.drop(index + 1).any { it in thousands }) return null
        val rest = tokens.drop(index + 1)
        return leading * 1000 + if (rest.isEmpty()) 0 else underThousand(rest) ?: return null
    }
}
