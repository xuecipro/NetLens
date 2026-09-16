package com.opensource.netlens.data.speed

import com.opensource.netlens.data.model.PingStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt

class PingEngine {

    /**
     * Prefer system ping (ICMP). Fall back to TCP connect latency if ping is blocked.
     */
    suspend fun measure(
        host: String = "1.1.1.1",
        count: Int = 8,
        timeoutSec: Int = 2
    ): PingStats = withContext(Dispatchers.IO) {
        val icmp = tryIcmpPing(host, count, timeoutSec)
        if (icmp != null && icmp.received > 0) return@withContext icmp
        tryTcpPing(host, 443, count)
    }

    private fun tryIcmpPing(host: String, count: Int, timeoutSec: Int): PingStats? {
        return try {
            val pb = ProcessBuilder("ping", "-c", count.toString(), "-W", timeoutSec.toString(), host)
            pb.redirectErrorStream(true)
            val process = pb.start()
            val out = process.inputStream.bufferedReader().readText()
            process.waitFor(30, TimeUnit.SECONDS)
            parsePingOutput(host, count, out)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parsePingOutput(host: String, sent: Int, output: String): PingStats {
        val times = Regex("time[=<]([0-9.]+)\\s*ms")
            .findAll(output)
            .map { it.groupValues[1].toDouble() }
            .toList()
        val lossMatch = Regex("([0-9.]+)% packet loss").find(output)
        val loss = lossMatch?.groupValues?.get(1)?.toDouble()
            ?: if (times.isEmpty()) 100.0 else (sent - times.size) * 100.0 / sent

        if (times.isEmpty()) {
            return PingStats(host, sent, 0, 100.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        }
        return buildStats(host, sent, times, loss)
    }

    private fun tryTcpPing(host: String, port: Int, count: Int): PingStats {
        val times = mutableListOf<Double>()
        var sent = 0
        repeat(count) {
            sent++
            val start = System.nanoTime()
            try {
                java.net.Socket().use { s ->
                    s.connect(java.net.InetSocketAddress(host, port), 1500)
                }
                val ms = (System.nanoTime() - start) / 1e6
                times += ms
            } catch (_: IOException) {
                // count as loss
            }
        }
        val loss = (sent - times.size) * 100.0 / sent
        return buildStats(host, sent, times, loss)
    }

    private fun buildStats(host: String, sent: Int, times: List<Double>, loss: Double): PingStats {
        if (times.isEmpty()) {
            return PingStats(host, sent, 0, 100.0, 0.0, 0.0, 0.0, 0.0, emptyList())
        }
        val min = times.min()
        val max = times.max()
        val avg = times.average()
        val jitter = if (times.size < 2) 0.0 else {
            var sum = 0.0
            for (i in 1 until times.size) sum += abs(times[i] - times[i - 1])
            sum / (times.size - 1)
        }
        return PingStats(
            host = host,
            sent = sent,
            received = times.size,
            lossPercent = loss.coerceIn(0.0, 100.0),
            minMs = min,
            avgMs = avg,
            maxMs = max,
            jitterMs = jitter,
            samplesMs = times
        )
    }
}

class SpeedTestEngine(
    private val downloadUrl: String = DEFAULT_DOWNLOAD_URL,
    private val uploadUrl: String = DEFAULT_UPLOAD_URL,
    private val serverLabel: String = "Cloudflare"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class Progress(val phase: Phase, val percent: Int)

    enum class Phase { DOWNLOAD, UPLOAD, DONE }

    /**
     * Download test: pull a large object and compute throughput from received bytes.
     * sizeBytes default ~25 MB for a reasonably stable sample.
     */
    suspend fun download(
        sizeBytes: Long = 25L * 1024 * 1024,
        urlOverride: String? = null,
        onProgress: (Progress) -> Unit = {}
    ): Double = withContext(Dispatchers.IO) {
        val base = urlOverride?.takeIf { it.isNotBlank() } ?: downloadUrl
        val url = if (base.contains("__down") || "?" !in base && sizeBytes > 0 && base.contains("speed.cloudflare.com")) {
            base + (if ("?" in base) "&" else "?") + "bytes=$sizeBytes"
        } else base

        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

        val body = response.body ?: throw IOException("Empty body")
        val buffer = ByteArray(64 * 1024)
        var received = 0L
        val start = System.nanoTime()
        var lastProgress = 0

        body.byteStream().use { input ->
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                received += n
                if (sizeBytes > 0) {
                    val p = ((received * 100) / sizeBytes).toInt().coerceIn(0, 99)
                    if (p != lastProgress) {
                        lastProgress = p
                        onProgress(Progress(Phase.DOWNLOAD, p))
                    }
                }
            }
        }
        val elapsedSec = (System.nanoTime() - start) / 1e9
        if (elapsedSec <= 0 || received <= 0) throw IOException("No data")
        onProgress(Progress(Phase.DOWNLOAD, 100))
        received * 8.0 / elapsedSec / 1_000_000.0
    }

    /**
     * Upload test: POST random payload and measure wall time.
     */
    suspend fun upload(
        sizeBytes: Long = 8L * 1024 * 1024,
        urlOverride: String? = null,
        onProgress: (Progress) -> Unit = {}
    ): Double = withContext(Dispatchers.IO) {
        val target = urlOverride?.takeIf { it.isNotBlank() } ?: uploadUrl
        val payload = ByteArray(sizeBytes.toInt().coerceAtMost(16 * 1024 * 1024))
        java.util.Random(42).nextBytes(payload)
        val body = payload.toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder().url(target).post(body).build()
        onProgress(Progress(Phase.UPLOAD, 0))
        val start = System.nanoTime()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 200) {
                // Cloudflare __up often returns 200 with empty body; other codes may still work
                if (response.code >= 500) throw IOException("HTTP ${response.code}")
            }
        }
        val elapsedSec = (System.nanoTime() - start) / 1e9
        if (elapsedSec <= 0) throw IOException("Upload failed")
        onProgress(Progress(Phase.UPLOAD, 100))
        payload.size * 8.0 / elapsedSec / 1_000_000.0
    }

