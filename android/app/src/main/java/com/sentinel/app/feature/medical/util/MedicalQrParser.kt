package com.sentinel.app.feature.medical.util

import com.sentinel.app.feature.medical.model.MedicalProfile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
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
        coerceInputValues = true
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
     * Supports compact keys (v, n, a, bg, al, co, me, en, ep),
     * verbose keys (version, name, age, bloodGroup, allergies, conditions, etc.),
     * wrapped JSON payloads, and plain-text emergency key-value cards.
     * Returns null if no valid medical fields are present.
     */
    fun parse(raw: String): MedicalProfile? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        // 1. Try parsing JSON (extracting JSON substring if wrapped in prefixes/URLs)
        val jsonProfile = parseJsonCandidate(trimmed)
        if (jsonProfile != null) return jsonProfile

        // 2. Try parsing plain text key-value format
        return parseTextCandidate(trimmed)
    }

    private fun parseJsonCandidate(raw: String): MedicalProfile? {
        return try {
            var candidate = raw
            if (candidate.contains("{") && candidate.contains("}")) {
                val start = candidate.indexOf("{")
                val end = candidate.lastIndexOf("}")
                if (start < end) {
                    candidate = candidate.substring(start, end + 1)
                }
            }

            val element = json.parseToJsonElement(candidate)
            val root = element.jsonObject
            val target: JsonObject = if (root.containsKey("medicalId") && root["medicalId"] is JsonObject) {
                root["medicalId"]!!.jsonObject
            } else {
                root
            }

            fun extract(vararg keys: String): String {
                for (key in keys) {
                    val elem = target[key] ?: target[key.lowercase()] ?: continue
                    when (elem) {
                        is JsonPrimitive -> {
                            val content = elem.content.trim()
                            if (content.isNotBlank() && content != "null") return content
                        }
                        is JsonArray -> {
                            val items = elem.mapNotNull { item ->
                                (item as? JsonPrimitive)?.content?.trim()
                            }.filter { it.isNotBlank() && it != "null" }
                            if (items.isNotEmpty()) return items.joinToString(", ")
                        }
                        else -> {}
                    }
                }
                return ""
            }

            val name = extract("n", "name", "fullName", "patientName")
            val age = extract("a", "age", "years")
            val bloodGroup = extract("bg", "bloodGroup", "blood", "bloodType")
            val allergies = extract("al", "allergies", "allergy")
            val conditions = extract("co", "conditions", "medicalConditions", "chronic")
            val medications = extract("me", "medications", "meds")
            val emergencyContactName = extract("en", "emergencyContactName", "contactName", "emergencyContact", "iceName")
            val emergencyContactPhone = extract("ep", "emergencyContactPhone", "contactPhone", "icePhone", "phone")

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

    private fun parseTextCandidate(raw: String): MedicalProfile? {
        try {
            val lines = raw.lines()
            var name = ""
            var age = ""
            var bloodGroup = ""
            var allergies = ""
            var conditions = ""
            var medications = ""
            var emergencyContactName = ""
            var emergencyContactPhone = ""

            for (line in lines) {
                val colonIndex = line.indexOf(':')
                if (colonIndex > 0) {
                    val key = line.substring(0, colonIndex).trim().lowercase()
                    val value = line.substring(colonIndex + 1).trim()
                    if (value.isBlank()) continue

                    when {
                        key.contains("name") && !key.contains("contact") -> name = value
                        key.contains("age") -> age = value
                        key.contains("blood") -> bloodGroup = value
                        key.contains("allerg") -> allergies = value
                        key.contains("condition") -> conditions = value
                        key.contains("med") -> medications = value
                        key.contains("contact") && (key.contains("name") || !key.contains("phone")) -> emergencyContactName = value
                        key.contains("phone") || key.contains("mobile") || key.contains("contact phone") -> emergencyContactPhone = value
                    }
                }
            }

            val hasContent = listOf(name, age, bloodGroup, allergies, conditions, medications, emergencyContactName, emergencyContactPhone).any { it.isNotBlank() }
            if (hasContent) {
                return MedicalProfile(
                    name = name,
                    age = age,
                    bloodGroup = bloodGroup,
                    allergies = allergies,
                    conditions = conditions,
                    medications = medications,
                    emergencyContactName = emergencyContactName,
                    emergencyContactPhone = emergencyContactPhone
                )
            }
        } catch (_: Exception) {
            // Text parse fallback
        }
        return null
    }
}
