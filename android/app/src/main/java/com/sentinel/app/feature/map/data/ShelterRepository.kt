package com.sentinel.app.feature.map.data

import android.content.Context
import android.util.Log
import com.sentinel.app.feature.map.model.Shelter
import kotlinx.serialization.json.Json

class ShelterRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Loads shelters from the bundled assets/shelters.json.
     * If the asset file is empty, missing, or corrupt, returns an empty list safely without crashing.
     */
    fun loadShelters(): List<Shelter> {
        return try {
            val content = context.assets.open("shelters.json").bufferedReader().use { it.readText() }
            if (content.isBlank() || content.trim() == "[]") {
                Log.i(TAG, "shelters.json is empty")
                emptyList()
            } else {
                json.decodeFromString<List<Shelter>>(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load shelters from assets: ${e.message}", e)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "ShelterRepository"
    }
}
