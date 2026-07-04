package com.example.ui

data class MarketSuggestion(
    val title: String,
    val confidence: Double,
    val resolutionSource: String,
    val tags: List<String>,
    val rationale: String
)
