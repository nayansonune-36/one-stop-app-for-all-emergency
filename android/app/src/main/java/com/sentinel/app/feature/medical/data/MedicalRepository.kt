package com.sentinel.app.feature.medical.data

import android.content.Context
import android.content.SharedPreferences
import com.sentinel.app.core.medical.MedicalSummary
import com.sentinel.app.core.medical.MedicalSummaryProvider
import com.sentinel.app.feature.medical.model.MedicalProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MedicalRepository(context: Context) : MedicalSummaryProvider {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<MedicalProfile> = _profile.asStateFlow()

    private val _isLocked = MutableStateFlow(prefs.getBoolean(KEY_LOCKED, false))
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private fun loadProfile(): MedicalProfile {
        return MedicalProfile(
            name = prefs.getString(KEY_NAME, "") ?: "",
            age = prefs.getString(KEY_AGE, "") ?: "",
            blood = prefs.getString(KEY_BLOOD, "") ?: "",
            allergies = prefs.getString(KEY_ALLERGIES, "") ?: "",
            conditions = prefs.getString(KEY_CONDITIONS, "") ?: "",
            medications = prefs.getString(KEY_MEDICATIONS, "") ?: "",
            contactName = prefs.getString(KEY_CONTACT_NAME, "") ?: "",
            contactPhone = prefs.getString(KEY_CONTACT_PHONE, "") ?: "",
            notes = prefs.getString(KEY_NOTES, "") ?: ""
        )
    }

    fun getProfile(): MedicalProfile = _profile.value

    fun saveProfile(profile: MedicalProfile) {
        prefs.edit()
            .putString(KEY_NAME, profile.name)
            .putString(KEY_AGE, profile.age)
            .putString(KEY_BLOOD, profile.blood)
            .putString(KEY_ALLERGIES, profile.allergies)
            .putString(KEY_CONDITIONS, profile.conditions)
            .putString(KEY_MEDICATIONS, profile.medications)
            .putString(KEY_CONTACT_NAME, profile.contactName)
            .putString(KEY_CONTACT_PHONE, profile.contactPhone)
            .putString(KEY_NOTES, profile.notes)
            .apply()
        _profile.value = profile
    }

    fun setLocked(locked: Boolean) {
        prefs.edit().putBoolean(KEY_LOCKED, locked).apply()
        _isLocked.value = locked
    }

    fun getPin(): String? {
        return prefs.getString(KEY_PIN, null)
    }

    fun setPin(pin: String?) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    override fun getMedicalSummary(): MedicalSummary? {
        val p = _profile.value
        if (p.name.isBlank() && p.blood.isBlank() && p.allergies.isBlank() && p.conditions.isBlank() && p.contactPhone.isBlank()) {
            return null
        }
        val contact = listOf(p.contactName, p.contactPhone).filter { it.isNotBlank() }.joinToString(": ")
        return MedicalSummary(
            bloodGroup = p.blood.takeIf { it.isNotBlank() },
            allergies = p.allergies.takeIf { it.isNotBlank() },
            conditions = p.conditions.takeIf { it.isNotBlank() },
            contact = contact.takeIf { it.isNotBlank() }
        )
    }

    companion object {
        private const val PREFS_NAME = "sentinel_medical_prefs"
        private const val KEY_NAME = "med_name"
        private const val KEY_AGE = "med_age"
        private const val KEY_BLOOD = "med_blood"
        private const val KEY_ALLERGIES = "med_allergies"
        private const val KEY_CONDITIONS = "med_conditions"
        private const val KEY_MEDICATIONS = "med_medications"
        private const val KEY_CONTACT_NAME = "med_contact_name"
        private const val KEY_CONTACT_PHONE = "med_contact_phone"
        private const val KEY_NOTES = "med_notes"
        private const val KEY_LOCKED = "med_locked"
        private const val KEY_PIN = "med_pin"
    }
}
