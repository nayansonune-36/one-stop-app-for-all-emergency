package com.sentinel.app.feature.medical

import com.sentinel.app.feature.medical.model.MedicalProfile
import com.sentinel.app.feature.medical.util.MedicalQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicalQrParserTest {

    @Test
    fun testSerializeCompactIncludesVersion() {
        val profile = MedicalProfile(
            name = "Ravi Kumar",
            age = "32",
            bloodGroup = "O+",
            allergies = "Penicillin",
            conditions = "Asthma",
            medications = "Inhaler",
            emergencyContactName = "Priya Kumar",
            emergencyContactPhone = "+919876543210"
        )

        val json = MedicalQrParser.serialize(profile)

        assertTrue(json.contains("\"v\":1"))
        assertTrue(json.contains("\"n\":\"Ravi Kumar\""))
        assertTrue(json.contains("\"bg\":\"O+\""))
        assertTrue(json.contains("\"en\":\"Priya Kumar\""))
        assertTrue(json.contains("\"ep\":\"+919876543210\""))
    }

    @Test
    fun testParseCompactPayload() {
        val raw = """{"v":1,"n":"Aarav Sharma","a":"29","bg":"B+","al":"Peanuts","co":"Hypertension","me":"Amlodipine","en":"Sunita Sharma","ep":"+919123456780"}"""

        val profile = MedicalQrParser.parse(raw)

        assertNotNull(profile)
        assertEquals("Aarav Sharma", profile?.name)
        assertEquals("29", profile?.age)
        assertEquals("B+", profile?.bloodGroup)
        assertEquals("Peanuts", profile?.allergies)
        assertEquals("Hypertension", profile?.conditions)
        assertEquals("Amlodipine", profile?.medications)
        assertEquals("Sunita Sharma", profile?.emergencyContactName)
        assertEquals("+919123456780", profile?.emergencyContactPhone)
    }

    @Test
    fun testParseFullPayload() {
        val raw = """
            {
                "version": 1,
                "name": "Jane Smith",
                "age": "45",
                "bloodGroup": "AB-",
                "allergies": "Sulfa",
                "conditions": "Diabetes",
                "medications": "Metformin",
                "emergencyContactName": "Bob Smith",
                "emergencyContactPhone": "+1234567890"
            }
        """.trimIndent()

        val profile = MedicalQrParser.parse(raw)

        assertNotNull(profile)
        assertEquals("Jane Smith", profile?.name)
        assertEquals("AB-", profile?.bloodGroup)
        assertEquals("Sulfa", profile?.allergies)
        assertEquals("Bob Smith", profile?.emergencyContactName)
    }

    @Test
    fun testParseLegacyPayload() {
        val raw = """
            {
                "name": "Old Format",
                "blood": "A+",
                "contactName": "ICE Contact",
                "contactPhone": "9999999999"
            }
        """.trimIndent()

        val profile = MedicalQrParser.parse(raw)

        assertNotNull(profile)
        assertEquals("Old Format", profile?.name)
        assertEquals("A+", profile?.bloodGroup)
        assertEquals("ICE Contact", profile?.emergencyContactName)
        assertEquals("9999999999", profile?.emergencyContactPhone)
    }

    @Test
    fun testParseInvalidOrEmptyReturnsNull() {
        assertNull(MedicalQrParser.parse(""))
        assertNull(MedicalQrParser.parse("{}"))
        assertNull(MedicalQrParser.parse("random unformatted text"))
        assertNull(MedicalQrParser.parse("""{"v":1}"""))
    }
}
