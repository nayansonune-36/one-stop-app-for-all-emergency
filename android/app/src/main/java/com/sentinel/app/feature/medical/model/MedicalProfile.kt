package com.sentinel.app.feature.medical.model

import kotlinx.serialization.Serializable

/**
 * Standard Medical ID profile.
 * Stored locally and encrypted using an AES-GCM key from Android Keystore.
 * Never transmitted over the network.
 */
@Serializable
data class MedicalProfile(
    val name: String = "",
    val age: String = "",
    val bloodGroup: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = ""
)
