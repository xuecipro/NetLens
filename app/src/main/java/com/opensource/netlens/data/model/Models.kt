package com.opensource.netlens.data.model

import androidx.compose.ui.graphics.Color

enum class Band(val label: String) {
    BAND_24("2.4 GHz"),
    BAND_5("5 GHz"),
    BAND_6("6 GHz"),
    UNKNOWN("?")
}

enum class SecurityType {
    OPEN, WEP, WPA, WPA2, WPA3, WPA2_WPA3, ENTERPRISE, UNKNOWN
}

object SignalLabels {
    const val EXCELLENT = "excellent"
    const val GOOD = "good"
    const val FAIR = "fair"
    const val WEAK = "weak"
    const val DEAD = "dead"

    fun of(rssi: Int): String = when {
        rssi >= -50 -> EXCELLENT
        rssi >= -60 -> GOOD
        rssi >= -70 -> FAIR
        rssi >= -80 -> WEAK
        else -> DEAD
    }

    fun level(rssi: Int): Int = when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        rssi >= -80 -> 1
        else -> 0
    }
}

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val vendor: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val channelWidthMhz: Int?,
    val security: SecurityType,
    val wifiStandard: String,
    val band: Band,
    val isCurrent: Boolean = false,
    val capabilities: String = ""
) {
    val signalLevel: Int get() = SignalLabels.level(rssi)
}

data class CurrentConnection(
    val ssid: String,
    val bssid: String,
    val vendor: String,
    val rssi: Int,
    val linkSpeedMbps: Int,
    val txSpeedMbps: Int?,
    val rxSpeedMbps: Int?,
    val frequencyMhz: Int,
    val channel: Int,
    val channelWidthMhz: Int?,
    val security: SecurityType,
    val wifiStandard: String,
    val ipAddress: String,
    val gateway: String,
    val dns: String,
    val band: Band
)

data class LanDevice(
    val ip: String,
    val mac: String,
    val vendor: String,
    val hostname: String,
    val isGateway: Boolean = false,
    val isThisDevice: Boolean = false,
    val latencyMs: Double? = null
)

data class PingStats(
    val host: String,
    val sent: Int,
    val received: Int,
    val lossPercent: Double,
    val minMs: Double,
    val avgMs: Double,
    val maxMs: Double,
    val jitterMs: Double,
    val samplesMs: List<Double>
)

data class SpeedResult(
    val downloadMbps: Double,
    val uploadMbps: Double,
    val latencyMs: Double,
    val jitterMs: Double,
    val packetLossPercent: Double,
    val serverLabel: String,
    val timestamp: Long,
    val score: Int,
    val grade: String
)

enum class AdviceSeverity { HIGH, MEDIUM, LOW, OK }

data class AdviceItem(
    val severity: AdviceSeverity,
    val titleZh: String,
    val titleEn: String,
    val detailZh: String,
    val detailEn: String,
    val category: String
)

data class ChannelRating(
    val channel: Int,
    val apCount: Int,
    val avgRssi: Int,
    val score: Int
)

data class NetworkSnapshot(
    val connection: CurrentConnection?,
    val networks: List<WifiNetwork>,
    val devices: List<LanDevice>,
    val lastSpeed: SpeedResult?,
    val ping: PingStats?
)

fun signalColor(level: Int): Color = when (level) {
    4 -> Color(0xFF22C55E)
    3 -> Color(0xFF84CC16)
    2 -> Color(0xFFFBBF24)
    1 -> Color(0xFFF97316)
    else -> Color(0xFFEF4444)
}

fun severityColor(severity: AdviceSeverity): Color = when (severity) {
    AdviceSeverity.HIGH -> Color(0xFFEF4444)
    AdviceSeverity.MEDIUM -> Color(0xFFF59E0B)
    AdviceSeverity.LOW -> Color(0xFF3B82F6)
    AdviceSeverity.OK -> Color(0xFF22C55E)
}
