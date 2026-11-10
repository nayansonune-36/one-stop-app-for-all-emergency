package com.sentinel.app.feature.map.tiles

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * High-performance offline vector tile reader for MBTiles SQLite databases.
 * Directly queries vector protobuf tiles (PBF) using standard Android SQLite APIs.
 */
class MbtilesReader(private val file: File) {

    private var database: SQLiteDatabase? = null

    init {
        open()
    }

    private fun open() {
        if (!file.exists() || file.length() == 0L) return
        try {
            database = SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not open MBTiles file at ${file.absolutePath}: ${e.message}")
        }
    }

    /**
     * Reads the vector PBF tile bytes for a given Slippy Map (Z, X, Y) coordinate.
     * Converts Y to MBTiles TMS row coordinate: (1 shl zoom) - 1 - y.
     */
    fun getTile(zoom: Int, x: Int, y: Int): ByteArray? {
        val db = database ?: return null
        if (!db.isOpen) return null

        val tmsY = (1 shl zoom) - 1 - y
        val query = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?"
        val args = arrayOf(zoom.toString(), x.toString(), tmsY.toString())

        return try {
            db.rawQuery(query, args).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getBlob(0)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying tile ($zoom, $x, $y): ${e.message}")
            null
        }
    }

    fun close() {
        try {
            database?.close()
        } catch (_: Exception) {}
        database = null
    }

    companion object {
        private const val TAG = "MbtilesReader"
    }
}
