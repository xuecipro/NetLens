package com.opensource.netlens.data.speed

data class SpeedNode(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val locationZh: String,
    val locationEn: String,
    val downloadUrl: String,
    val uploadUrl: String?,
    val pingHost: String,
    val downloadSizeBytes: Long = 20L * 1024 * 1024,
    val uploadSizeBytes: Long = 6L * 1024 * 1024
)

object SpeedNodes {
    val ALL = listOf(
        SpeedNode(
            id = "cf_auto",
            nameZh = "Cloudflare 自动",
            nameEn = "Cloudflare Auto",
            locationZh = "全球任播",
            locationEn = "Global anycast",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = "https://speed.cloudflare.com/__up",
            pingHost = "1.1.1.1"
        ),
        SpeedNode(
            id = "cf_1111",
            nameZh = "Cloudflare 1.1.1.1",
            nameEn = "Cloudflare 1.1.1.1",
            locationZh = "全球任播",
            locationEn = "Global anycast",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = "https://speed.cloudflare.com/__up",
            pingHost = "1.1.1.1"
        ),
        SpeedNode(
            id = "google",
            nameZh = "Google DNS",
            nameEn = "Google DNS",
            locationZh = "全球任播",
            locationEn = "Global anycast",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = null,
            pingHost = "8.8.8.8"
        ),
        SpeedNode(
            id = "quad9",
            nameZh = "Quad9",
            nameEn = "Quad9",
            locationZh = "全球安全 DNS",
            locationEn = "Global secure DNS",
            downloadUrl = "https://speed.cloudflare.com/__down",
            uploadUrl = null,
            pingHost = "9.9.9.9"
        ),
        SpeedNode(
            id = "gateway",
            nameZh = "本地网关",
            nameEn = "Local gateway",
            locationZh = "局域网",
            locationEn = "LAN",
            downloadUrl = "",
            uploadUrl = null,
            pingHost = "",
            downloadSizeBytes = 0,
            uploadSizeBytes = 0
        )
    )

    fun byId(id: String): SpeedNode = ALL.firstOrNull { it.id == id } ?: ALL.first()
}
