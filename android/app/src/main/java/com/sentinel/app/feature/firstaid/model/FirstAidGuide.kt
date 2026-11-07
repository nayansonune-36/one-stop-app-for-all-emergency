package com.sentinel.app.feature.firstaid.model

import kotlinx.serialization.Serializable

@Serializable
data class FirstAidGuide(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val warning: String,
    val steps: List<String>
)
