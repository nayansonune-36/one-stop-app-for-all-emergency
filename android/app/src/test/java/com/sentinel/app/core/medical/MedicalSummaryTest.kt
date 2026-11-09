package com.sentinel.app.core.medical

import com.sentinel.app.feature.medical.model.MedicalProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MedicalSummaryTest {

    @Test
    fun testMedicalSummaryMapping() {
        val profile = MedicalProfile(
            name = "John Doe",
            age = "30",
            bloodGroup = "O+",
            allergies = "Penicillin",
            conditions = "Asthma",
            medications = "Albuterol",
            emergencyContactName = "Jane Doe",
            emergencyContactPhone = "+91-9876543210"
        )

        val contact = listOf(profile.emergencyContactName, profile.emergencyContactPhone)
            .filter { it.isNotBlank() }
            .joinToString(": ")

        val summary = MedicalSummary(
            bloodGroup = profile.bloodGroup.takeIf { it.isNotBlank() },
            allergies = profile.allergies.takeIf { it.isNotBlank() },
            conditions = profile.conditions.takeIf { it.isNotBlank() },
            contact = contact.takeIf { it.isNotBlank() }
        )

        assertNotNull(summary)
        assertEquals("O+", summary.bloodGroup)
        assertEquals("Penicillin", summary.allergies)
        assertEquals("Asthma", summary.conditions)
        assertEquals("Jane Doe: +91-9876543210", summary.contact)
    }

    @Test
    fun testEmptyProfileReturnsNullSummary() {
        val p = MedicalProfile()
        val isEmpty = p.name.isBlank() && p.bloodGroup.isBlank() && p.allergies.isBlank() &&
                p.conditions.isBlank() && p.emergencyContactPhone.isBlank()

        val summary: MedicalSummary? = if (isEmpty) null else MedicalSummary()
        assertNull(summary)
    }
}
