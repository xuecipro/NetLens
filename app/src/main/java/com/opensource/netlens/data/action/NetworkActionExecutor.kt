package com.opensource.netlens.data.action

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.MacAddress
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.opensource.netlens.data.model.AdviceAction
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.WifiNetwork

/**
 * Turns advice into one-tap in-app strategies the user can actually run.
 * Android cannot change router channel/security remotely; those open guided steps.
 */
class NetworkActionExecutor(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    sealed class ActionResult {
        data class Success(val messageZh: String, val messageEn: String) : ActionResult()
        data class NeedsPermission(val permission: String) : ActionResult()
        data class Error(val messageZh: String, val messageEn: String) : ActionResult()
        data class Navigation(val intent: Intent) : ActionResult()
    }

    fun execute(
        action: AdviceAction,
        networks: List<WifiNetwork>,
        currentSsid: String?,
        currentBand: Band?,
        param: String?,
        targetBssid: String?,
        targetSsid: String?
    ): ActionResult = when (action) {
        AdviceAction.SWITCH_TO_5GHZ -> switchTo5Ghz(networks, currentSsid, targetBssid, targetSsid)
        AdviceAction.OPEN_ROUTER_ADMIN -> openRouterAdmin(param)
        AdviceAction.OPEN_WIFI_SETTINGS -> openWifiSettings()
        AdviceAction.OPEN_LOCATION_SETTINGS -> openLocationSettings()
        AdviceAction.OPEN_WIRELESS_SETTINGS -> openWirelessSettings(param)
        AdviceAction.RUN_SPEED_TEST -> ActionResult.Success(
            "已跳转到测速页，点击开始测速",
            "Opened speed test. Tap Start to run."
        )
        AdviceAction.RUN_DEVICE_SCAN -> ActionResult.Success(
            "已触发局域网扫描",
            "LAN scan started."
        )
        AdviceAction.COPY_CHANNEL_GUIDE -> copyChannelGuide(param, currentSsid)
        AdviceAction.NONE -> ActionResult.Success("", "")
    }

    /**
     * Prefer the 5/6 GHz BSSID of the same SSID via WifiNetworkSuggestion (API 29+).
     * System may prompt the user to accept the suggestion.
     */
    private fun switchTo5Ghz(
        networks: List<WifiNetwork>,
        currentSsid: String?,
        targetBssid: String?,
        targetSsid: String?
    ): ActionResult {
        val ssid = targetSsid ?: currentSsid
        if (ssid.isNullOrBlank()) {
            return ActionResult.Error("当前无 WiFi 名称", "No current SSID")
        }

        val candidates = networks.filter {
            it.ssid == ssid && (it.band == Band.BAND_5 || it.band == Band.BAND_6) && it.rssi > -75
        }.sortedByDescending { it.rssi }

        val pick = candidates.firstOrNull { targetBssid == null || it.bssid.equals(targetBssid, true) }
            ?: candidates.firstOrNull()
            ?: return ActionResult.Error(
                "未发现同名 5/6 GHz 热点，或信号太弱。可到路由器开启双频合一/独立 5GHz。",
                "No usable 5/6 GHz BSSID for this SSID (or signal too weak). Enable dual-band on the router."
            )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return openWifiSettings()
        }

        return try {
            val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
            runCatching { builder.setBssid(MacAddress.fromString(pick.bssid)) }
            val suggestion = builder.build()
            runCatching { wifiManager.removeNetworkSuggestions(listOf(suggestion)) }
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                ActionResult.Success(
                    "已建议系统优先连接 ${ssid} 的 ${pick.band.label}（CH${pick.channel}）。" +
                        "若弹出确认请点允许；若未自动切换，请在系统 WiFi 中选择该 5/6GHz 热点。",
                    "Suggested ${pick.band.label} for $ssid (CH${pick.channel}). Accept if prompted, or pick it in system Wi‑Fi."
                )
            } else {
                ActionResult.Navigation(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (e: Exception) {
            ActionResult.Error(
                "切换失败：${e.message ?: "未知错误"}，请在系统 WiFi 中手动选择 5GHz",
                "Switch failed: ${e.message}. Pick 5 GHz in system Wi‑Fi settings."
            )
        }
    }

    private fun openRouterAdmin(gateway: String?): ActionResult {
        val host = gateway?.takeIf { it.isNotBlank() } ?: return ActionResult.Error(
            "未获取到网关地址",
            "Gateway address unavailable"
        )
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://$host")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            ActionResult.Navigation(intent)
        } catch (e: Exception) {
            ActionResult.Error("无法打开路由器页面", "Cannot open router page")
        }
    }

    private fun openWifiSettings(): ActionResult =
        ActionResult.Navigation(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun openLocationSettings(): ActionResult =
        ActionResult.Navigation(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun openWirelessSettings(gateway: String?): ActionResult {
        val fallback = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val intent = if (!gateway.isNullOrBlank()) {
            try {
                Intent(Intent.ACTION_VIEW, Uri.parse("http://$gateway")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                fallback
            }
        } else fallback
        return ActionResult.Navigation(intent)
    }

    private fun copyChannelGuide(channel: String?, ssid: String?): ActionResult {
        val ch = channel ?: "1/6/11"
        val text = buildString {
            appendLine("【信道优化步骤 / Channel guide】")
            appendLine("当前网络：${ssid ?: "-"}")
            appendLine("1. 浏览器打开路由器后台（通常 http://192.168.1.1 或 http://192.168.0.1）")
            appendLine("2. 登录管理账号（见路由器底部标签）")
            appendLine("3. 进入 无线设置 / Wi‑Fi 设置")
            appendLine("4. 2.4GHz 信道改为 $ch（优先 1/6/11 不重叠）")
            appendLine("5. 若有 5GHz，固定到评分更高的信道（如 36/149）")
            appendLine("6. 保存并重启无线；手机重新连接后用 NetLens 复测")
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("NetLens Channel Guide", text))
        return ActionResult.Success(
            "步骤已复制到剪贴板，按提示操作路由器即可",
            "Channel guide copied to clipboard"
        )
    }

    fun toast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /** Suggest joining a specific scanned BSSID (same or any SSID). */
    fun connectToNetwork(network: com.opensource.netlens.data.model.WifiNetwork): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ActionResult.Navigation(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        return try {
            val builder = WifiNetworkSuggestion.Builder().setSsid(network.ssid)
            runCatching { builder.setBssid(MacAddress.fromString(network.bssid)) }
            val suggestion = builder.build()
            runCatching { wifiManager.removeNetworkSuggestions(listOf(suggestion)) }
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                ActionResult.Success(
                    "已请求连接 ${network.ssid}（${network.band.label} / CH${network.channel}）。若系统弹出确认请点允许。",
                    "Requested join ${network.ssid} (${network.band.label} / CH${network.channel}). Accept if prompted."
                )
            } else {
                ActionResult.Navigation(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (e: Exception) {
            ActionResult.Navigation(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** Copy + open router admin for switching the AP channel. */
    fun switchChannel(channel: Int, ssid: String?, gateway: String?): ActionResult {
        val guide = copyChannelGuide(channel.toString(), ssid)
        val admin = openRouterAdmin(gateway)
        return if (admin is ActionResult.Navigation) {
            ActionResult.Navigation(admin.intent).also {
                toast("已复制步骤，正在打开路由器后台")
            }
        } else {
            guide
        }
    }
}
