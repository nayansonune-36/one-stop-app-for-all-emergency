package com.sentinel.app.core.mesh

import com.sentinel.app.core.medical.MedicalSummary
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SosMessage(
    val id: String,
    val ts: Long,
    val lat: Double,
    val lon: Double,
    val hops: Int = 0,
    val name: String? = null,
    val medical: MedicalSummary? = null
) {
    fun toByteArray(): ByteArray {
        return jsonConfig.encodeToString(this).toByteArray(Charsets.UTF_8)
    }

    companion object {
        @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
        val jsonConfig = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

        fun fromByteArray(bytes: ByteArray): SosMessage? {
            return try {
                val str = String(bytes, Charsets.UTF_8)
                jsonConfig.decodeFromString<SosMessage>(str)
            } catch (_: Exception) {
                null
            }
        }
    }
}
