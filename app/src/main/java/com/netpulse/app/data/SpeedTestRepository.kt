package com.netpulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class SpeedTestResult(
    val timestampMs: Long,
    val network: NetworkType,
    val downBps: Long,
    val upBps: Long,
    val pingMs: Int,
    val jitterMs: Int,
)

/** Keeps the last [MAX] speed test results. */
class SpeedTestRepository(private val context: Context) {
    private val key = stringPreferencesKey("speed_test_history")

    val history: Flow<List<SpeedTestResult>> = context.dataStore.data.map { prefs ->
        prefs[key].orEmpty().lineSequence().mapNotNull(::decode).toList()
    }

    suspend fun add(result: SpeedTestResult) {
        context.dataStore.edit { prefs ->
            val lines = listOf(encode(result)) + prefs[key].orEmpty().lines().filter { it.isNotBlank() }
            prefs[key] = lines.take(MAX).joinToString("\n")
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(key) }
    }

    companion object {
        const val MAX = 30

        internal fun encode(r: SpeedTestResult) =
            listOf(r.timestampMs, r.network.name, r.downBps, r.upBps, r.pingMs, r.jitterMs).joinToString(",")

        internal fun decode(line: String): SpeedTestResult? {
            val p = line.split(',')
            if (p.size != 6) return null
            return runCatching {
                SpeedTestResult(
                    timestampMs = p[0].toLong(),
                    network = NetworkType.valueOf(p[1]),
                    downBps = p[2].toLong(),
                    upBps = p[3].toLong(),
                    pingMs = p[4].toInt(),
                    jitterMs = p[5].toInt(),
                )
            }.getOrNull()
        }
    }
}
