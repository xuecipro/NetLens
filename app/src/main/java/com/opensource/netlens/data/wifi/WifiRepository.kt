package com.opensource.netlens.data.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.DhcpInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.CurrentConnection
import com.opensource.netlens.data.model.WifiNetwork
import com.opensource.netlens.util.Formatters
import com.opensource.netlens.util.FreqChannel
import com.opensource.netlens.util.SecurityParser
import com.opensource.netlens.util.WifiStandards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.net.Inet4Address
import java.net.NetworkInterface

class WifiRepository(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled

    fun scanNetworks(): List<WifiNetwork> {
        val results = try {
            @Suppress("DEPRECATION")
            wifiManager.scanResults
        } catch (_: SecurityException) {
            emptyList()
        }
        val currentBssid = currentConnection()?.bssid?.uppercase()

        return results.map { r ->
            val freq = r.frequency
            val channel = FreqChannel.channelOf(freq)
            val widthMhz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when (r.channelWidth) {
                    0 -> 20
                    1 -> 40
                    2 -> 80
                    3 -> 160
                    4 -> 80 // 80+80
                    5 -> 160
                    6 -> 320
                    else -> null
                }
            } else null

            val bssid = r.BSSID ?: ""
            WifiNetwork(
                ssid = r.SSID?.takeIf { it.isNotBlank() } ?: "",
                bssid = bssid,
                vendor = Formatters.macVendor(bssid),
                rssi = r.level,
                frequencyMhz = freq,
                channel = channel,
                channelWidthMhz = widthMhz,
                security = SecurityParser.parse(r.capabilities ?: ""),
                wifiStandard = wifiStandardOf(r),
                band = FreqChannel.bandOf(freq),
                isCurrent = bssid.uppercase() == currentBssid,
                capabilities = r.capabilities ?: ""
            )
        }.distinctBy { it.bssid.uppercase() }
            .sortedByDescending { it.rssi }
    }

    @SuppressLint("MissingPermission")
    fun triggerScan() {
        try {
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        } catch (_: Exception) {
        }
    }

    @Suppress("DEPRECATION")
    fun currentConnection(): CurrentConnection? {
        val info: WifiInfo = try {
            wifiManager.connectionInfo
        } catch (_: SecurityException) {
            return null
        } ?: return null

        if (info.networkId == -1 && info.ssid.isNullOrBlank()) return null
        val ssid = (info.ssid ?: "").removePrefix("\"").removeSuffix("\"")
        if (ssid.isBlank() || ssid == "<unknown ssid>") return null

        val bssid = info.bssid ?: ""
        val freq = info.frequency
        val channel = FreqChannel.channelOf(freq)
        val dhcp: DhcpInfo? = try {
            wifiManager.dhcpInfo
        } catch (_: Exception) {
            null
        }

        val linkSpeed = try {
            info.linkSpeed
        } catch (_: Exception) {
            -1
        }.coerceAtLeast(0)

        val rssi = try {
            wifiManager.connectionInfo.rssi
        } catch (_: Exception) {
            -127
        }

        val tx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { info.txLinkSpeedMbps }.getOrNull()?.takeIf { it > 0 }
        } else null
        val rx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { info.rxLinkSpeedMbps }.getOrNull()?.takeIf { it > 0 }
        } else null

        val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                when (info.channelWidth) {
                    0 -> 20; 1 -> 40; 2 -> 80; 3 -> 160; 4 -> 80; 5 -> 160; 6 -> 320
                    else -> null
                }
            }.getOrNull()
        } else null

        return CurrentConnection(
            ssid = ssid,
            bssid = bssid,
            vendor = Formatters.macVendor(bssid),
            rssi = rssi,
            linkSpeedMbps = linkSpeed,
            txSpeedMbps = tx ?: linkSpeed.takeIf { it > 0 },
            rxSpeedMbps = rx ?: linkSpeed.takeIf { it > 0 },
            frequencyMhz = freq,
            channel = channel,
            channelWidthMhz = width,
            security = securityFromScan(bssid),
            wifiStandard = WifiStandards.fromLinkSpeed(linkSpeed, freq),
            ipAddress = intToIp(info.ipAddress),
            gateway = dhcp?.let { intToIp(it.gateway) } ?: "",
            dns = dhcp?.let { intToIp(it.dns1) } ?: "",
            band = FreqChannel.bandOf(freq)
        )
    }

    private fun securityFromScan(bssid: String): com.opensource.netlens.data.model.SecurityType {
        val hit = try {
            @Suppress("DEPRECATION")
            wifiManager.scanResults.firstOrNull { it.BSSID.equals(bssid, true) }
        } catch (_: Exception) {
            null
        }
        return SecurityParser.parse(hit?.capabilities ?: "")
    }

    fun localIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress ?: "0.0.0.0"
        } catch (_: Exception) {
            "0.0.0.0"
        }
    }

    fun is5GhzSupported(): Boolean = try {
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            wifiManager.is5GHzBandSupported
        } else wifiManager.is5GHzBandSupported
    } catch (_: Exception) {
        false
    }

    fun is6GhzSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return try {
            wifiManager.is6GHzBandSupported
        } catch (_: Exception) {
            false
        }
    }

    fun observeNetworks(intervalMs: Long = 3000L): Flow<List<WifiNetwork>> = flow {
        while (true) {
            triggerScan()
            delay(800)
            emit(scanNetworks())
            delay(intervalMs.coerceAtLeast(1000))
        }
    }.flowOn(Dispatchers.IO)

    fun observeConnection(): Flow<CurrentConnection?> = flow {
        while (true) {
            emit(currentConnection())
            delay(1500)
        }
    }.flowOn(Dispatchers.IO)

    private fun wifiStandardOf(r: android.net.wifi.ScanResult): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (r.wifiStandard) {
                4 -> "802.11n / WiFi 4"
                5 -> "802.11ac / WiFi 5"
                6 -> "802.11ax / WiFi 6"
                7 -> "802.11be / WiFi 7"
                else -> WifiStandards.fromScan(r.frequency, r.capabilities ?: "", null)
            }
        } else {
            WifiStandards.fromScan(r.frequency, r.capabilities ?: "", null)
        }
    }

    private fun intToIp(ip: Int): String =
        "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"

    companion object {
        fun channelRatings(networks: List<WifiNetwork>, band: Band): List<com.opensource.netlens.data.model.ChannelRating> {
            val channels = when (band) {
                Band.BAND_24 -> (1..13).toList()
                Band.BAND_5 -> listOf(36, 40, 44, 48, 52, 56, 60, 64, 100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144, 149, 153, 157, 161, 165)
                else -> emptyList()
            }
            val inBand = networks.filter { it.band == band && it.channel > 0 }
            return channels.map { ch ->
                val near = if (band == Band.BAND_24) {
                    inBand.filter { kotlin.math.abs(it.channel - ch) <= 2 }
                } else {
                    inBand.filter { it.channel == ch }
                }
                val apCount = near.size
                val avgRssi = if (near.isEmpty()) -100 else near.map { it.rssi }.average().toInt()
                // Higher score is better: fewer APs and weaker neighbors
                val score = (100 - apCount * 12 - ((avgRssi + 100).coerceAtLeast(0) / 3))
                    .coerceIn(0, 100)
                com.opensource.netlens.data.model.ChannelRating(ch, apCount, avgRssi, score)
            }.sortedByDescending { it.score }
        }
    }
}
