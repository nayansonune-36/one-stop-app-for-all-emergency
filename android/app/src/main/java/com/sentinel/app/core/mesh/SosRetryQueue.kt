package com.sentinel.app.core.mesh

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistent offline queue for SOS beacons awaiting upload to the backend gateway.
 */
class SosRetryQueue(private val context: Context) {

    private val file = File(context.filesDir, "pending_sos_queue.json")
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun enqueue(sos: SosMessage) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val list = loadInternal().toMutableList()
            if (list.none { it.id == sos.id }) {
                list.add(sos)
                saveInternal(list)
            }
        }
    }

    suspend fun remove(sosId: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val list = loadInternal().filterNot { it.id == sosId }
            saveInternal(list)
        }
    }

    suspend fun getAll(): List<SosMessage> = mutex.withLock {
        withContext(Dispatchers.IO) {
            loadInternal()
        }
    }

    private fun loadInternal(): List<SosMessage> {
        return try {
            if (!file.exists()) return emptyList()
            val text = file.readText(Charsets.UTF_8)
            if (text.isBlank()) return emptyList()
            json.decodeFromString<List<SosMessage>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveInternal(list: List<SosMessage>) {
        try {
            val text = json.encodeToString(list)
            file.writeText(text, Charsets.UTF_8)
        } catch (_: Exception) {
            // Ignore write errors gracefully
        }
    }
}
