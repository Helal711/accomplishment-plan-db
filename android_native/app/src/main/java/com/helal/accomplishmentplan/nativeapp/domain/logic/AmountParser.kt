package com.helal.accomplishmentplan.nativeapp.domain.logic

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Safely parses and formats task amounts stored in Firestore as String, Long, Int, or Double,
 * including comma-formatted numbers, "TK" / "৳" currency suffixes, and Bengali digits (০-৯).
 */
object AmountParser {

    private val integerFormatter: DecimalFormat
        get() = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))

    private val decimalFormatter: DecimalFormat
        get() = DecimalFormat("#,##0.##", DecimalFormatSymbols(Locale.US))

    fun rawToStorageString(raw: Any?): String {
        return when (raw) {
            null -> ""
            is String -> raw.trim()
            is Long -> raw.toString()
            is Int -> raw.toString()
            is Double -> {
                if (!raw.isFinite()) "" else formatNumberWithoutCommas(raw)
            }
            is Float -> {
                val d = raw.toDouble()
                if (!d.isFinite()) "" else formatNumberWithoutCommas(d)
            }
            is Number -> {
                val d = raw.toDouble()
                if (!d.isFinite()) "" else formatNumberWithoutCommas(d)
            }
            else -> raw.toString().trim()
        }
    }

    fun parseToDouble(raw: Any?): Double? {
        return when (raw) {
            null -> null
            is Long -> raw.toDouble()
            is Int -> raw.toDouble()
            is Double -> raw.takeIf { it.isFinite() }
            is Float -> raw.toDouble().takeIf { it.isFinite() }
            is Number -> raw.toDouble().takeIf { it.isFinite() }
            is String -> parseStringToDouble(raw)
            else -> parseStringToDouble(raw.toString())
        }
    }

    private fun parseStringToDouble(raw: String): Double? {
        val normalized = normalizeBengaliDigits(raw)
            .replace(",", "")
            .replace("TK", "", ignoreCase = true)
            .replace("BDT", "", ignoreCase = true)
            .replace("৳", "")
            .trim()
        if (normalized.isEmpty()) return null
        return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    fun formatForDisplay(raw: Any?, includeCurrencySuffix: Boolean = true): String {
        val numeric = parseToDouble(raw)
        if (numeric == null) {
            val fallback = rawToStorageString(raw)
            return fallback.ifBlank { if (includeCurrencySuffix) "0 TK" else "0" }
        }
        val formatted = if (numeric % 1.0 == 0.0) {
            integerFormatter.format(numeric.toLong())
        } else {
            decimalFormatter.format(numeric)
        }
        return if (includeCurrencySuffix) "$formatted TK" else formatted
    }

    fun searchTokens(raw: Any?): List<String> {
        val str = rawToStorageString(raw)
        val tokens = mutableSetOf<String>()
        if (str.isNotBlank()) {
            tokens += str
            tokens += normalizeBengaliDigits(str)
            tokens += str.replace(",", "")
        }
        val numeric = parseToDouble(raw)
        if (numeric != null) {
            val plain = formatNumberWithoutCommas(numeric)
            val comma = formatForDisplay(numeric, includeCurrencySuffix = false)
            tokens += plain
            tokens += comma
            tokens += "$plain tk"
            tokens += "$comma tk"
            tokens += "${plain}tk"
            tokens += "${comma}tk"
        }
        return tokens.toList()
    }

    fun normalizeBengaliDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '\u09E6'..'\u09EF') {
                builder.append(('0'.code + (ch.code - '\u09E6'.code)).toChar())
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    private fun formatNumberWithoutCommas(value: Double): String {
        return if (value % 1.0 == 0.0) {
            BigDecimal.valueOf(value).toBigInteger().toString()
        } else {
            BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
        }
    }
}
