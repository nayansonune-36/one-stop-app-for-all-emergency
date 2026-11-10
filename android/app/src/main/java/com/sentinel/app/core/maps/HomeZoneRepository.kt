package com.sentinel.app.core.maps

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.sentinel.app.feature.map.model.Shelter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Download and installation status of the Home Pack.
 */
sealed class HomePackStatus {
    data object NotConfigured : HomePackStatus()
    data class Downloading(val progressPercent: Int) : HomePackStatus()
    data object OfflineReady : HomePackStatus()
    data class Failed(val reason: String) : HomePackStatus()
}

/**
 * Repository managing Home Zone configuration, local offline pack storage,
 * mobile data download policy, and home-zone cached emergency shelters.
 */
class HomeZoneRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _homePackStatus = MutableStateFlow<HomePackStatus>(HomePackStatus.NotConfigured)
    val homePackStatus: StateFlow<HomePackStatus> = _homePackStatus.asStateFlow()

    init {
        // Initial state determination
        val currentZone = getHomeZone()
        val packFile = getHomePackFile()
        if (currentZone != null && packFile.exists() && packFile.length() > 0) {
            _homePackStatus.value = HomePackStatus.OfflineReady
        } else if (currentZone != null) {
            _homePackStatus.value = HomePackStatus.NotConfigured
        } else {
            // For demo build: prefill Guwahati default home zone if nothing is set
            saveHomeZone(HomeZone.DEMO_GUWAHATI)
            if (packFile.exists() && packFile.length() > 0) {
                _homePackStatus.value = HomePackStatus.OfflineReady
            }
        }
    }

    /**
     * Retrieves the stored HomeZone or null if not yet selected.
     */
    fun getHomeZone(): HomeZone? {
        val serialized = prefs.getString(KEY_HOME_ZONE, null) ?: return null
        return try {
            json.decodeFromString<HomeZone>(serialized)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize HomeZone: ${e.message}")
            null
        }
    }

    /**
     * Saves the HomeZone and updates cached shelters.
     */
    fun saveHomeZone(zone: HomeZone) {
        try {
            val serialized = json.encodeToString(zone)
            prefs.edit().putString(KEY_HOME_ZONE, serialized).apply()
            Log.i(TAG, "Saved HomeZone: ${zone.name} (${zone.radiusKm} km radius)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize HomeZone: ${e.message}")
        }
    }

    /**
     * Mobile data download policy toggle (Wi-Fi only vs mobile data allowed).
     */
    fun isAllowMobileData(): Boolean {
        return prefs.getBoolean(KEY_ALLOW_MOBILE_DATA, false)
    }

    fun setAllowMobileData(allow: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_MOBILE_DATA, allow).apply()
    }

    fun updateStatus(status: HomePackStatus) {
        _homePackStatus.value = status
    }

    /**
     * File location for the downloaded or pre-bundled Home Pack archive.
     * Prefers .mbtiles for SQLite MapLibre compatibility, or .pmtiles.
     */
    fun getHomePackFile(): File {
        val mapsDir = File(context.filesDir, "maps")
        if (!mapsDir.exists()) mapsDir.mkdirs()
        
        val mbtiles = File(mapsDir, "home_pack.mbtiles")
        if (mbtiles.exists()) return mbtiles

        val pmtiles = File(mapsDir, "home_pack.pmtiles")
        if (pmtiles.exists()) return pmtiles

        return mbtiles
    }

    /**
     * Size of the stored Home Pack in Megabytes.
     */
    fun getHomePackSizeBytes(): Long {
        val file = getHomePackFile()
        return if (file.exists()) file.length() else 0L
    }

    fun getHomePackSizeMb(): Double {
        return getHomePackSizeBytes() / (1024.0 * 1024.0)
    }

    /**
     * Calculates free internal disk space in Megabytes.
     */
    fun getAvailableStorageMb(): Double {
        return try {
            val stat = StatFs(context.filesDir.absolutePath)
            val bytes = stat.availableBlocksLong * stat.blockSizeLong
            bytes / (1024.0 * 1024.0)
        } catch (e: Exception) {
            500.0 // Reasonable fallback default
        }
    }

    /**
     * Deletes the local Home Pack and resets status.
     */
    fun deleteHomePack(): Boolean {
        val mapsDir = File(context.filesDir, "maps")
        var deleted = false
        File(mapsDir, "home_pack.mbtiles").let { if (it.exists()) deleted = it.delete() || deleted }
        File(mapsDir, "home_pack.pmtiles").let { if (it.exists()) deleted = it.delete() || deleted }
        _homePackStatus.value = HomePackStatus.NotConfigured
        return deleted
    }

    /**
     * Caches all shelters that fall inside the configured Home Zone.
     */
    fun cacheSheltersForHomeZone(allShelters: List<Shelter>) {
        val zone = getHomeZone() ?: return
        val filtered = allShelters.filter { zone.contains(it.lat, it.lon) }
        try {
            val serialized = json.encodeToString(filtered)
            prefs.edit().putString(KEY_CACHED_SHELTERS, serialized).apply()
            Log.i(TAG, "Cached ${filtered.size} emergency shelters for Home Zone '${zone.name}'")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching shelters: ${e.message}")
        }
    }

    /**
     * Loads the cached Home Zone emergency shelters.
     */
    fun getCachedShelters(): List<Shelter> {
        val serialized = prefs.getString(KEY_CACHED_SHELTERS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Shelter>>(serialized)
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val TAG = "HomeZoneRepository"
        private const val PREFS_NAME = "sentinel_home_zone_prefs"
        private const val KEY_HOME_ZONE = "key_home_zone"
        private const val KEY_ALLOW_MOBILE_DATA = "key_allow_mobile_data"
        private const val KEY_CACHED_SHELTERS = "key_cached_shelters"

        @Volatile
        private var INSTANCE: HomeZoneRepository? = null

        fun getInstance(context: Context): HomeZoneRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HomeZoneRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
