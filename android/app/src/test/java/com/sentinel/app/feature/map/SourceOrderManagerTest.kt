package com.sentinel.app.feature.map

import com.sentinel.app.core.maps.BoundingBox
import com.sentinel.app.core.maps.HomeZone
import com.sentinel.app.feature.map.tiles.SourceOrderManager
import com.sentinel.app.feature.map.tiles.TileSourceChoice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceOrderManagerTest {

    private val guwahatiZone = HomeZone.DEMO_GUWAHATI

    @Test
    fun testHomeZoneTileAlwaysServedFromHomePack() {
        val zoom = 14
        val homeTileX = BoundingBox.lonToTileX(guwahatiZone.centerLon, zoom)
        val homeTileY = BoundingBox.latToTileY(guwahatiZone.centerLat, zoom)

        // Inside home zone bbox when offline -> HOME_PACK
        val choiceOffline = SourceOrderManager.selectSource(
            z = zoom,
            x = homeTileX,
            y = homeTileY,
            homeZone = guwahatiZone,
            isOnline = false,
            hasHomePack = true,
            hasLiveSource = false,
            hasIndiaBase = true
        )
        assertEquals(TileSourceChoice.HOME_PACK, choiceOffline)

        // Inside home zone bbox when online -> Still HOME_PACK!
        val choiceOnline = SourceOrderManager.selectSource(
            z = zoom,
            x = homeTileX,
            y = homeTileY,
            homeZone = guwahatiZone,
            isOnline = true,
            hasHomePack = true,
            hasLiveSource = true,
            hasIndiaBase = true
        )
        assertEquals(TileSourceChoice.HOME_PACK, choiceOnline)
    }

    @Test
    fun testLiveSourceUsedWhenOnlineOutsideHomeZone() {
        val zoom = 12
        // Delhi coordinates (outside Guwahati home zone)
        val delhiTileX = BoundingBox.lonToTileX(77.2090, zoom)
        val delhiTileY = BoundingBox.latToTileY(28.6139, zoom)

        val choice = SourceOrderManager.selectSource(
            z = zoom,
            x = delhiTileX,
            y = delhiTileY,
            homeZone = guwahatiZone,
            isOnline = true,
            hasHomePack = true,
            hasLiveSource = true,
            hasIndiaBase = true
        )
        assertEquals(TileSourceChoice.LIVE_SOURCE, choice)
    }

    @Test
    fun testIndiaBaseFallbackWhenOfflineOutsideHomeZone() {
        // Low zoom (z = 6 <= INDIA_BASE_MAX_ZOOM) in Delhi
        val zoom = 6
        val delhiTileX = BoundingBox.lonToTileX(77.2090, zoom)
        val delhiTileY = BoundingBox.latToTileY(28.6139, zoom)

        val choice = SourceOrderManager.selectSource(
            z = zoom,
            x = delhiTileX,
            y = delhiTileY,
            homeZone = guwahatiZone,
            isOnline = false,
            hasHomePack = true,
            hasLiveSource = true,
            hasIndiaBase = true
        )
        assertEquals(TileSourceChoice.INDIA_BASE, choice)
    }

    @Test
    fun testOverzoomWhenOfflineOutsideHomeZoneAtHighZoom() {
        // High zoom (z = 12 > INDIA_BASE_MAX_ZOOM) in Delhi when offline
        val zoom = 12
        val delhiTileX = BoundingBox.lonToTileX(77.2090, zoom)
        val delhiTileY = BoundingBox.latToTileY(28.6139, zoom)

        val choice = SourceOrderManager.selectSource(
            z = zoom,
            x = delhiTileX,
            y = delhiTileY,
            homeZone = guwahatiZone,
            isOnline = false,
            hasHomePack = true,
            hasLiveSource = true,
            hasIndiaBase = true
        )
        // Must return OVERZOOM_OR_EMPTY without errors or crashing
        assertEquals(TileSourceChoice.OVERZOOM_OR_EMPTY, choice)
    }

    @Test
    fun testIsInsideHomeZone() {
        // Guwahati GMCH is inside Guwahati home zone
        assertTrue(SourceOrderManager.isInsideHomeZone(26.155, 91.765, guwahatiZone))

        // Mumbai is outside Guwahati home zone
        assertFalse(SourceOrderManager.isInsideHomeZone(19.0760, 72.8777, guwahatiZone))

        // When home zone is null, always false
        assertFalse(SourceOrderManager.isInsideHomeZone(26.155, 91.765, null))
    }
}
