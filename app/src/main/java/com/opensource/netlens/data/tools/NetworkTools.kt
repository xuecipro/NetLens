package com.opensource.netlens.data.tools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.TimeUnit

data class PingResult(
    val host: String,
    val reachable: Boolean,
    val avgMs: Double?,
    val samples: List<Double>
)

data class DnsResult(
    val hostname: String,
    val addresses: List<String>,
    val elapsedMs: Double,
    val error: String? = null
)

data class HttpCheckResult(
    val url: String,
    val code: Int?,
    val ok: Boolean,
    val elapsedMs: Double,
    val error: String? = null
)

data class PublicIpResult(
    val ip: String?,
    val source: String,
    val error: String? = null
)

class NetworkTools {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun pingHost(host: String, count: Int = 4): PingResult = withContext(Dispatchers.IO) {
        val samples = mutableListOf<Double>()
        try {
            val addr = InetAddress.getByName(host)
            repeat(count) {
                val t0 = System.nanoTime()
                val ok = runCatching { addr.isReachable(1200) }.getOrDefault(false)
                if (ok) samples += (System.nanoTime() - t0) / 1e6
            }
        } catch (_: Exception) {
        }
        // Fallback ICMP via system ping for better accuracy on some devices
        if (samples.isEmpty()) {
            try {
                val pb = ProcessBuilder("ping", "-c", count.toString(), "-W", "2", host)
                pb.redirectErrorStream(true)
                val p = pb.start()
                val out = p.inputStream.bufferedReader().readText()
                p.waitFor(8, TimeUnit.SECONDS)
                Regex("time[=<]([0-9.]+)\\s*ms").findAll(out).forEach {
                    samples += it.groupValues[1].toDouble()
                }
            } catch (_: Exception) {
            }
        }
        PingResult(
            host = host,
            reachable = samples.isNotEmpty(),
            avgMs = samples.takeIf { it.isNotEmpty() }?.average(),
            samples = samples
        )
    }

    suspend fun resolveDns(hostname: String): DnsResult = withContext(Dispatchers.IO) {
        val t0 = System.nanoTime()
        return@withContext try {
            val addrs = InetAddress.getAllByName(hostname).map { it.hostAddress ?: "" }.filter { it.isNotBlank() }
            DnsResult(hostname, addrs, (System.nanoTime() - t0) / 1e6)
        } catch (e: Exception) {
            DnsResult(hostname, emptyList(), (System.nanoTime() - t0) / 1e6, e.message)
        }
    }

    suspend fun httpCheck(url: String): HttpCheckResult = withContext(Dispatchers.IO) {
        val normalized = if (url.startsWith("http")) url else "https://$url"
        val t0 = System.nanoTime()
        return@withContext try {
            val req = Request.Builder().url(normalized).get().build()
            client.newCall(req).execute().use { resp ->
                HttpCheckResult(normalized, resp.code, resp.isSuccessful, (System.nanoTime() - t0) / 1e6)
            }
        } catch (e: Exception) {
            HttpCheckResult(normalized, null, false, (System.nanoTime() - t0) / 1e6, e.message)
        }
    }

    suspend fun publicIp(): PublicIpResult = withContext(Dispatchers.IO) {
        val sources = listOf(
            "https://api.ipify.org" to "ipify",
            "https://ifconfig.me/ip" to "ifconfig",
            "https://checkip.amazonaws.com" to "aws"
        )
        for ((url, name) in sources) {
            try {
                val text = URL(url).readText().trim()
                if (text.isNotBlank() && text.length < 64) {
                    return@withContext PublicIpResult(text, name)
                }
            } catch (_: Exception) {
            }
        }
        PublicIpResult(null, "-", "all sources failed")
    }

    /** Concurrent TCP probe of common local/router ports (authorized own LAN only). */
    suspend fun probePorts(host: String, ports: List<Int> = DEFAULT_PORTS): Map<Int, Boolean> =
        coroutineScope {
            ports.map { port ->
                async {
                    val open = withContext(Dispatchers.IO) {
                        try {
                            java.net.Socket().use { s ->
                                s.connect(java.net.InetSocketAddress(host, port), 600)
                            }
                            true
                        } catch (_: Exception) {
                            false
                        }
                    }
                    port to open
                }
            }.awaitAll().toMap()
        }

    companion object {
        val DEFAULT_PORTS = listOf(22, 53, 80, 443, 8080, 8443, 3389, 62001)
    }
}
