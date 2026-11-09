package com.sentinel.app.feature.firstaid

import com.sentinel.app.feature.firstaid.model.FirstAidGuide
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FirstAidContentTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val expectedTopicIds = listOf(
        "cpr",
        "choking",
        "bleeding",
        "burns",
        "fracture",
        "snakebite",
        "drowning",
        "heatstroke",
        "fainting",
        "poisoning"
    )

    private fun findContentFile(name: String): File {
        val candidates = listOf(
            File("../../content/$name"),
            File("../content/$name"),
            File("content/$name"),
            File("src/main/assets/firstaid/$name"),
            File("src/main/assets/content/$name")
        )
        val found = candidates.firstOrNull { it.exists() }
        assertNotNull("Could not locate $name in search paths", found)
        return found!!
    }

    @Test
    fun testEnglishContentHas10TopicsWithValidSchema() {
        val file = findContentFile("firstaid_en.json")
        assertTrue("firstaid_en.json must exist", file.exists())

        val content = file.readText()
        val guides = json.decodeFromString<List<FirstAidGuide>>(content)

        assertEquals("Exactly 10 first-aid topics required", 10, guides.size)

        val ids = guides.map { it.id }
        assertEquals(expectedTopicIds, ids)

        for (guide in guides) {
            assertTrue("Title must not be blank for ${guide.id}", guide.title.isNotBlank())
            assertTrue("Summary must not be blank for ${guide.id}", guide.summary.isNotBlank())
            assertTrue("Steps must not be empty for ${guide.id}", guide.steps.isNotEmpty())
            assertTrue("Steps must not exceed 6 for ${guide.id}", guide.steps.size <= 6)
            assertTrue("WhenToCall must not be blank for ${guide.id}", guide.whenToCall.isNotBlank())
            assertTrue("Disclaimer must not be blank for ${guide.id}", guide.disclaimer.isNotBlank())

            for (step in guide.steps) {
                assertFalse("Step must not be blank", step.isBlank())
            }
        }
    }

    @Test
    fun testHindiContentHas10TopicsWithValidSchema() {
        val file = findContentFile("firstaid_hi.json")
        assertTrue("firstaid_hi.json must exist", file.exists())

        val content = file.readText()
        val guides = json.decodeFromString<List<FirstAidGuide>>(content)

        assertEquals("Exactly 10 first-aid topics required", 10, guides.size)

        val ids = guides.map { it.id }
        assertEquals(expectedTopicIds, ids)

        for (guide in guides) {
            assertTrue("Title must not be blank for ${guide.id}", guide.title.isNotBlank())
            assertTrue("Summary must not be blank for ${guide.id}", guide.summary.isNotBlank())
            assertTrue("Steps must not be empty for ${guide.id}", guide.steps.isNotEmpty())
            assertTrue("Steps must not exceed 6 for ${guide.id}", guide.steps.size <= 6)
            assertTrue("WhenToCall must not be blank for ${guide.id}", guide.whenToCall.isNotBlank())
            assertTrue("Disclaimer must not be blank for ${guide.id}", guide.disclaimer.isNotBlank())

            for (step in guide.steps) {
                assertFalse("Step must not be blank", step.isBlank())
            }
        }
    }
}
