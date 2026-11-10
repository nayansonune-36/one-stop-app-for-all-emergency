package com.sentinel.app.feature.map

import com.sentinel.app.feature.map.model.Shelter
import com.sentinel.app.feature.map.util.ShelterDistanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NearestShelterTest {

    private val userLocationGuwahati = Pair(26.15, 91.75)

    private val exactCloseShelter = Shelter(
        id = "exact-close",
        name = "Exact Close Shelter",
        nameHi = "सटीक पास आश्रय",
        type = "shelter",
        lat = 26.16,
        lon = 91.76,
        capacity = 500,
        address = "Guwahati Central",
        phone = "+91-361-2223344",
        source = "test",
        verified = "2026-11-08",
        state = "Assam",
        coordPrecision = "exact"
    )

    private val approximateSuperCloseShelter = Shelter(
        id = "approx-super-close",
        name = "Approximate Super Close Shelter",
        nameHi = "अनुमानित पास आश्रय",
        type = "shelter",
        lat = 26.151,
        lon = 91.751,
        capacity = 200,
        address = "Town Centroid",
        phone = null,
        source = "test",
        verified = "2026-11-08",
        state = "Assam",
        coordPrecision = "approximate"
    )

    private val exactFarShelterBeyond50Km = Shelter(
        id = "exact-far-beyond-50km",
        name = "Exact Far Facility (>50km)",
        nameHi = "सटीक दूर सुविधा",
        type = "hospital",
        lat = 26.80, // ~72 km north of 26.15
        lon = 91.75,
        capacity = 100,
        address = "Far District",
        phone = null,
        source = "test",
        verified = "2026-11-08",
        state = "Assam",
        coordPrecision = "exact"
    )

    @Test
    fun testNearestShelterSelectsExactWithin50Km() {
        val list = listOf(exactCloseShelter)
        val result = ShelterDistanceCalculator.findNearestShelter(userLocationGuwahati, list)

        assertNotNull("Nearest shelter must be found", result)
        assertEquals("exact-close", result?.first?.id)
        assertTrue("Distance should be less than 50 km", result!!.second < 50.0)
    }

    @Test
    fun testApproximateShelterIsNeverSelectedEvenIfCloser() {
        // Approximate shelter is physically much closer (~0.15 km vs ~1.5 km),
        // but rule dictates approximate entries are NEVER eligible for distance/nearest.
        val list = listOf(approximateSuperCloseShelter, exactCloseShelter)
        val result = ShelterDistanceCalculator.findNearestShelter(userLocationGuwahati, list)

        assertNotNull("Nearest shelter must select exact entry", result)
        assertEquals("Must select exact entry, ignoring closer approximate entry", "exact-close", result?.first?.id)
    }

    @Test
    fun testShelterBeyond50KmIsExcluded() {
        // Distance is ~72 km (> 50 km threshold)
        val list = listOf(exactFarShelterBeyond50Km)
        val result = ShelterDistanceCalculator.findNearestShelter(userLocationGuwahati, list)

        assertNull("Shelters beyond 50 km must be excluded, returning null", result)
    }

    @Test
    fun testEmptyResultWhenOnlyApproximateSheltersExistWithin50Km() {
        val list = listOf(approximateSuperCloseShelter)
        val result = ShelterDistanceCalculator.findNearestShelter(userLocationGuwahati, list)

        assertNull("Only approximate entries present; must return null so UI shows 'No shelter found near you yet'", result)
    }

    @Test
    fun testEmptyResultWhenUserLocationIsNull() {
        val list = listOf(exactCloseShelter)
        val result = ShelterDistanceCalculator.findNearestShelter(null, list)

        assertNull("Null user location must return null", result)
    }

    @Test
    fun testEmptyResultWhenShelterListIsEmpty() {
        val result = ShelterDistanceCalculator.findNearestShelter(userLocationGuwahati, emptyList())

        assertNull("Empty shelter list must return null", result)
    }
}