    companion object {
        const val DEFAULT_DOWNLOAD_URL = "https://speed.cloudflare.com/__down"
        const val DEFAULT_UPLOAD_URL = "https://speed.cloudflare.com/__up"

        /**
         * Score 0-100 roughly inspired by typical QoE thresholds used by
         * broadband / mobile network testing apps (download weighted, then latency/loss).
         */
        fun scoreOf(
            downloadMbps: Double,
            uploadMbps: Double,
            latencyMs: Double,
            jitterMs: Double,
            lossPercent: Double
        ): Pair<Int, String> {
            var score = 0.0
            // download up to 40
            score += when {
                downloadMbps >= 300 -> 40.0
                downloadMbps >= 100 -> 36.0
                downloadMbps >= 50 -> 32.0
                downloadMbps >= 30 -> 28.0
                downloadMbps >= 20 -> 24.0
                downloadMbps >= 10 -> 18.0
                downloadMbps >= 5 -> 12.0
                downloadMbps >= 2 -> 6.0
                else -> 2.0
            }
            // upload up to 20
            score += when {
                uploadMbps >= 100 -> 20.0
                uploadMbps >= 50 -> 18.0
                uploadMbps >= 20 -> 16.0
                uploadMbps >= 10 -> 13.0
                uploadMbps >= 5 -> 10.0
                uploadMbps >= 2 -> 6.0
                else -> 2.0
            }
            // latency up to 20
            score += when {
                latencyMs <= 10 -> 20.0
                latencyMs <= 20 -> 18.0
                latencyMs <= 40 -> 15.0
                latencyMs <= 80 -> 11.0
                latencyMs <= 150 -> 6.0
                else -> 2.0
            }
            // jitter up to 10
            score += when {
                jitterMs <= 2 -> 10.0
                jitterMs <= 5 -> 8.0
                jitterMs <= 10 -> 6.0
                jitterMs <= 20 -> 3.0
                else -> 1.0
            }
            // loss up to 10
            score += when {
                lossPercent <= 0.0 -> 10.0
                lossPercent <= 1.0 -> 7.0
                lossPercent <= 3.0 -> 4.0
                else -> 0.0
            }
            val s = score.toInt().coerceIn(0, 100)
            val grade = when {
                s >= 90 -> "A+"
                s >= 80 -> "A"
                s >= 70 -> "B"
                s >= 60 -> "C"
                s >= 50 -> "D"
                else -> "F"
            }
            return s to grade
        }
    }
}

object JitterUtil {
    fun fromSamples(samples: List<Double>): Double {
        if (samples.size < 2) return 0.0
        var sum = 0.0
        for (i in 1 until samples.size) sum += abs(samples[i] - samples[i - 1])
        return sum / (samples.size - 1)
    }

    fun stdDev(samples: List<Double>): Double {
        if (samples.isEmpty()) return 0.0
        val mean = samples.average()
        val varr = samples.map { (it - mean) * (it - mean) }.average()
        return sqrt(varr)
    }

    fun minOf2(a: Double, b: Double): Double = if (a < b) a else b
    fun maxOf2(a: Double, b: Double): Double = if (a > b) a else b
}
