package com.opensource.netlens.data.advisor

import com.opensource.netlens.data.model.AdviceAction
import com.opensource.netlens.data.model.AdviceItem
import com.opensource.netlens.data.model.AdviceSeverity
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.CurrentConnection
import com.opensource.netlens.data.model.PingStats
import com.opensource.netlens.data.model.SecurityType
import com.opensource.netlens.data.model.SpeedResult
import com.opensource.netlens.data.model.WifiNetwork
import com.opensource.netlens.data.wifi.WifiRepository

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
                "connection",
                action = AdviceAction.OPEN_WIFI_SETTINGS,
                actionLabelZh = "打开 WiFi 设置",
                actionLabelEn = "Open Wi‑Fi settings"
            )
            return items
        }

        val sameSsid5g = networks.filter {
            it.ssid == connection.ssid && (it.band == Band.BAND_5 || it.band == Band.BAND_6) && it.rssi > -80
        }.maxByOrNull { it.rssi }

        when {
            connection.rssi < -80 -> items += AdviceItem(
                AdviceSeverity.HIGH,
                "信号极弱",
                "Very weak signal",
                "当前信号 ${connection.rssi} dBm，处于覆盖边缘。靠近路由器或增加中继。",
                "RSSI ${connection.rssi} dBm is at the coverage edge. Move closer or add mesh.",
                "signal",
                action = AdviceAction.OPEN_WIFI_SETTINGS,
                actionLabelZh = "检查附近网络",
                actionLabelEn = "Check nearby APs"
            )
            connection.rssi < -70 -> items += AdviceItem(
                AdviceSeverity.MEDIUM,
                "信号偏弱",
                "Weak signal",
                "当前信号 ${connection.rssi} dBm。可尝试靠近路由器，或切换到 5 GHz。",
                "RSSI ${connection.rssi} dBm. Move closer or prefer 5 GHz.",
                "signal",
                action = if (sameSsid5g != null) AdviceAction.SWITCH_TO_5GHZ else AdviceAction.OPEN_WIFI_SETTINGS,
                actionLabelZh = if (sameSsid5g != null) "切换到 5 GHz" else "打开 WiFi 设置",
                actionLabelEn = if (sameSsid5g != null) "Switch to 5 GHz" else "Open Wi‑Fi settings",
                targetBssid = sameSsid5g?.bssid,
                targetSsid = connection.ssid
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

        // Band switch strategy
        if (connection.band == Band.BAND_24) {
            if (sameSsid5g != null && sameSsid5g.rssi > -70 && connection.rssi > -75) {
                items += AdviceItem(
                    AdviceSeverity.MEDIUM,
                    "建议切换到 ${sameSsid5g.band.label}",
                    "Switch to ${sameSsid5g.band.label}",
                    "发现同名 ${sameSsid5g.band.label} 热点（${sameSsid5g.bssid}，信号 ${sameSsid5g.rssi} dBm，CH${sameSsid5g.channel}）。近距离 5/6GHz 干扰更少、吞吐更高。",
                    "Found same SSID on ${sameSsid5g.band.label} (${sameSsid5g.bssid}, ${sameSsid5g.rssi} dBm, CH${sameSsid5g.channel}). Better throughput at close range.",
                    "band",
                    action = AdviceAction.SWITCH_TO_5GHZ,
                    actionLabelZh = "一键切换 ${sameSsid5g.band.label}",
                    actionLabelEn = "Switch to ${sameSsid5g.band.label}",
                    targetBssid = sameSsid5g.bssid,
                    targetSsid = connection.ssid
                )
            } else if (connection.rssi > -65 && sameSsid5g == null) {
                items += AdviceItem(
                    AdviceSeverity.LOW,
                    "可考虑启用 5 GHz",
                    "Consider enabling 5 GHz",
                    "当前在 2.4 GHz。若路由器支持，请开启 5 GHz 或双频独立 SSID，再回来一键切换。",
                    "On 2.4 GHz. Enable 5 GHz on the router, then switch from this app.",
                    "band",
                    action = AdviceAction.OPEN_ROUTER_ADMIN,
                    actionLabelZh = "打开路由器后台",
                    actionLabelEn = "Open router admin",
                    actionParam = connection.gateway
                )
            }
        }

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
                "当前信道 ${connection.channel} 附近约有 $crowded 个网络。到路由器改到更空闲信道" +
                    (best?.let { "（推荐 ${it.channel}）" } ?: "") + "。",
                "About $crowded APs near channel ${connection.channel}. Change AP channel" +
                    (best?.let { " (try ${it.channel})" } ?: "") + ".",
                "channel",
                action = AdviceAction.COPY_CHANNEL_GUIDE,
                actionLabelZh = "复制改信道步骤",
                actionLabelEn = "Copy channel guide",
                actionParam = best?.channel?.toString() ?: "1/6/11",
                targetSsid = connection.ssid
            )
            items += AdviceItem(
                AdviceSeverity.LOW,
                "打开路由器设置",
                "Open router settings",
                "登录路由器后台修改无线信道与频段（网关 ${connection.gateway.ifBlank { "未知" }}）。",
                "Sign in to the router to change channel/band (gateway ${connection.gateway.ifBlank { "unknown" }}).",
                "channel",
                action = AdviceAction.OPEN_ROUTER_ADMIN,
                actionLabelZh = "打开路由器后台",
                actionLabelEn = "Open router admin",
                actionParam = connection.gateway
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

        when (connection.security) {
            SecurityType.OPEN -> items += AdviceItem(
                AdviceSeverity.HIGH,
                "开放网络无加密",
                "Open network (no encryption)",
                "当前 WiFi 未加密。请到路由器启用 WPA2/WPA3。",
                "Unencrypted. Enable WPA2/WPA3 on the router.",
                "security",
                action = AdviceAction.OPEN_ROUTER_ADMIN,
                actionLabelZh = "打开路由器后台",
                actionLabelEn = "Open router admin",
                actionParam = connection.gateway
            )
            SecurityType.WEP, SecurityType.WPA -> items += AdviceItem(
                AdviceSeverity.HIGH,
                "安全协议过时",
                "Outdated security",
                "检测到 ${connection.security}。请在路由器改为 WPA2 或 WPA3。",
                "Detected ${connection.security}. Switch the AP to WPA2/WPA3.",
                "security",
                action = AdviceAction.OPEN_ROUTER_ADMIN,
                actionLabelZh = "打开路由器后台",
                actionLabelEn = "Open router admin",
                actionParam = connection.gateway
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

        if (speed != null && connection.linkSpeedMbps >= 150 &&
            speed.downloadMbps < connection.linkSpeedMbps * 0.25
        ) {
            items += AdviceItem(
                AdviceSeverity.MEDIUM,
                "协商速率与测速差距大",
                "Large gap: link speed vs speed test",
                "链路约 ${connection.linkSpeedMbps} Mbps，测速仅 ${"%.1f".format(speed.downloadMbps)} Mbps。可切换 5GHz 或改信道后复测。",
                "Link ~${connection.linkSpeedMbps} Mbps but measured ${"%.1f".format(speed.downloadMbps)} Mbps. Try 5 GHz / change channel.",
                "throughput",
                action = if (sameSsid5g != null) AdviceAction.SWITCH_TO_5GHZ else AdviceAction.COPY_CHANNEL_GUIDE,
                actionLabelZh = if (sameSsid5g != null) "切换 5 GHz 后复测" else "复制改信道步骤",
                actionLabelEn = if (sameSsid5g != null) "Switch to 5 GHz & retest" else "Copy channel guide",
                actionParam = WifiRepository.channelRatings(networks, connection.band).firstOrNull()?.channel?.toString(),
                targetBssid = sameSsid5g?.bssid,
                targetSsid = connection.ssid
            )
        }

        if (ping != null) {
            if (ping.lossPercent >= 5) {
                items += AdviceItem(
                    AdviceSeverity.HIGH,
                    "丢包偏高",
                    "High packet loss",
                    "丢包约 ${"%.1f".format(ping.lossPercent)}%。改善信号/信道后再测速。",
                    "Loss ~${"%.1f".format(ping.lossPercent)}%. Improve signal/channel then retest.",
                    "latency",
                    action = AdviceAction.RUN_SPEED_TEST,
                    actionLabelZh = "重新测速",
                    actionLabelEn = "Re-run speed test"
                )
            }
            if (ping.avgMs >= 100) {
                items += AdviceItem(
                    AdviceSeverity.MEDIUM,
                    "延迟偏高",
                    "High latency",
                    "平均延迟 ${"%.1f".format(ping.avgMs)} ms。可切换 5GHz 或改信道。",
                    "Latency ${"%.1f".format(ping.avgMs)} ms. Try 5 GHz or change channel.",
                    "latency",
                    action = if (sameSsid5g != null) AdviceAction.SWITCH_TO_5GHZ else AdviceAction.COPY_CHANNEL_GUIDE,
                    actionLabelZh = if (sameSsid5g != null) "切换 5 GHz" else "复制改信道步骤",
                    actionLabelEn = if (sameSsid5g != null) "Switch to 5 GHz" else "Copy channel guide",
                    targetBssid = sameSsid5g?.bssid,
                    targetSsid = connection.ssid
                )
            }
            if (ping.jitterMs >= 15) {
                items += AdviceItem(
                    AdviceSeverity.MEDIUM,
                    "抖动较大",
                    "High jitter",
                    "抖动约 ${"%.1f".format(ping.jitterMs)} ms。优先 5 GHz、减少同频干扰。",
                    "Jitter ~${"%.1f".format(ping.jitterMs)} ms. Prefer 5 GHz.",
                    "jitter",
                    action = if (sameSsid5g != null) AdviceAction.SWITCH_TO_5GHZ else AdviceAction.OPEN_ROUTER_ADMIN,
                    actionLabelZh = if (sameSsid5g != null) "切换 5 GHz" else "打开路由器后台",
                    actionLabelEn = if (sameSsid5g != null) "Switch to 5 GHz" else "Open router admin",
                    actionParam = connection.gateway,
                    targetBssid = sameSsid5g?.bssid,
                    targetSsid = connection.ssid
                )
            }
        }

        if (speed != null) {
            when {
                speed.downloadMbps < 10 -> items += AdviceItem(
                    AdviceSeverity.HIGH,
                    "下载速率偏低",
                    "Low download throughput",
                    "下载仅 ${"%.1f".format(speed.downloadMbps)} Mbps。先优化无线，再测速对比。",
                    "Download ${"%.1f".format(speed.downloadMbps)} Mbps. Optimize Wi‑Fi then retest.",
                    "speed",
                    action = AdviceAction.RUN_SPEED_TEST,
                    actionLabelZh = "重新测速",
                    actionLabelEn = "Re-run speed test"
                )
                speed.uploadMbps < 5 -> items += AdviceItem(
                    AdviceSeverity.LOW,
                    "上传速率偏低",
                    "Low upload throughput",
                    "上传 ${"%.1f".format(speed.uploadMbps)} Mbps。",
                    "Upload ${"%.1f".format(speed.uploadMbps)} Mbps.",
                    "speed",
                    action = AdviceAction.OPEN_ROUTER_ADMIN,
                    actionLabelZh = "检查路由器",
                    actionLabelEn = "Check router",
                    actionParam = connection.gateway
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
        }

        items += AdviceItem(
            AdviceSeverity.LOW,
            "扫描局域网设备",
            "Scan LAN devices",
            "确认是否有陌生设备占网、是否存在异常高流量终端。",
            "Check for unknown or heavy devices on your LAN.",
            "devices",
            action = AdviceAction.RUN_DEVICE_SCAN,
            actionLabelZh = "扫描设备",
            actionLabelEn = "Scan devices"
        )

        if (items.none { it.severity == AdviceSeverity.HIGH || it.severity == AdviceSeverity.MEDIUM }) {
            if (items.none { it.severity == AdviceSeverity.OK }) {
                items += AdviceItem(
                    AdviceSeverity.OK,
                    "整体状态良好",
                    "Overall looking good",
                    "未发现明显问题。可跑一次完整测速确认吞吐。",
                    "No obvious issues. Run a full speed test.",
                    "summary",
                    action = AdviceAction.RUN_SPEED_TEST,
                    actionLabelZh = "开始测速",
                    actionLabelEn = "Start speed test"
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
