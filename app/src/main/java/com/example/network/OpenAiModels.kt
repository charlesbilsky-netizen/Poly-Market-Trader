package com.example.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val messages: List<OpenAiMessage>,
    val model: String = "gpt-4o-mini",
    val stream: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val choices: List<OpenAiChoice>?
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    val message: OpenAiMessage?
)
