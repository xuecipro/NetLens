package com.opensource.netlens.data.devices

import android.content.Context
import android.net.wifi.WifiManager
import com.opensource.netlens.data.model.LanDevice
import com.opensource.netlens.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.NetworkInterface

class DeviceScanner(private val context: Context) {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    suspend fun scan(limit: Int = 254): List<LanDevice> = withContext(Dispatchers.IO) {
        val selfIp = localIp()
        val gatewayIp = gatewayIp()
        val candidates = Formatters.subnetCandidates(selfIp.ifBlank { "192.168.1.1" }, 24, limit)
            .filter { it != selfIp }

        // Fast concurrent ping sweep
        coroutineScope {
            candidates.map { ip ->
                async {
                    val reachable = reach(ip)
                    if (reachable) ip else null
                }
            }.awaitAll().filterNotNull()
        }

        val arp = readArpTable()
        val devices = mutableListOf<LanDevice>()

        val selfMac = arp[selfIp] ?: ""
        devices += LanDevice(
            ip = selfIp,
            mac = selfMac,
            vendor = Formatters.macVendor(selfMac),
            hostname = "this-device",
            isGateway = false,
            isThisDevice = true
        )

        if (gatewayIp.isNotBlank()) {
            val mac = arp[gatewayIp] ?: ""
            devices += LanDevice(
                ip = gatewayIp,
                mac = mac,
                vendor = Formatters.macVendor(mac),
                hostname = resolveHost(gatewayIp),
                isGateway = true
            )
        }

        val known = setOf(selfIp, gatewayIp)
        for ((ip, mac) in arp) {
            if (ip in known) continue
            if (!ip.startsWith(selfIp.substringBeforeLast('.').ifBlank { "0.0.0" })) {
                // keep only same /24
                if (!inSameSubnet(ip, selfIp)) continue
            }
            devices += LanDevice(
                ip = ip,
                mac = mac,
                vendor = Formatters.macVendor(mac),
                hostname = resolveHost(ip)
            )
        }

        devices.distinctBy { it.ip }.sortedWith(
            compareByDescending<LanDevice> { it.isThisDevice || it.isGateway }
                .thenBy { it.ip }
        )
    }

    private fun inSameSubnet(a: String, b: String): Boolean {
        val prefix = b.substringBeforeLast('.')
        return a.startsWith("$prefix.")
    }

    private suspend fun reach(ip: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val addr = InetAddress.getByName(ip)
            addr.isReachable(300)
        } catch (_: Exception) {
            false
        }
    }

    private fun readArpTable(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.readLine() // header
                var line = reader.readLine()
                while (line != null) {
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val mac = parts[3]
                        if (mac != "00:00:00:00:00:00") {
                            map[ip] = mac
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    private fun resolveHost(ip: String): String {
        return try {
            val addr = InetAddress.getByName(ip)
            val host = addr.canonicalHostName
            if (host == ip) "" else host
        } catch (_: Exception) {
            ""
        }
    }

    @Suppress("DEPRECATION")
    fun localIp(): String {
        return try {
            val info = wifiManager.connectionInfo
            val ip = info?.ipAddress ?: 0
            if (ip == 0) {
                NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
                    .filter { it.isUp && !it.isLoopback }
                    .flatMap { it.inetAddresses.toList() }
                    .filterIsInstance<java.net.Inet4Address>()
                    .firstOrNull()?.hostAddress ?: "192.168.1.100"
            } else {
                Formatters.intToIp(Integer.reverseBytes(ip))
            }
        } catch (_: Exception) {
            "192.168.1.100"
        }
    }

    @Suppress("DEPRECATION")
    fun gatewayIp(): String {
        return try {
            val dhcp = wifiManager.dhcpInfo
            if (dhcp == null || dhcp.gateway == 0) ""
            else Formatters.intToIp(Integer.reverseBytes(dhcp.gateway))
        } catch (_: Exception) {
            ""
        }
    }
}
