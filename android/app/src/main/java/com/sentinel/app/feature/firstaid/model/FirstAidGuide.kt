package com.sentinel.app.feature.firstaid.model

import kotlinx.serialization.Serializable

/**
 * Standard offline First Aid Action Card model based on Red Cross / WHO principles.
 */
@Serializable
data class FirstAidGuide(
    val id: String,
    val title: String,
    val summary: String = "",
    val subtitle: String = "",
    val icon: String = "medical",
    val steps: List<String> = emptyList(),
    val whenToCall: String = "",
    val disclaimer: String = "Not a substitute for professional medical care."
)
