package com.sentinel.app.feature.map.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ShelterType(val wireName: String) {
    SHELTER("shelter"),
    HOSPITAL("hospital"),
    RELIEF_CAMP("relief_camp");

    companion object {
        fun fromWire(value: String): ShelterType {
            return entries.firstOrNull { it.wireName.equals(value, ignoreCase = true) } ?: SHELTER
        }
    }
}

@Serializable
data class Shelter(
    val id: String,
    val name: String,
    @SerialName("name_hi")
    val nameHi: String,
    val type: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int? = null,
    val address: String,
    val phone: String? = null,
    val source: String = "",
    val verified: String = ""
) {
    val shelterType: ShelterType
        get() = ShelterType.fromWire(type)

    fun localizedName(isHindi: Boolean): String {
        return if (isHindi && nameHi.isNotBlank()) nameHi else name
    }
}
