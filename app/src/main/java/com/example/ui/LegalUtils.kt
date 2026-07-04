package com.example.ui

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

object LegalUtils {
    fun loadLegalDocument(context: Context, filename: String): String {
        return try {
            val inputStream = context.assets.open("legal/$filename")
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.readText()
        } catch (e: Exception) {
            "Error loading document."
        }
    }
}
