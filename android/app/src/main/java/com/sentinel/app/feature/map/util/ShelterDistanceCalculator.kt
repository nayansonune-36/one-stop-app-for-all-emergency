package com.sentinel.app.feature.map.util

import com.sentinel.app.feature.map.model.Shelter

object ShelterDistanceCalculator {

    const val MAX_NEAREST_DISTANCE_KM = 50.0

    /**
     * Calculates nearest shelter strictly obeying the rules:
     * 1. Only considers entries within 50 km of user's location.
     * 2. Only considers entries whose coord_precision is "exact" for the distance figure.
     * 3. Never returns approximate entries for nearest calculation (returns null if none qualify).
     */
    fun findNearestShelter(
        userLocation: Pair<Double, Double>?,
        shelters: List<Shelter>
    ): Pair<Shelter, Double>? {
        if (userLocation == null || shelters.isEmpty()) return null

        val (uLat, uLon) = userLocation
        var nearest: Shelter? = null
        var minDistance = Double.MAX_VALUE

        for (shelter in shelters) {
            // Rule: Approximate entries are NEVER considered for distance figures
            if (shelter.isApproximate) continue

            val dist = GeoUtils.calculateDistanceKm(uLat, uLon, shelter.lat, shelter.lon)
            // Rule: Maximum 50 km threshold
            if (dist <= MAX_NEAREST_DISTANCE_KM && dist < minDistance) {
                minDistance = dist
                nearest = shelter
            }
        }

        return if (nearest != null) Pair(nearest, minDistance) else null
    }
}
