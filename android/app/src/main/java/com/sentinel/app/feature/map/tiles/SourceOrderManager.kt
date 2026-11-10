package com.sentinel.app.feature.map.tiles

import com.sentinel.app.core.maps.HomeZone

/**
 * Tile source resolution decision for hybrid map rendering.
 */
enum class TileSourceChoice {
    /** Tile served from the locally stored Home Pack (always offline and online). */
    HOME_PACK,

    /** Tile fetched dynamically from the live remote vector tile source when online. */
    LIVE_SOURCE,

    /** Coarse low-zoom base map bundled with the app (fallback when offline outside Home Zone). */
    INDIA_BASE,

    /** No tile available at this coordinate/zoom level; MapLibre overzooms parent tile. */
    OVERZOOM_OR_EMPTY
}

/**
 * Strict source order rendering logic:
 * 1. Home pack first for any tile inside the home bbox (online or not)
 * 2. Live source if online
 * 3. India base map fallback layer
 * 4. Overzoom beyond max zoom without errors; no blank screens
 */
object SourceOrderManager {

    /**
     * Maximum zoom level supported by the coarse base India map.
     */
    const val INDIA_BASE_MAX_ZOOM = 7

    /**
     * Maximum zoom level supported by the detailed Home Pack.
     */
    const val HOME_PACK_MAX_ZOOM = 15

    /**
     * Selects which source to query for a given Slippy Map tile coordinate (z, x, y).
     */
    fun selectSource(
        z: Int,
        x: Int,
        y: Int,
        homeZone: HomeZone?,
        isOnline: Boolean,
        hasHomePack: Boolean,
        hasLiveSource: Boolean,
        hasIndiaBase: Boolean
    ): TileSourceChoice {
        // Priority 1: Home pack for ANY tile intersecting Home Zone bbox
        if (hasHomePack && homeZone != null) {
            val tileRange = homeZone.bbox.toTileRange(z)
            if (tileRange.contains(x, y)) {
                return TileSourceChoice.HOME_PACK
            }
        }

        // Priority 2: Live vector tile service when online
        if (isOnline && hasLiveSource) {
            return TileSourceChoice.LIVE_SOURCE
        }

        // Priority 3: Coarse India base map fallback
        if (hasIndiaBase) {
            return if (z <= INDIA_BASE_MAX_ZOOM) {
                TileSourceChoice.INDIA_BASE
            } else {
                // When zooming past base map zoom outside home zone, overzoom without error
                TileSourceChoice.OVERZOOM_OR_EMPTY
            }
        }

        return TileSourceChoice.OVERZOOM_OR_EMPTY
    }

    /**
     * Checks if the current map view center and zoom level is inside the Home Zone.
     */
    fun isInsideHomeZone(lat: Double, lon: Double, homeZone: HomeZone?): Boolean {
        if (homeZone == null) return false
        return homeZone.contains(lat, lon)
    }
}
