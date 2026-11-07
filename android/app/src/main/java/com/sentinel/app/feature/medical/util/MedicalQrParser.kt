package com.sentinel.app.feature.medical.util

import com.sentinel.app.feature.medical.model.MedicalProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object MedicalQrParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Serializes MedicalProfile to the exact flat JSON format expected by SENTINEL QR scanners:
     * {"name":"...","age":"...","blood":"...","allergies":"...","conditions":"...","medications":"...","contactName":"...","contactPhone":"...","notes":"..."}
     */
    fun serialize(profile: MedicalProfile): String {
        return json.encodeToString(profile)
    }

    /**
     * Parses a QR code string. Supports both flat keys and wrapped {"medicalId": {...}} structure.
     * Returns null if no valid medical fields are present.
     */
    fun parse(raw: String): MedicalProfile? {
        return try {
            val element = json.parseToJsonElement(raw)
            val root = element.jsonObject
            val target = if (root.containsKey("medicalId")) {
                root["medicalId"]?.jsonObject ?: root
            } else {
                root
            }

            val name = target["name"]?.jsonPrimitive?.content ?: ""
            val age = target["age"]?.jsonPrimitive?.content ?: ""
            val blood = target["blood"]?.jsonPrimitive?.content ?: ""
            val allergies = target["allergies"]?.jsonPrimitive?.content ?: ""
            val conditions = target["conditions"]?.jsonPrimitive?.content ?: ""
            val medications = target["medications"]?.jsonPrimitive?.content ?: ""
            val contactName = target["contactName"]?.jsonPrimitive?.content ?: ""
            val contactPhone = target["contactPhone"]?.jsonPrimitive?.content ?: ""
            val notes = target["notes"]?.jsonPrimitive?.content ?: ""

            // Check if at least one meaningful field has non-blank data
            val hasContent = listOf(name, age, blood, allergies, conditions, medications, contactName, contactPhone, notes)
                .any { it.isNotBlank() }

            if (hasContent) {
                MedicalProfile(
                    name = name.trim(),
                    age = age.trim(),
                    blood = blood.trim(),
                    allergies = allergies.trim(),
                    conditions = conditions.trim(),
                    medications = medications.trim(),
                    contactName = contactName.trim(),
                    contactPhone = contactPhone.trim(),
                    notes = notes.trim()
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
