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
        val candidates = if (isHindi) {
            listOf("firstaid/firstaid_hi.json", "content/firstaid_hi.json", "firstaid_hi.json")
        } else {
            listOf("firstaid/firstaid_en.json", "content/firstaid_en.json", "firstaid_en.json")
        }

        for (fileName in candidates) {
            try {
                val content = context.assets.open(fileName).bufferedReader().use { it.readText() }
                val guides = json.decodeFromString<List<FirstAidGuide>>(content)
                if (guides.isNotEmpty()) return guides
            } catch (_: Exception) {
                // Try next candidate
            }
        }

        Log.e("FirstAidRepository", "Failed to load guides for isHindi=$isHindi")
        return emptyList()
    }
}
