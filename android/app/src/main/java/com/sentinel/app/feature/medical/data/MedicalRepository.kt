package com.sentinel.app.feature.medical.data

import android.content.Context
import android.content.SharedPreferences
import com.sentinel.app.core.medical.MedicalSummary
import com.sentinel.app.core.medical.MedicalSummaryProvider
import com.sentinel.app.feature.medical.model.MedicalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local encrypted repository for emergency Medical ID data.
 * All sensitive medical data is encrypted with AES-GCM via Android Keystore
 * and never transmitted to the network.
 * Implements MedicalSummaryProvider for attaching vital medical info to emergency SOS.
 */
class MedicalRepository(
    context: Context,
    private val cryptoManager: CryptoManager = CryptoManager()
) : MedicalSummaryProvider {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<MedicalProfile> = _profile.asStateFlow()

    private val _isLocked = MutableStateFlow(prefs.getBoolean(KEY_LOCKED, false))
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private fun loadProfile(): MedicalProfile {
        // 1. Check for encrypted JSON payload
        val encryptedData = prefs.getString(KEY_ENCRYPTED_PROFILE, null)
        if (!encryptedData.isNullOrBlank()) {
            val decryptedJson = cryptoManager.decrypt(encryptedData)
            if (!decryptedJson.isNullOrBlank()) {
                try {
                    return json.decodeFromString<MedicalProfile>(decryptedJson)
                } catch (_: Exception) {
                    // JSON parsing failed, fallback below
                }
            }
        }

        // 2. Migration check: if unencrypted legacy fields exist, migrate them to encrypted payload
        val legacyName = prefs.getString("med_name", null)
        if (legacyName != null) {
            val migrated = MedicalProfile(
                name = prefs.getString("med_name", "") ?: "",
                age = prefs.getString("med_age", "") ?: "",
                bloodGroup = prefs.getString("med_blood", "") ?: "",
                allergies = prefs.getString("med_allergies", "") ?: "",
                conditions = prefs.getString("med_conditions", "") ?: "",
                medications = prefs.getString("med_medications", "") ?: "",
                emergencyContactName = prefs.getString("med_contact_name", "") ?: "",
                emergencyContactPhone = prefs.getString("med_contact_phone", "") ?: ""
            )
            // Persist migrated encrypted profile and clean up plain text keys
            saveProfile(migrated)
            prefs.edit()
                .remove("med_name")
                .remove("med_age")
                .remove("med_blood")
                .remove("med_allergies")
                .remove("med_conditions")
                .remove("med_medications")
                .remove("med_contact_name")
                .remove("med_contact_phone")
                .remove("med_notes")
                .apply()
            return migrated
        }

        return MedicalProfile()
    }

    fun getProfile(): MedicalProfile = _profile.value

    fun saveProfile(profile: MedicalProfile) {
        try {
            val jsonString = json.encodeToString(profile)
            val encryptedBase64 = cryptoManager.encrypt(jsonString)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PROFILE, encryptedBase64)
                .apply()
        } catch (_: Exception) {
            // In case of keystore anomaly, do not store in plain text
        }
        _profile.value = profile
    }

    fun setLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_LOCKED, locked).apply()
        _isLocked.value = locked
    }

    fun getPin(): String? {
        val encryptedPin = prefs.getString(KEY_ENCRYPTED_PIN, null) ?: return null
        return cryptoManager.decrypt(encryptedPin)
    }

    fun setPin(pin: String?) {
        if (pin.isNullOrBlank()) {
            prefs.edit().remove(KEY_ENCRYPTED_PIN).remove(KEY_PIN_LEGACY).apply()
        } else {
            val encrypted = cryptoManager.encrypt(pin)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PIN, encrypted)
                .remove(KEY_PIN_LEGACY)
                .apply()
        }
    }

    override fun getMedicalSummary(): MedicalSummary? {
        val p = _profile.value
        if (p.name.isBlank() && p.bloodGroup.isBlank() && p.allergies.isBlank() &&
            p.conditions.isBlank() && p.emergencyContactPhone.isBlank()
        ) {
            return null
        }
        val contact = listOf(p.emergencyContactName, p.emergencyContactPhone)
            .filter { it.isNotBlank() }
            .joinToString(": ")
        return MedicalSummary(
            bloodGroup = p.bloodGroup.takeIf { it.isNotBlank() },
            allergies = p.allergies.takeIf { it.isNotBlank() },
            conditions = p.conditions.takeIf { it.isNotBlank() },
            contact = contact.takeIf { it.isNotBlank() }
        )
    }

    companion object {
        private const val PREFS_NAME = "sentinel_medical_encrypted_prefs"
        private const val KEY_ENCRYPTED_PROFILE = "enc_medical_profile"
        private const val KEY_ENCRYPTED_PIN = "enc_medical_pin"
        private const val KEY_PIN_LEGACY = "med_pin"
        private const val KEY_LOCKED = "med_locked"
    }
}
