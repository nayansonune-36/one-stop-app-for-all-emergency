package com.sentinel.app.feature.firstaid.data

import android.content.Context
import android.util.Log
import com.sentinel.app.feature.firstaid.model.FirstAidGuide
import kotlinx.serialization.json.Json

class FirstAidRepository(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun loadGuides(isHindi: Boolean): List<FirstAidGuide> {
        val fileName = if (isHindi) "firstaid/firstaid_hi.json" else "firstaid/firstaid_en.json"
        return try {
            val content = context.assets.open(fileName).bufferedReader().use { it.readText() }
            json.decodeFromString<List<FirstAidGuide>>(content)
        } catch (e: Exception) {
            Log.e("FirstAidRepository", "Error reading $fileName: ${e.message}")
            emptyList()
        }
    }
}
