package com.sentinel.app.feature.map.data

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class MapAssetManager(private val context: Context) {

    /**
     * Prepares offline map storage on first launch.
     * If an MBTiles bundle (e.g. guwahati.mbtiles) is bundled in assets/map,
     * copies it to the app's internal filesDir/map/ directory.
     */
    fun prepareOfflineStorage(): String {
        val mapDir = File(context.filesDir, "map")
        if (!mapDir.exists()) {
            mapDir.mkdirs()
        }

        val mbtilesDestination = File(mapDir, "guwahati.mbtiles")
        try {
            val assetList = context.assets.list("map") ?: emptyArray()
            if (assetList.contains("guwahati.mbtiles") && !mbtilesDestination.exists()) {
                Log.i(TAG, "Copying offline MBTiles package from assets to internal storage...")
                context.assets.open("map/guwahati.mbtiles").use { input ->
                    FileOutputStream(mbtilesDestination).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(TAG, "Offline MBTiles copied successfully: ${mbtilesDestination.absolutePath}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "No pre-bundled MBTiles found or error copying: ${e.message}")
        }

        // Return asset style path (loaded directly by MapLibre Native)
        return "asset://map/offline_style.json"
    }

    companion object {
        private const val TAG = "MapAssetManager"
        const val OFFLINE_STYLE_ASSET = "asset://map/offline_style.json"
    }
}
