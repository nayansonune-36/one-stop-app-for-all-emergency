package com.sentinel.app.feature.map.data

import android.content.Context
import android.util.Log
import com.sentinel.app.feature.map.model.Shelter
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray

class ShelterRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Loads shelters from the bundled assets/shelters.json.
     * Tolerant: Entries that fail to parse are skipped and logged, never crashing the screen.
     * If the asset file is empty, missing, or corrupt, returns an empty list safely without crashing.
     */
    fun loadShelters(): List<Shelter> {
        val candidates = listOf("shelters.json", "data/shelters.json")
        for (fileName in candidates) {
            try {
                val content = context.assets.open(fileName).bufferedReader().use { it.readText() }
                if (content.isBlank() || content.trim() == "[]") {
                    continue
                }
                val jsonElement = json.parseToJsonElement(content)
                val jsonArray = jsonElement.jsonArray

                val result = mutableListOf<Shelter>()
                for ((index, element) in jsonArray.withIndex()) {
                    try {
                        val shelter = json.decodeFromJsonElement(Shelter.serializer(), element)
                        result.add(shelter)
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping corrupt shelter entry at index $index: ${e.message}")
                    }
                }
                if (result.isNotEmpty()) {
                    Log.i(TAG, "Loaded ${result.size} shelter entries from $fileName")
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not open or parse $fileName: ${e.message}")
            }
        }
        return emptyList()
    }

    companion object {
        private const val TAG = "ShelterRepository"
    }
}
