package com.netpulse.app.data

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

enum class TestPhase { IDLE, PING, DOWNLOAD, UPLOAD, DONE, ERROR }

data class TestProgress(
    val phase: TestPhase = TestPhase.IDLE,
    /** Current speed for the gauge, bytes/sec. */
    val liveBps: Long = 0,
    val pingMs: Int? = null,
    val jitterMs: Int? = null,
    val downBps: Long? = null,
    val upBps: Long? = null,
    val error: String? = null,
)

/**
 * Measures ping, jitter, download and upload against Cloudflare's public speed test endpoints.
 * Several parallel connections are used so fast links are not limited by one TCP stream.
 */
class SpeedTester(
    private val baseUrl: String = "https://speed.cloudflare.com",
    private val phaseMs: Long = 8_000,
    private val warmupMs: Long = 1_000,
) {

    suspend fun run(onProgress: (TestProgress) -> Unit): TestProgress = withContext(Dispatchers.IO) {
        var state = TestProgress(phase = TestPhase.PING)
        onProgress(state)
        try {
            val (ping, jitter) = measurePing()
            state = state.copy(pingMs = ping, jitterMs = jitter, phase = TestPhase.DOWNLOAD)
            onProgress(state)

            val down = measure(workers = 4, onLive = { onProgress(state.copy(liveBps = it)) }) { counter, deadline ->
                download(counter, deadline)
            }
            state = state.copy(downBps = down, liveBps = 0, phase = TestPhase.UPLOAD)
            onProgress(state)

            val up = measure(workers = 3, onLive = { onProgress(state.copy(liveBps = it)) }) { counter, deadline ->
                upload(counter, deadline)
            }
            state = state.copy(upBps = up, liveBps = 0, phase = TestPhase.DONE)
            onProgress(state)
            state
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state.copy(phase = TestPhase.ERROR, liveBps = 0, error = e.message ?: "Network error").also(onProgress)
        }
    }

    /** Median HTTP round trip of tiny requests (first one, with the TLS handshake, is dropped). */
    private suspend fun measurePing(): Pair<Int, Int> {
        val times = mutableListOf<Long>()
        repeat(9) { i ->
            currentCoroutineContext().ensureActive()
            val start = SystemClock.elapsedRealtimeNanos()
            val conn = open("$baseUrl/__down?bytes=0")
            try {
                if (conn.responseCode !in 200..299) throw IOException("Server returned ${conn.responseCode}")
                conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
            if (i > 0) times += (SystemClock.elapsedRealtimeNanos() - start) / 1_000_000
        }
        val sorted = times.sorted()
        val median = sorted[sorted.size / 2].toInt()
        val jitter = times.zipWithNext { a, b -> abs(a - b) }.average().toInt()
        return median to jitter
    }

    /**
     * Runs [workers] transfers in parallel for [phaseMs], reporting the live rate.
     * The result ignores the first [warmupMs] (TCP slow start).
     */
    private suspend fun measure(
        workers: Int,
        onLive: (Long) -> Unit,
        transfer: suspend (AtomicLong, Long) -> Unit,
    ): Long = coroutineScope {
        val counter = AtomicLong()
        val start = SystemClock.elapsedRealtime()
        val deadline = start + phaseMs
        var warmBytes = -1L
        var warmTime = 0L

        val jobs = List(workers) { launch(Dispatchers.IO) { transfer(counter, deadline) } }
        var lastBytes = 0L
        var lastTime = start
        var smoothed = 0.0
        while (SystemClock.elapsedRealtime() < deadline && jobs.any { it.isActive }) {
            delay(250)
            val now = SystemClock.elapsedRealtime()
            val bytes = counter.get()
            val instant = (bytes - lastBytes) * 1000.0 / (now - lastTime).coerceAtLeast(1)
            smoothed = if (smoothed == 0.0) instant else smoothed * 0.7 + instant * 0.3
            onLive(smoothed.toLong())
            lastBytes = bytes
            lastTime = now
            if (warmBytes < 0 && now - start >= warmupMs) {
                warmBytes = bytes
                warmTime = now
            }
        }
        jobs.forEach { it.cancel() }
        val end = SystemClock.elapsedRealtime()
        val total = counter.get()
        if (warmBytes < 0 || end - warmTime <= 0) {
            total * 1000 / (end - start).coerceAtLeast(1)
        } else {
            (total - warmBytes) * 1000 / (end - warmTime)
        }
    }

    private suspend fun download(counter: AtomicLong, deadline: Long) {
        val buffer = ByteArray(64 * 1024)
        while (currentCoroutineContext().isActive && SystemClock.elapsedRealtime() < deadline) {
            val conn = open("$baseUrl/__down?bytes=$CHUNK")
            try {
                conn.inputStream.use { input ->
                    while (SystemClock.elapsedRealtime() < deadline) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        counter.addAndGet(n.toLong())
                    }
                }
            } catch (e: IOException) {
                if (SystemClock.elapsedRealtime() < deadline) throw e
            } finally {
                conn.disconnect()
            }
        }
    }

    private suspend fun upload(counter: AtomicLong, deadline: Long) {
        val buffer = ByteArray(64 * 1024)
        val body = UPLOAD_CHUNK
        while (currentCoroutineContext().isActive && SystemClock.elapsedRealtime() < deadline) {
            val conn = open("$baseUrl/__up").apply {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/octet-stream")
                setFixedLengthStreamingMode(body)
            }
            try {
                conn.outputStream.use { out ->
                    var sent = 0
                    while (sent < body && SystemClock.elapsedRealtime() < deadline) {
                        currentCoroutineContext().ensureActive()
                        val n = minOf(buffer.size, body - sent)
                        out.write(buffer, 0, n)
                        sent += n
                        counter.addAndGet(n.toLong())
                    }
                }
                if (SystemClock.elapsedRealtime() < deadline) conn.responseCode
            } catch (e: IOException) {
                // Aborting a body mid-way at the deadline is expected.
                if (SystemClock.elapsedRealtime() < deadline) throw e
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            useCaches = false
            setRequestProperty("User-Agent", "NetPulse/1.0 (Android)")
        }

    companion object {
        private const val CHUNK = 25_000_000
        private const val UPLOAD_CHUNK = 4_000_000
    }
}
