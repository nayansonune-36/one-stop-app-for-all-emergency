package com.sentinel.app.feature.bot.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String, // "user" or "assistant"
    val content: String,
    val isCritical: Boolean = false
)

@Serializable
data class ChatRequest(
    val lang: String, // "en" or "hi"
    val messages: List<ChatMessageItem>
)

@Serializable
data class ChatMessageItem(
    val role: String,
    val content: String
)

@Serializable
data class ChatResponse(
    val reply: String,
    val critical: Boolean
)
