package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GrokChatRequest(
    val messages: List<GrokMessage>,
    val model: String = "grok-beta",
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GrokMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GrokChatResponse(
    val choices: List<GrokChoice>?
)

@JsonClass(generateAdapter = true)
data class GrokChoice(
    val message: GrokMessage?
)
