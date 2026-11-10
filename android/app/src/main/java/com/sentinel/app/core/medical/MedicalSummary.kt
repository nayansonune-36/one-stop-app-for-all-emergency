package com.sentinel.app.core.medical

import kotlinx.serialization.Serializable

/**
 * Emergency medical summary attached to outgoing SOS alerts.
 */
@Serializable
data class MedicalSummary(
    val bloodGroup: String? = null,
    val allergies: String? = null,
    val conditions: String? = null,
    val contact: String? = null
)
