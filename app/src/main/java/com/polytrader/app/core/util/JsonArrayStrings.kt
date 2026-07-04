package com.polytrader.app.core.util

import org.json.JSONArray

/**
 * The Gamma API returns several array fields as JSON-encoded *strings*, e.g.
 * `"outcomes": "[\"Yes\", \"No\"]"`. These helpers do the second decode.
 */
object JsonArrayStrings {

    fun parseStrings(encoded: String?): List<String> {
        if (encoded.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(encoded)
            List(arr.length()) { arr.optString(it, "") }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun parseDoubles(encoded: String?): List<Double> =
        parseStrings(encoded).mapNotNull { it.toDoubleOrNull() }
}
