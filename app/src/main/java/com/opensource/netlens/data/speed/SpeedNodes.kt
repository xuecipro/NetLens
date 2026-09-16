package com.opensource.netlens.data.speed

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
    val downloadSizeBytes: Long = 20L * 1024 * 1024,
    val uploadSizeBytes: Long = 8L * 1024 * 1024,
    val isGateway: Boolean = false,
    val domestic: Boolean = false
)

/**
 * Speed nodes including domestic CDN / edu mirrors commonly reachable from CN.
 * Domestic nodes mostly provide download samples; upload uses Cloudflare when available.
 */
object SpeedNodes {

    private fun cfDownload(size: Long) = "https://speed.cloudflare.com/__down?bytes=$size"
    private const val CF_UPLOAD = "https://speed.cloudflare.com/__up"

    val ALL: List<SpeedNode> = listOf(
        // ---- 国内 / 教育网镜像（下载样本） ----
        SpeedNode(
            id = "aliyun",
            nameZh = "阿里云镜像",
            nameEn = "Aliyun Mirror",
            ispZh = "阿里云",
            ispEn = "Alibaba Cloud",
            regionZh = "国内 · 多线 CDN",
            regionEn = "CN · multi-line CDN",
            host = "mirrors.aliyun.com",
            downloadUrl = "https://mirrors.aliyun.com/ubuntu-releases/22.04/ubuntu-22.04.5-desktop-amd64.iso",
            uploadUrl = null,
            pingHost = "mirrors.aliyun.com",
            downloadSizeBytes = 30L * 1024 * 1024,
            domestic = true
        ),
        SpeedNode(
            id = "tuna",
            nameZh = "清华 TUNA 镜像",
            nameEn = "Tsinghua TUNA",
            ispZh = "教育网 / 清华",
            ispEn = "CERNET / Tsinghua",
            regionZh = "国内 · 教育网优质出口",
            regionEn = "CN · CERNET",
            host = "mirrors.tuna.tsinghua.edu.cn",
            downloadUrl = "https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/22.04/ubuntu-22.04.5-desktop-amd64.iso",
            uploadUrl = null,
            pingHost = "mirrors.tuna.tsinghua.edu.cn",
            downloadSizeBytes = 30L * 1024 * 1024,
            domestic = true
        ),
        SpeedNode(
            id = "ustc",
            nameZh = "中科大 USTC 镜像",
            nameEn = "USTC Mirror",
            ispZh = "教育网 / 中科大",
            ispEn = "CERNET / USTC",
            regionZh = "国内 · 教育网",
            regionEn = "CN · CERNET",
            host = "mirrors.ustc.edu.cn",
            downloadUrl = "https://mirrors.ustc.edu.cn/ubuntu-releases/22.04/ubuntu-22.04.5-desktop-amd64.iso",
            uploadUrl = null,
            pingHost = "mirrors.ustc.edu.cn",
            downloadSizeBytes = 30L * 1024 * 1024,
            domestic = true
        ),
        SpeedNode(
            id = "huawei",
            nameZh = "华为云镜像",
            nameEn = "Huawei Cloud Mirror",
            ispZh = "华为云",
            ispEn = "Huawei Cloud",
            regionZh = "国内 · 多线",
            regionEn = "CN · multi-line",
            host = "mirrors.huaweicloud.com",
            downloadUrl = "https://mirrors.huaweicloud.com/ubuntu-releases/22.04/ubuntu-22.04.5-desktop-amd64.iso",
            uploadUrl = null,
            pingHost = "mirrors.huaweicloud.com",
            downloadSizeBytes = 30L * 1024 * 1024,
            domestic = true
        ),
        SpeedNode(
            id = "tencent",
            nameZh = "腾讯云镜像",
            nameEn = "Tencent Cloud Mirror",
            ispZh = "腾讯云",
            ispEn = "Tencent Cloud",
            regionZh = "国内 · 多线",
            regionEn = "CN · multi-line",
            host = "mirrors.cloud.tencent.com",
            downloadUrl = "https://mirrors.cloud.tencent.com/ubuntu-releases/22.04/ubuntu-22.04.5-desktop-amd64.iso",
            uploadUrl = null,
            pingHost = "mirrors.cloud.tencent.com",
            downloadSizeBytes = 30L * 1024 * 1024,
            domestic = true
        ),
        SpeedNode(
            id = "netease",
            nameZh = "网易开源镜像",
            nameEn = "NetEase Mirror",
            ispZh = "网易",
            ispEn = "NetEase",
            regionZh = "国内",
            regionEn = "CN",
            host = "mirrors.163.com",
            downloadUrl = "http://mirrors.163.com/ubuntu-releases/22.04/ubuntu-22.04.5-desktop-amd64.iso",
            uploadUrl = null,
            pingHost = "mirrors.163.com",
            protocol = "HTTP",
            downloadSizeBytes = 30L * 1024 * 1024,
            domestic = true
        ),
        SpeedNode(
            id = "sjtu",
            nameZh = "上海交大镜像",
            nameEn = "SJTU Mirror",
            ispZh = "教育网 / 上海交大",
            ispEn = "CERNET / SJTU",
            regionZh = "国内 · 教育网",
            regionEn = "CN · CERNET",
            host = "mirror.sjtu.edu.cn",
            downloadUrl = "https://mirror.sjtu.edu.cn/ubuntu-releases/22.04/ubuntu-22.04.5-desktop-amd64.iso",
            uploadUrl = null,
            pingHost = "mirror.sjtu.edu.cn",
            downloadSizeBytes = 30L * 1024 * 1024,
            domestic = true
        ),
        // ---- 国际（含上传） ----
        SpeedNode(
            id = "cf_auto",
            nameZh = "Cloudflare 全球（含上传）",
            nameEn = "Cloudflare Global (DL+UL)",
            ispZh = "Cloudflare",
            ispEn = "Cloudflare",
            regionZh = "全球任播 · 可测上传",
            regionEn = "Global anycast · upload included",
            host = "speed.cloudflare.com",
            downloadUrl = cfDownload(25L * 1024 * 1024),
            uploadUrl = CF_UPLOAD,
            pingHost = "1.1.1.1",
            downloadSizeBytes = 25L * 1024 * 1024,
            uploadSizeBytes = 8L * 1024 * 1024
        ),
        SpeedNode(
            id = "google_dns",
            nameZh = "Google DNS 延迟",
            nameEn = "Google DNS latency",
            ispZh = "Google",
            ispEn = "Google",
            regionZh = "全球 · 8.8.8.8",
            regionEn = "Global · 8.8.8.8",
            host = "8.8.8.8",
            downloadUrl = cfDownload(12L * 1024 * 1024),
            uploadUrl = null,
            pingHost = "8.8.8.8",
            downloadSizeBytes = 12L * 1024 * 1024
        ),
        SpeedNode(
            id = "gateway",
            nameZh = "本地网关（路由器）",
            nameEn = "Local gateway (router)",
            ispZh = "局域网",
            ispEn = "LAN",
            regionZh = "本机 → 网关 · 仅延迟/丢包",
            regionEn = "Device → gateway · latency/loss",
            host = "",
            downloadUrl = "",
            uploadUrl = null,
            pingHost = "",
            downloadSizeBytes = 0L,
            uploadSizeBytes = 0L,
            isGateway = true,
            domestic = true
        )
    )

    fun byId(id: String): SpeedNode = ALL.firstOrNull { it.id == id } ?: ALL.first()

    fun domestic(): List<SpeedNode> = ALL.filter { it.domestic }
    fun international(): List<SpeedNode> = ALL.filter { !it.domestic }
}

object ThroughputUnits {
    fun mbpsToMBs(mbps: Double): Double = mbps / 8.0

    fun formatDual(mbps: Double, zh: Boolean): String {
        val mbs = mbpsToMBs(mbps)
        return if (zh) "${fmt(mbps)} Mbps（${fmt(mbs)} MB/s）"
        else "${fmt(mbps)} Mbps (${fmt(mbs)} MB/s)"
    }

    fun fmt(v: Double): String = when {
        v >= 100 -> String.format("%.0f", v)
        v >= 10 -> String.format("%.1f", v)
        else -> String.format("%.2f", v)
    }
}
