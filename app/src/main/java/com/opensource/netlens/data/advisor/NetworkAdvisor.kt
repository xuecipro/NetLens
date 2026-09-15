package com.opensource.netlens.data.advisor

import com.opensource.netlens.data.model.AdviceItem
import com.opensource.netlens.data.model.AdviceSeverity
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.CurrentConnection
import com.opensource.netlens.data.model.PingStats
import com.opensource.netlens.data.model.SecurityType
import com.opensource.netlens.data.model.SpeedResult
import com.opensource.netlens.data.model.WifiNetwork
import com.opensource.netlens.data.wifi.WifiRepository

/**
 * Rule-based advisor combining WiFi scan + speed/ping metrics,
 * in the spirit of CAICT 全球网测 diagnostics + WiFi Analyzer recommendations.
 */
object NetworkAdvisor {

    fun analyze(
        connection: CurrentConnection?,
        networks: List<WifiNetwork>,
        speed: SpeedResult?,
        ping: PingStats?
    ): List<AdviceItem> {
        val items = mutableListOf<AdviceItem>()

        if (connection == null) {
            items += AdviceItem(
                AdviceSeverity.MEDIUM,
                "未连接 WiFi",
                "Not connected to Wi‑Fi",
                "请先连接到无线网络，再进行信号分析与测速。",
                "Connect to a Wi‑Fi network before signal analysis and speed test.",
                "connection"
            )
            return items
        }

        // Signal strength
        when {
            connection.rssi < -80 -> items += AdviceItem(
                AdviceSeverity.HIGH,
                "信号极弱",
                "Very weak signal",
                "当前信号 ${connection.rssi} dBm，处于覆盖边缘。建议靠近路由器、减少障碍物，或增加 Mesh/中继节点。",
                "RSSI ${connection.rssi} dBm is at the edge of coverage. Move closer to the AP, reduce obstacles, or add a mesh/extender.",
                "signal"
            )
            connection.rssi < -70 -> items += AdviceItem(
                AdviceSeverity.MEDIUM,
                "信号偏弱",
                "Weak signal",
                "当前信号 ${connection.rssi} dBm。若测速明显低于宽带套餐，优先改善摆放位置或切换到 5 GHz/6 GHz。",
                "RSSI ${connection.rssi} dBm. If speed is far below your plan, reposition the AP or prefer 5/6 GHz.",
                "signal"
            )
            connection.rssi >= -50 -> items += AdviceItem(
                AdviceSeverity.OK,
                "信号良好",
                "Good signal",
                "当前信号 ${connection.rssi} dBm，覆盖质量优秀。",
                "RSSI ${connection.rssi} dBm — excellent coverage.",
                "signal"
            )
        }

        // Band advice
        if (connection.band == Band.BAND_24 && connection.rssi > -65) {
            items += AdviceItem(
                AdviceSeverity.LOW,
                "可考虑切换 5 GHz",
                "Consider switching to 5 GHz",
                "近距离使用 5 GHz 通常干扰更少、吞吐更高。若设备支持且信号良好，建议优先 5 GHz。",
                "At close range, 5 GHz usually has less interference and higher throughput. Prefer it when supported.",
                "band"
            )
        }

        // Channel congestion
        val sameBand = networks.filter { it.band == connection.band && it.channel > 0 }
        val crowded = if (connection.band == Band.BAND_24) {
            sameBand.count { kotlin.math.abs(it.channel - connection.channel) <= 2 }
        } else {
            sameBand.count { it.channel == connection.channel }
        }
        if (connection.channel > 0 && crowded >= 6) {
            val ratings = WifiRepository.channelRatings(networks, connection.band)
            val best = ratings.firstOrNull()
            items += AdviceItem(
                AdviceSeverity.MEDIUM,
                "信道拥挤",
                "Crowded channel",
                "当前信道 ${connection.channel} 附近约有 $crowded 个网络。建议在路由器后台改到更空闲信道" +
                    (best?.let { "（推荐 ${it.channel}，评分 ${it.score}）" } ?: "") + "。",
                "About $crowded networks near channel ${connection.channel}. Change the AP channel" +
                    (best?.let { " (try ${it.channel}, score ${it.score})" } ?: "") + ".",
                "channel"
            )
        } else if (connection.channel > 0 && crowded <= 2) {
            items += AdviceItem(
                AdviceSeverity.OK,
                "信道较空闲",
                "Channel is relatively clear",
                "当前信道 ${connection.channel} 邻区较少，干扰可控。",
                "Channel ${connection.channel} has few overlapping APs.",
                "channel"
            )
        }

        // Security
        when (connection.security) {
            SecurityType.OPEN -> items += AdviceItem(
                AdviceSeverity.HIGH,
                "开放网络无加密",
                "Open network (no encryption)",
                "当前 WiFi 未加密，存在窃听与中间人风险。建议启用 WPA2/WPA3。",
                "Network is unencrypted. Enable WPA2/WPA3.",
                "security"
            )
            SecurityType.WEP, SecurityType.WPA -> items += AdviceItem(
                AdviceSeverity.HIGH,
                "安全协议过时",
                "Outdated security",
                "检测到 ${connection.security}，已不安全。建议在路由器改为 WPA2 或 WPA3。",
                "Detected ${connection.security}, which is insecure. Switch the AP to WPA2/WPA3.",
                "security"
            )
            SecurityType.WPA2, SecurityType.WPA3, SecurityType.WPA2_WPA3 -> items += AdviceItem(
                AdviceSeverity.OK,
                "安全协议正常",
                "Security looks fine",
                "当前加密为 ${connection.security}。",
                "Current encryption: ${connection.security}.",
                "security"
            )
            else -> Unit
        }

        // Link speed vs throughput
        if (speed != null && connection.linkSpeedMbps > 0) {
            val ratio = speed.downloadMbps / connection.linkSpeedMbps.toDouble()
            if (connection.linkSpeedMbps >= 150 && speed.downloadMbps < connection.linkSpeedMbps * 0.25) {
                items += AdviceItem(
                    AdviceSeverity.MEDIUM,
                    "协商速率与测速差距大",
                    "Large gap between link speed and speed test",
                    "链路速率约 ${connection.linkSpeedMbps} Mbps，但测速仅 ${"%.1f".format(speed.downloadMbps)} Mbps。可能是信道干扰、路由器性能、宽带瓶颈或测速节点问题。",
                    "Link speed ~${connection.linkSpeedMbps} Mbps but measured ${"%.1f".format(speed.downloadMbps)} Mbps. Likely interference, AP CPU limit, WAN bottleneck, or test server.",
                    "throughput"
                )
            }
            if (ratio > 0.7 && speed.downloadMbps >= 50) {
                items += AdviceItem(
                    AdviceSeverity.OK,
                    "吞吐接近协商速率",
                    "Throughput close to link rate",
                    "无线链路利用率较好。",
                    "Wireless link utilization looks healthy.",
                    "throughput"
                )
            }
        }

        // Latency / jitter / loss
        if (ping != null) {
            if (ping.lossPercent >= 5) {
                items += AdviceItem(
                    AdviceSeverity.HIGH,
                    "丢包偏高",
                    "High packet loss",
                    "丢包约 ${"%.1f".format(ping.lossPercent)}%。检查干扰、弱信号、过载或运营商链路。",
                    "Packet loss ~${"%.1f".format(ping.lossPercent)}%. Check interference, weak signal, overload, or ISP issues.",
                    "latency"
                )
            }
            if (ping.avgMs >= 100) {
                items += AdviceItem(
                    AdviceSeverity.MEDIUM,
                    "延迟偏高",
                    "High latency",
                    "平均延迟 ${"%.1f".format(ping.avgMs)} ms。在线游戏/会议可能受影响；可尝试有线、优化 2.4 GHz 干扰或联系运营商。",
                    "Average latency ${"%.1f".format(ping.avgMs)} ms may hurt gaming/voice. Prefer wired, reduce 2.4 GHz interference, or contact ISP.",
                    "latency"
                )
            }
            if (ping.jitterMs >= 15) {
                items += AdviceItem(
                    AdviceSeverity.MEDIUM,
                    "抖动较大",
                    "High jitter",
                    "抖动约 ${"%.1f".format(ping.jitterMs)} ms，视频会议可能卡顿。优先 5 GHz、减少同频干扰。",
                    "Jitter ~${"%.1f".format(ping.jitterMs)} ms may cause call glitches. Prefer 5 GHz and reduce co-channel use.",
                    "jitter"
                )
            }
        }

        // Speed quality thresholds (全球网测-style)
        if (speed != null) {
            when {
                speed.downloadMbps < 10 -> items += AdviceItem(
                    AdviceSeverity.HIGH,
                    "下载速率偏低",
                    "Low download throughput",
                    "测速下载仅 ${"%.1f".format(speed.downloadMbps)} Mbps。若套餐远高于此，排查距离、干扰、路由器与宽带故障。",
                    "Download ${"%.1f".format(speed.downloadMbps)} Mbps. If your plan is much higher, check distance, interference, AP, and WAN.",
                    "speed"
                )
                speed.downloadMbps in 10.0..50.0 -> items += AdviceItem(
                    AdviceSeverity.LOW,
                    "下载速率一般",
                    "Moderate download throughput",
                    "下载 ${"%.1f".format(speed.downloadMbps)} Mbps，可满足日常使用，高清多路并发可能吃紧。",
                    "Download ${"%.1f".format(speed.downloadMbps)} Mbps is fine for daily use; multi-stream 4K may struggle.",
                    "speed"
                )
                speed.downloadMbps >= 100 -> items += AdviceItem(
                    AdviceSeverity.OK,
                    "下载速率优秀",
                    "Excellent download throughput",
                    "下载 ${"%.1f".format(speed.downloadMbps)} Mbps。",
                    "Download ${"%.1f".format(speed.downloadMbps)} Mbps.",
                    "speed"
                )
            }
            if (speed.uploadMbps < 5) {
                items += AdviceItem(
                    AdviceSeverity.LOW,
                    "上传速率偏低",
                    "Low upload throughput",
                    "上传 ${"%.1f".format(speed.uploadMbps)} Mbps，备份/直播/上行会议可能受限。",
                    "Upload ${"%.1f".format(speed.uploadMbps)} Mbps may limit backup/streaming.",
                    "speed"
                )
            }
        }

        // Hidden SSID / guest note from neighbors
        val possibleGuest = networks.count { it.ssid.contains("guest", true) || it.ssid.contains("访客", true) }
        if (possibleGuest >= 3) {
            items += AdviceItem(
                AdviceSeverity.LOW,
                "周边访客网络较多",
                "Many neighboring guest networks",
                "检测到多个含 guest/访客 的 SSID，说明无线环境较密集，建议固定非重叠信道。",
                "Several guest SSIDs nearby suggest a dense RF environment. Use non-overlapping channels.",
                "rf"
            )
        }

        if (items.none { it.severity == AdviceSeverity.HIGH || it.severity == AdviceSeverity.MEDIUM }) {
            if (items.none { it.severity == AdviceSeverity.OK }) {
                items += AdviceItem(
                    AdviceSeverity.OK,
                    "整体状态良好",
                    "Overall looking good",
                    "未发现明显问题。可再跑一次完整测速获取吞吐结论。",
                    "No obvious issues. Run a full speed test for throughput conclusions.",
                    "summary"
                )
            }
        }

        return items.sortedBy {
            when (it.severity) {
                AdviceSeverity.HIGH -> 0
                AdviceSeverity.MEDIUM -> 1
                AdviceSeverity.LOW -> 2
                AdviceSeverity.OK -> 3
            }
        }
    }
}
