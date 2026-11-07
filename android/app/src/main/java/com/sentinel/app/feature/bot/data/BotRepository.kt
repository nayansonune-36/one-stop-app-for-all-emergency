package com.sentinel.app.feature.bot.data

import com.sentinel.app.BuildConfig
import com.sentinel.app.feature.bot.model.ChatMessageItem
import com.sentinel.app.feature.bot.model.ChatRequest
import com.sentinel.app.feature.bot.model.ChatResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class BotRepository(
    private val baseUrl: String = BuildConfig.BACKEND_BASE_URL,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendMessage(lang: String, history: List<ChatMessageItem>): Result<ChatResponse> =
        withContext(Dispatchers.IO) {
            try {
                val requestPayload = ChatRequest(lang = lang, messages = history)
                val body = json.encodeToString(requestPayload).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("$baseUrl/chat")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val respBody = response.body?.string() ?: ""
                    val chatResponse = json.decodeFromString<ChatResponse>(respBody)
                    Result.success(chatResponse)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
