package com.sentinel.app.core.medical

/**
 * Contract to obtain medical profile information for emergency broadcasts.
 */
interface MedicalSummaryProvider {
    fun getMedicalSummary(): MedicalSummary?
}

/**
 * Default null implementation allowing the SOS feature to compile and function
 * before feature/medical is fully merged.
 */
class DefaultMedicalSummaryProvider : MedicalSummaryProvider {
    override fun getMedicalSummary(): MedicalSummary? = null
}
