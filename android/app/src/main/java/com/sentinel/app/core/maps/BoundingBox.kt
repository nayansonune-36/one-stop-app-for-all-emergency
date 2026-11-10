package com.sentinel.app.core.maps

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Geographic bounding box represented by minimum and maximum longitude/latitude coordinates.
 * All coordinates are in WGS84 decimal degrees.
 */
@Serializable
data class BoundingBox(
    val minLon: Double,
    val minLat: Double,
    val maxLon: Double,
    val maxLat: Double
) {
    init {
        require(minLon <= maxLon) { "minLon ($minLon) must be <= maxLon ($maxLon)" }
        require(minLat <= maxLat) { "minLat ($minLat) must be <= maxLat ($maxLat)" }
    }

    /**
     * Checks if a point (lat, lon) falls inside this bounding box.
     */
    fun contains(lat: Double, lon: Double): Boolean {
        return lat in minLat..maxLat && lon in minLon..maxLon
    }

    /**
     * Checks if this bounding box intersects another bounding box.
     */
    fun intersects(other: BoundingBox): Boolean {
        return !(other.minLon > maxLon || other.maxLon < minLon ||
                other.minLat > maxLat || other.maxLat < minLat)
    }

    /**
     * Formats bounding box as standard "minLon,minLat,maxLon,maxLat" string (e.g. for pmtiles CLI).
     */
    fun toCliString(): String {
        return "%.4f,%.4f,%.4f,%.4f".format(minLon, minLat, maxLon, maxLat)
    }

    /**
     * Converts the bounding box to tile coordinate range (minX, minY, maxX, maxY) at a given zoom level.
     * Uses standard Slippy Map / Web Mercator tile equations.
     */
    fun toTileRange(zoom: Int): TileRange {
        val minTileX = lonToTileX(minLon, zoom)
        val maxTileX = lonToTileX(maxLon, zoom)
        val minTileY = latToTileY(maxLat, zoom) // Latitude is inverted in tile Y
        val maxTileY = latToTileY(minLat, zoom)
        return TileRange(
            zoom = zoom,
            minX = min(minTileX, maxTileX),
            maxX = max(minTileX, maxTileX),
            minY = min(minTileY, maxTileY),
            maxY = max(minTileY, maxTileY)
        )
    }

    companion object {
        fun lonToTileX(lon: Double, zoom: Int): Int {
            val n = 1 shl zoom
            return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        }

        fun latToTileY(lat: Double, zoom: Int): Int {
            val n = 1 shl zoom
            val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
            return floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
        }
    }
}

/**
 * Tile coordinate range at a specific zoom level.
 */
data class TileRange(
    val zoom: Int,
    val minX: Int,
    val maxX: Int,
    val minY: Int,
    val maxY: Int
) {
    val count: Int
        get() = (maxX - minX + 1) * (maxY - minY + 1)

    fun contains(x: Int, y: Int): Boolean {
        return x in minX..maxX && y in minY..maxY
    }
}
