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

/**
 * Shelter data transfer object and domain model.
 * Tolerant to extra keys, with coord_precision defaulting to "exact".
 */
@Serializable
data class Shelter(
    val id: String,
    val name: String,
    @SerialName("name_hi")
    val nameHi: String = "",
    val type: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int? = null,
    val address: String = "",
    val phone: String? = null,
    val source: String = "",
    val verified: String = "",
    val state: String = "",
    @SerialName("coord_precision")
    val coordPrecision: String = "exact",
    @SerialName("data_notes")
    val dataNotes: String? = null
) {
    val shelterType: ShelterType
        get() = ShelterType.fromWire(type)

    val isApproximate: Boolean
        get() = coordPrecision.equals("approximate", ignoreCase = true)

    val isControlRoom: Boolean
        get() = phone == "1077" || phone == "1913" || (dataNotes?.contains("control room", ignoreCase = true) == true)

    fun localizedName(isHindi: Boolean): String {
        return if (isHindi && nameHi.isNotBlank()) nameHi else name
    }
}

typealias ShelterDto = Shelter
