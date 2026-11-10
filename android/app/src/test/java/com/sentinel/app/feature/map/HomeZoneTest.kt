package com.sentinel.app.feature.map

import com.sentinel.app.core.maps.BoundingBox
import com.sentinel.app.core.maps.HomeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeZoneTest {

    @Test
    fun testRadiusClamping() {
        val zoneMin = HomeZone.fromCenterAndRadius(
            id = "test_min",
            name = "Test Min",
            centerLat = 26.14,
            centerLon = 91.73,
            radiusKm = 10.0 // Below 25 km
        )
        assertEquals(25.0, zoneMin.radiusKm, 0.001)

        val zoneMax = HomeZone.fromCenterAndRadius(
            id = "test_max",
            name = "Test Max",
            centerLat = 26.14,
            centerLon = 91.73,
            radiusKm = 100.0 // Above 50 km
        )
        assertEquals(50.0, zoneMax.radiusKm, 0.001)

        val zoneDefault = HomeZone.fromCenterAndRadius(
            id = "test_default",
            name = "Test Default",
            centerLat = 26.14,
            centerLon = 91.73,
            radiusKm = 35.0
        )
        assertEquals(35.0, zoneDefault.radiusKm, 0.001)
    }

    @Test
    fun testBoundingBoxMaths() {
        val centerLat = 26.1445
        val centerLon = 91.7362
        val zone = HomeZone.fromCenterAndRadius(
            id = "guwahati",
            name = "Guwahati",
            centerLat = centerLat,
            centerLon = centerLon,
            radiusKm = 30.0
        )

        // Center must be strictly inside calculated bounding box
        assertTrue(zone.bbox.minLat < centerLat)
        assertTrue(zone.bbox.maxLat > centerLat)
        assertTrue(zone.bbox.minLon < centerLon)
        assertTrue(zone.bbox.maxLon > centerLon)

        // Center point check
        assertTrue(zone.contains(centerLat, centerLon))

        // Dispur (approx 26.14, 91.79) should be contained in Guwahati 30km radius
        assertTrue(zone.contains(26.14, 91.79))

        // Delhi (28.6139, 77.2090) should NOT be contained in Guwahati
        assertFalse(zone.contains(28.6139, 77.2090))
    }

    @Test
    fun testTileRangeConversion() {
        val zone = HomeZone.DEMO_GUWAHATI
        val zoom = 14
        val tileRange = zone.bbox.toTileRange(zoom)

        assertTrue(tileRange.minX <= tileRange.maxX)
        assertTrue(tileRange.minY <= tileRange.maxY)
        assertTrue(tileRange.count > 0)

        // Tile for Guwahati center
        val centerTileX = BoundingBox.lonToTileX(zone.centerLon, zoom)
        val centerTileY = BoundingBox.latToTileY(zone.centerLat, zoom)

        assertTrue(tileRange.contains(centerTileX, centerTileY))

        // Tile for Delhi should NOT be in Guwahati's tile range
        val delhiTileX = BoundingBox.lonToTileX(77.2090, zoom)
        val delhiTileY = BoundingBox.latToTileY(28.6139, zoom)
        assertFalse(tileRange.contains(delhiTileX, delhiTileY))
    }

    @Test
    fun testBoundingBoxIntersections() {
        val bboxA = BoundingBox(minLon = 91.0, minLat = 26.0, maxLon = 92.0, maxLat = 27.0)
        val bboxOverlapping = BoundingBox(minLon = 91.5, minLat = 26.5, maxLon = 93.0, maxLat = 28.0)
        val bboxDisjoint = BoundingBox(minLon = 77.0, minLat = 28.0, maxLon = 78.0, maxLat = 29.0)

        assertTrue(bboxA.intersects(bboxOverlapping))
        assertTrue(bboxOverlapping.intersects(bboxA))
        assertFalse(bboxA.intersects(bboxDisjoint))
        assertFalse(bboxDisjoint.intersects(bboxA))
    }
}
