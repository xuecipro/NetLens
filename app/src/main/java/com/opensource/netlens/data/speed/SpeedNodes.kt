package com.opensource.netlens.data.speed

/**
 * Multi-node speed test catalog, modeled after domestic speedtest sites:
 * each node has ISP/region/host metadata and its own endpoints.
 */
data class SpeedNode(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val ispZh: String,
    val ispEn: String,
    val regionZh: String,
    val regionEn: String,
    val host: String,
    val downloadUrl: String,
    val uploadUrl: String?,
    val pingHost: String,
    val protocol: String = "HTTPS",
    val downloadSizeBytes: Long = 25L * 1024 * 1024,
    val uploadSizeBytes: Long = 8L * 1024 * 1024,
    val isGateway: Boolean = false
)

object SpeedNodes {

    val ALL: List<SpeedNode> = listOf(
        SpeedNode(
            id = "cf_auto",
            nameZh = "Cloudflare 全球",
            nameEn = "Cloudflare Global",
            ispZh = "Cloudflare",
            ispEn = "Cloudflare",
            regionZh = "全球任播（自动就近）",
            regionEn = "Global anycast (auto)",
            host = "speed.cloudflare.com",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = "https://speed.cloudflare.com/__up",
            pingHost = "1.1.1.1",
            protocol = "HTTPS"
        ),
        SpeedNode(
            id = "cf_1111",
            nameZh = "Cloudflare DNS",
            nameEn = "Cloudflare DNS",
            ispZh = "Cloudflare",
            ispEn = "Cloudflare",
            regionZh = "全球任播 · 1.1.1.1",
            regionEn = "Global anycast · 1.1.1.1",
            host = "speed.cloudflare.com",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = "https://speed.cloudflare.com/__up",
            pingHost = "1.1.1.1"
        ),
        SpeedNode(
            id = "google_dns",
            nameZh = "Google 公共 DNS",
            nameEn = "Google Public DNS",
            ispZh = "Google",
            ispEn = "Google",
            regionZh = "全球任播 · 8.8.8.8",
            regionEn = "Global anycast · 8.8.8.8",
            host = "8.8.8.8",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = null,
            pingHost = "8.8.8.8",
            downloadSizeBytes = 15L * 1024 * 1024
        ),
        SpeedNode(
            id = "quad9",
            nameZh = "Quad9 安全 DNS",
            nameEn = "Quad9 Secure DNS",
            ispZh = "Quad9 / IBM",
            ispEn = "Quad9 / IBM",
            regionZh = "全球安全解析 · 9.9.9.9",
            regionEn = "Global secure DNS · 9.9.9.9",
            host = "9.9.9.9",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = null,
            pingHost = "9.9.9.9",
            downloadSizeBytes = 15L * 1024 * 1024
        ),
        SpeedNode(
            id = "ovh_eu",
            nameZh = "OVH 欧洲",
            nameEn = "OVH Europe",
            ispZh = "OVHcloud",
            ispEn = "OVHcloud",
            regionZh = "欧洲 · 法国/加拿大镜像",
            regionEn = "Europe · FR/CA mirrors",
            host = "proof.ovh.net",
            downloadUrl = "https://proof.ovh.net/files/100Mb.dat",
            uploadUrl = null,
            pingHost = "proof.ovh.net",
            downloadSizeBytes = 50L * 1024 * 1024
        ),
        SpeedNode(
            id = "tele2_eu",
            nameZh = "Tele2 欧洲",
            nameEn = "Tele2 Europe",
            ispZh = "Tele2",
            ispEn = "Tele2",
            regionZh = "欧洲 · 公开测速镜像",
            regionEn = "Europe · public mirror",
            host = "speedtest.tele2.net",
            downloadUrl = "http://speedtest.tele2.net/100MB.zip",
            uploadUrl = null,
            pingHost = "speedtest.tele2.net",
            protocol = "HTTP",
            downloadSizeBytes = 40L * 1024 * 1024
        ),
        SpeedNode(
            id = "thinkbroadband_uk",
            nameZh = "ThinkBroadband 英国",
            nameEn = "ThinkBroadband UK",
            ispZh = "ThinkBroadband",
            ispEn = "ThinkBroadband",
            regionZh = "英国 · 公开大文件",
            regionEn = "UK · public large file",
            host = "ipv4.download.thinkbroadband.com",
            downloadUrl = "http://ipv4.download.thinkbroadband.com/100MB.zip",
            uploadUrl = null,
            pingHost = "ipv4.download.thinkbroadband.com",
            protocol = "HTTP",
            downloadSizeBytes = 40L * 1024 * 1024
        ),
        SpeedNode(
            id = "gateway",
            nameZh = "本地网关（路由器）",
            nameEn = "Local gateway (router)",
            ispZh = "局域网",
            ispEn = "LAN",
            regionZh = "本机到网关 · 仅测延迟/丢包",
            regionEn = "Device → gateway · latency/loss only",
            host = "",
            downloadUrl = "",
            uploadUrl = null,
            pingHost = "",
            downloadSizeBytes = 0L,
            uploadSizeBytes = 0L,
            isGateway = true
        )
    )

    fun byId(id: String): SpeedNode = ALL.firstOrNull { it.id == id } ?: ALL.first()
}

object ThroughputUnits {
    /** Mbps → MB/s (megabytes). 1 byte = 8 bits. */
    fun mbpsToMBs(mbps: Double): Double = mbps / 8.0

    fun formatDual(mbps: Double, zh: Boolean): String {
        val mbs = mbpsToMBs(mbps)
        return if (zh) {
            "${fmt(mbps)} Mbps（${fmt(mbs)} MB/s）"
        } else {
            "${fmt(mbps)} Mbps (${fmt(mbs)} MB/s)"
        }
    }

    fun fmt(v: Double): String = when {
        v >= 100 -> String.format("%.0f", v)
        v >= 10 -> String.format("%.1f", v)
        else -> String.format("%.2f", v)
    }
}
