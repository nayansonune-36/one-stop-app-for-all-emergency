package com.sentinel.app.feature.medical.model

import kotlinx.serialization.Serializable

/**
 * Standard Medical ID profile. Serializes to/from flat JSON keys:
 * name, age, blood, allergies, conditions, medications, contactName, contactPhone, notes.
 */
@Serializable
data class MedicalProfile(
    val name: String = "",
    val age: String = "",
    val blood: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val notes: String = ""
)
