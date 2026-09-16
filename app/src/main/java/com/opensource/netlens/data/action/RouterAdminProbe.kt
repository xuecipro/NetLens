package com.opensource.netlens.data.action

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Probe common router admin endpoints so we don't blindly open
 * http://gateway:80 when the UI is on 443/8080/8443.
 */
object RouterAdminProbe {

    data class Endpoint(val scheme: String, val port: Int, val url: String) {
        val label: String get() = "$scheme://…:$port"
    }

    val CANDIDATES = listOf(
        Endpoint("https", 443, "https://%s"),
        Endpoint("http", 80, "http://%s"),
        Endpoint("http", 8080, "http://%s:8080"),
        Endpoint("https", 8443, "https://%s:8443"),
        Endpoint("http", 8888, "http://%s:8888")
    )

    fun defaultUrl(gateway: String): String = "http://$gateway"

    fun allUrls(gateway: String): List<Endpoint> =
        CANDIDATES.map { it.copy(url = String.format(it.url, gateway)) }

    /** TCP connect timeout probe. Returns first endpoint that accepts a connection. */
    suspend fun probe(gateway: String, timeoutMs: Int = 800): Endpoint? =
        withContext(Dispatchers.IO) {
            if (gateway.isBlank()) return@withContext null
            for (ep in allUrls(gateway)) {
                try {
                    Socket().use { s ->
                        s.connect(InetSocketAddress(gateway, ep.port), timeoutMs)
                    }
                    return@withContext ep
                } catch (_: Exception) {
                    // try next port
                }
            }
            null
        }

    fun refusedHelpText(gateway: String): String = buildString {
        appendLine("无法打开路由器后台（Connection refused）。可依次尝试：")
        appendLine("1. 确认网关是否为 $gateway（设置→WiFi→当前网络）")
        appendLine("2. 浏览器手动试：https://$gateway、http://$gateway:8080、https://$gateway:8443")
        appendLine("3. 看路由器底部标签上的管理地址（不一定是网关 IP）")
        appendLine("4. 光猫模式时，管理页可能在 192.168.1.1，而路由器是 192.168.0.1")
        appendLine("5. 部分品牌需装官方 App，或仅允许有线/指定设备管理")
        appendLine("6. 关闭移动数据后再试，避免走了蜂窝网络")
    }
}
