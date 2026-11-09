package com.sentinel.app.feature.medical.util

import com.sentinel.app.feature.medical.model.MedicalProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Compact offline QR serializer and parser for MedicalProfile.
 * Generates an ultra-compact versioned JSON payload designed for rapid,
 * error-resilient scanning even under low-light or degraded camera conditions.
 */
object MedicalQrParser {

    const val CURRENT_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Serializes MedicalProfile to a compact, versioned JSON payload:
     * {"v":1,"n":"Name","a":"Age","bg":"O+","al":"...","co":"...","me":"...","en":"...","ep":"..."}
     * Omits empty fields to keep QR module density minimal.
     */
    fun serialize(profile: MedicalProfile): String {
        val root = buildJsonObject {
            put("v", CURRENT_VERSION)
            if (profile.name.isNotBlank()) put("n", profile.name.trim())
            if (profile.age.isNotBlank()) put("a", profile.age.trim())
            if (profile.bloodGroup.isNotBlank()) put("bg", profile.bloodGroup.trim())
            if (profile.allergies.isNotBlank()) put("al", profile.allergies.trim())
            if (profile.conditions.isNotBlank()) put("co", profile.conditions.trim())
            if (profile.medications.isNotBlank()) put("me", profile.medications.trim())
            if (profile.emergencyContactName.isNotBlank()) put("en", profile.emergencyContactName.trim())
            if (profile.emergencyContactPhone.isNotBlank()) put("ep", profile.emergencyContactPhone.trim())
        }
        return root.toString()
    }

    /**
     * Parses a QR code string.
     * Supports both ultra-compact keys (v, n, a, bg, al, co, me, en, ep)
     * and full verbose keys (version, name, age, bloodGroup, allergies, conditions, etc.).
     * Returns null if no valid medical fields are present.
     */
    fun parse(raw: String): MedicalProfile? {
        return try {
            val element = json.parseToJsonElement(raw.trim())
            val root = element.jsonObject
            val target = if (root.containsKey("medicalId")) {
                root["medicalId"]?.jsonObject ?: root
            } else {
                root
            }

            fun extract(vararg keys: String): String {
                for (key in keys) {
                    val value = target[key]?.jsonPrimitive?.content
                    if (!value.isNullOrBlank()) return value.trim()
                }
                return ""
            }

            val name = extract("n", "name")
            val age = extract("a", "age")
            val bloodGroup = extract("bg", "bloodGroup", "blood")
            val allergies = extract("al", "allergies")
            val conditions = extract("co", "conditions")
            val medications = extract("me", "medications")
            val emergencyContactName = extract("en", "emergencyContactName", "contactName")
            val emergencyContactPhone = extract("ep", "emergencyContactPhone", "contactPhone")

            // Check if at least one meaningful field has non-blank data
            val hasContent = listOf(
                name, age, bloodGroup, allergies, conditions, medications,
                emergencyContactName, emergencyContactPhone
            ).any { it.isNotBlank() }

            if (hasContent) {
                MedicalProfile(
                    name = name,
                    age = age,
                    bloodGroup = bloodGroup,
                    allergies = allergies,
                    conditions = conditions,
                    medications = medications,
                    emergencyContactName = emergencyContactName,
                    emergencyContactPhone = emergencyContactPhone
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
