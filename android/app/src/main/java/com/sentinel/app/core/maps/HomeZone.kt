package com.sentinel.app.core.maps

import kotlinx.serialization.Serializable
import kotlin.math.cos

/**
 * Represents the user's configured Home Zone.
 * The Home Zone tiles are downloaded and stored permanently on the device,
 * ensuring streets, landmarks, and emergency facilities are 100% available offline.
 *
 * @param id Unique identifier slug
 * @param name User-facing name (e.g. "Guwahati", "South Delhi")
 * @param centerLat Center latitude in WGS84 decimal degrees
 * @param centerLon Center longitude in WGS84 decimal degrees
 * @param radiusKm Coverage radius in kilometers (clamped between 25.0 and 50.0 km)
 * @param bbox Calculated bounding box enclosing the home area
 */
@Serializable
data class HomeZone(
    val id: String,
    val name: String,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double,
    val bbox: BoundingBox
) {
    /**
     * Checks if a coordinate (lat, lon) falls inside the home zone bounding box.
     */
    fun contains(lat: Double, lon: Double): Boolean {
        return bbox.contains(lat, lon)
    }

    companion object {
        const val MIN_RADIUS_KM = 25.0
        const val MAX_RADIUS_KM = 50.0
        const val DEFAULT_RADIUS_KM = 35.0

        /**
         * Calculates a HomeZone with bounding box derived from center coordinates and radius in km.
         */
        fun fromCenterAndRadius(
            id: String,
            name: String,
            centerLat: Double,
            centerLon: Double,
            radiusKm: Double = DEFAULT_RADIUS_KM
        ): HomeZone {
            val clampedRadius = radiusKm.coerceIn(MIN_RADIUS_KM, MAX_RADIUS_KM)
            
            // 1 degree latitude ≈ 111.0 km
            val dLat = clampedRadius / 111.0
            
            // 1 degree longitude ≈ 111.0 * cos(lat) km
            val latRad = Math.toRadians(centerLat)
            val cosLat = cos(latRad).coerceAtLeast(0.01)
            val dLon = clampedRadius / (111.0 * cosLat)

            val minLat = (centerLat - dLat).coerceIn(-90.0, 90.0)
            val maxLat = (centerLat + dLat).coerceIn(-90.0, 90.0)
            val minLon = (centerLon - dLon).coerceIn(-180.0, 180.0)
            val maxLon = (centerLon + dLon).coerceIn(-180.0, 180.0)

            val bbox = BoundingBox(
                minLon = minLon,
                minLat = minLat,
                maxLon = maxLon,
                maxLat = maxLat
            )

            return HomeZone(
                id = id,
                name = name,
                centerLat = centerLat,
                centerLon = centerLon,
                radiusKm = clampedRadius,
                bbox = bbox
            )
        }

        /**
         * Default fallback home zone for the demo build (Guwahati Metropolitan Region).
         */
        val DEMO_GUWAHATI = fromCenterAndRadius(
            id = "home_guwahati",
            name = "Guwahati",
            centerLat = 26.1445,
            centerLon = 91.7362,
            radiusKm = 30.0
        )
    }
}

/**
 * Pre-bundled list of Indian metropolitan cities and emergency hubs for quick selection.
 */
data class IndianCity(
    val name: String,
    val state: String,
    val lat: Double,
    val lon: Double
) {
    val displayName: String
        get() = "$name, $state"
}

object BundledIndianCities {
    val CITIES = listOf(
        IndianCity("Guwahati", "Assam", 26.1445, 91.7362),
        IndianCity("Dispur", "Assam", 26.1408, 91.7906),
        IndianCity("New Delhi", "Delhi", 28.6139, 77.2090),
        IndianCity("Mumbai", "Maharashtra", 19.0760, 72.8777),
        IndianCity("Pune", "Maharashtra", 18.5204, 73.8567),
        IndianCity("Bengaluru", "Karnataka", 12.9716, 77.5946),
        IndianCity("Kolkata", "West Bengal", 22.5726, 88.3639),
        IndianCity("Chennai", "Tamil Nadu", 13.0827, 80.2707),
        IndianCity("Hyderabad", "Telangana", 17.3850, 78.4867),
        IndianCity("Ahmedabad", "Gujarat", 23.0225, 72.5714),
        IndianCity("Patna", "Bihar", 25.5941, 85.1376),
        IndianCity("Bhubaneswar", "Odisha", 20.2961, 85.8245),
        IndianCity("Cuttack", "Odisha", 20.4625, 85.8828),
        IndianCity("Kochi", "Kerala", 9.9312, 76.2673),
        IndianCity("Thiruvananthapuram", "Kerala", 8.5241, 76.9366),
        IndianCity("Alappuzha", "Kerala", 9.4981, 76.3388),
        IndianCity("Jaipur", "Rajasthan", 26.9124, 75.7873),
        IndianCity("Lucknow", "Uttar Pradesh", 26.8467, 80.9462),
        IndianCity("Srinagar", "Jammu & Kashmir", 34.0837, 74.7973),
        IndianCity("Shimla", "Himachal Pradesh", 31.1048, 77.1734),
        IndianCity("Dehradun", "Uttarakhand", 30.3165, 78.0322),
        IndianCity("Ranchi", "Jharkhand", 23.3441, 85.3096),
        IndianCity("Raipur", "Chhattisgarh", 21.2514, 81.6296),
        IndianCity("Shillong", "Meghalaya", 25.5788, 91.8933),
        IndianCity("Imphal", "Manipur", 24.8170, 93.9368),
        IndianCity("Aizawl", "Mizoram", 23.7271, 92.7176),
        IndianCity("Kohima", "Nagaland", 25.6751, 94.1086),
        IndianCity("Agartala", "Tripura", 23.8315, 91.2868),
        IndianCity("Itanagar", "Arunachal Pradesh", 27.0844, 93.6053)
    )

    fun findNearest(lat: Double, lon: Double): IndianCity {
        return CITIES.minByOrNull { city ->
            val dLat = city.lat - lat
            val dLon = city.lon - lon
            dLat * dLat + dLon * dLon
        } ?: CITIES.first()
    }

    fun search(query: String): List<IndianCity> {
        if (query.isBlank()) return CITIES
        val q = query.trim().lowercase()
        return CITIES.filter {
            it.name.lowercase().contains(q) || it.state.lowercase().contains(q)
        }
    }
}
