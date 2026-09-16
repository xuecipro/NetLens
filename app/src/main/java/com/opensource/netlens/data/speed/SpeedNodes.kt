package com.opensource.netlens.data.speed

/**
 * Speed test catalog.
 *
 * Domestic strategy (CN campus/consumer networks):
 * - Prefer always-reachable anycast DNS for latency/jitter/loss.
 * - For throughput, try multiple public mirror URLs in order; first 200 wins.
 * - Do NOT rely on a single Ubuntu ISO path (versions 404 often).
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
    val downloadUrls: List<String>,
    val uploadUrl: String?,
    val pingHost: String,
    val protocol: String = "HTTPS",
    val downloadSizeBytes: Long = 20L * 1024 * 1024,
    val uploadSizeBytes: Long = 8L * 1024 * 1024,
    val isGateway: Boolean = false,
    val domestic: Boolean = false,
    /** Only measure ping/jitter/loss; skip HTTP download. */
    val latencyOnly: Boolean = false
) {
    val primaryDownloadUrl: String get() = downloadUrls.firstOrNull().orEmpty()
}

object SpeedNodes {

    private fun cfDownload(size: Long) = listOf("https://speed.cloudflare.com/__down?bytes=$size")
    private const val CF_UPLOAD = "https://speed.cloudflare.com/__up"

    /** Common large files on public mirrors (multiple fallbacks). */
    private val ALIYUN_DL = listOf(
        "https://mirrors.aliyun.com/ubuntu-releases/24.04.2/ubuntu-24.04.2-desktop-amd64.iso",
        "https://mirrors.aliyun.com/ubuntu-releases/22.04.5/ubuntu-22.04.5-desktop-amd64.iso",
        "https://mirrors.aliyun.com/ubuntu-releases/22.04.4/ubuntu-22.04.4-desktop-amd64.iso",
        "https://mirrors.aliyun.com/debian/ls-lR.gz"
    )
    private val TUNA_DL = listOf(
        "https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/24.04.2/ubuntu-24.04.2-desktop-amd64.iso",
        "https://mirrors.tuna.tsinghua.edu.cn/ubuntu-releases/22.04.5/ubuntu-22.04.5-desktop-amd64.iso",
        "https://mirrors.tuna.tsinghua.edu.cn/kernel.org/linux/kernel/v6.x/linux-6.6.tar.xz",
        "https://mirrors.tuna.tsinghua.edu.cn/debian/ls-lR.gz"
    )
    private val USTC_DL = listOf(
        "https://mirrors.ustc.edu.cn/ubuntu-releases/24.04.2/ubuntu-24.04.2-desktop-amd64.iso",
        "https://mirrors.ustc.edu.cn/ubuntu-releases/22.04.5/ubuntu-22.04.5-desktop-amd64.iso",
        "https://mirrors.ustc.edu.cn/debian/ls-lR.gz"
    )
    private val HUAWEI_DL = listOf(
        "https://mirrors.huaweicloud.com/ubuntu-releases/24.04.2/ubuntu-24.04.2-desktop-amd64.iso",
        "https://mirrors.huaweicloud.com/ubuntu-releases/22.04.5/ubuntu-22.04.5-desktop-amd64.iso",
        "https://mirrors.huaweicloud.com/debian/ls-lR.gz"
    )
    private val TENCENT_DL = listOf(
        "https://mirrors.cloud.tencent.com/ubuntu-releases/24.04.2/ubuntu-24.04.2-desktop-amd64.iso",
        "https://mirrors.cloud.tencent.com/ubuntu-releases/22.04.5/ubuntu-22.04.5-desktop-amd64.iso",
        "https://mirrors.cloud.tencent.com/debian/ls-lR.gz"
    )
    private val SJTU_DL = listOf(
        "https://mirror.sjtu.edu.cn/ubuntu-releases/24.04.2/ubuntu-24.04.2-desktop-amd64.iso",
        "https://mirror.sjtu.edu.cn/ubuntu-releases/22.04.5/ubuntu-22.04.5-desktop-amd64.iso"
    )

    val ALL: List<SpeedNode> = buildList {
        // ---- 国内延迟（几乎必通）----
        add(
            SpeedNode(
                id = "ali_dns",
                nameZh = "阿里 DNS 延迟",
                nameEn = "Aliyun DNS latency",
                ispZh = "阿里云",
                ispEn = "Alibaba Cloud",
                regionZh = "国内任播 · 223.5.5.5",
                regionEn = "CN anycast · 223.5.5.5",
                host = "223.5.5.5",
                downloadUrls = emptyList(),
                uploadUrl = null,
                pingHost = "223.5.5.5",
                latencyOnly = true,
                domestic = true
            )
        )
        add(
            SpeedNode(
                id = "dnspod",
                nameZh = "腾讯 DNSPod 延迟",
                nameEn = "DNSPod latency",
                ispZh = "腾讯云",
                ispEn = "Tencent Cloud",
                regionZh = "国内任播 · 119.29.29.29",
                regionEn = "CN anycast · 119.29.29.29",
                host = "119.29.29.29",
                downloadUrls = emptyList(),
                uploadUrl = null,
                pingHost = "119.29.29.29",
                latencyOnly = true,
                domestic = true
            )
        )
        add(
            SpeedNode(
                id = "dns114",
                nameZh = "114DNS 延迟",
                nameEn = "114DNS latency",
                ispZh = "114DNS",
                ispEn = "114DNS",
                regionZh = "国内 · 114.114.114.114",
                regionEn = "CN · 114.114.114.114",
                host = "114.114.114.114",
                downloadUrls = emptyList(),
                uploadUrl = null,
                pingHost = "114.114.114.114",
                latencyOnly = true,
                domestic = true
            )
        )

        // ---- 国内下载（多 URL 回退）----
        add(
            SpeedNode(
                id = "aliyun",
                nameZh = "阿里云镜像下载",
                nameEn = "Aliyun mirror download",
                ispZh = "阿里云",
                ispEn = "Alibaba Cloud",
                regionZh = "国内 · 多线 CDN",
                regionEn = "CN · multi-line CDN",
                host = "mirrors.aliyun.com",
                downloadUrls = ALIYUN_DL,
                uploadUrl = null,
                pingHost = "mirrors.aliyun.com",
                downloadSizeBytes = 20L * 1024 * 1024,
                domestic = true
            )
        )
        add(
            SpeedNode(
                id = "tuna",
                nameZh = "清华 TUNA 镜像",
                nameEn = "Tsinghua TUNA",
                ispZh = "教育网 / 清华",
                ispEn = "CERNET / Tsinghua",
                regionZh = "国内 · 教育网",
                regionEn = "CN · CERNET",
                host = "mirrors.tuna.tsinghua.edu.cn",
                downloadUrls = TUNA_DL,
                uploadUrl = null,
                pingHost = "mirrors.tuna.tsinghua.edu.cn",
                downloadSizeBytes = 20L * 1024 * 1024,
                domestic = true
            )
        )
        add(
            SpeedNode(
                id = "ustc",
                nameZh = "中科大 USTC 镜像",
                nameEn = "USTC Mirror",
                ispZh = "教育网 / 中科大",
                ispEn = "CERNET / USTC",
                regionZh = "国内 · 教育网",
                regionEn = "CN · CERNET",
                host = "mirrors.ustc.edu.cn",
                downloadUrls = USTC_DL,
                uploadUrl = null,
                pingHost = "mirrors.ustc.edu.cn",
                downloadSizeBytes = 20L * 1024 * 1024,
                domestic = true
            )
        )
        add(
            SpeedNode(
                id = "huawei",
                nameZh = "华为云镜像",
                nameEn = "Huawei Cloud Mirror",
                ispZh = "华为云",
                ispEn = "Huawei Cloud",
                regionZh = "国内 · 多线",
                regionEn = "CN · multi-line",
                host = "mirrors.huaweicloud.com",
                downloadUrls = HUAWEI_DL,
                uploadUrl = null,
                pingHost = "mirrors.huaweicloud.com",
                downloadSizeBytes = 20L * 1024 * 1024,
                domestic = true
            )
        )
        add(
            SpeedNode(
                id = "tencent",
                nameZh = "腾讯云镜像",
                nameEn = "Tencent Cloud Mirror",
                ispZh = "腾讯云",
                ispEn = "Tencent Cloud",
                regionZh = "国内 · 多线",
                regionEn = "CN · multi-line",
                host = "mirrors.cloud.tencent.com",
                downloadUrls = TENCENT_DL,
                uploadUrl = null,
                pingHost = "mirrors.cloud.tencent.com",
                downloadSizeBytes = 20L * 1024 * 1024,
                domestic = true
            )
        )
        add(
            SpeedNode(
                id = "sjtu",
                nameZh = "上海交大镜像",
                nameEn = "SJTU Mirror",
                ispZh = "教育网 / 上海交大",
                ispEn = "CERNET / SJTU",
                regionZh = "国内 · 教育网",
                regionEn = "CN · CERNET",
                host = "mirror.sjtu.edu.cn",
                downloadUrls = SJTU_DL,
                uploadUrl = null,
                pingHost = "mirror.sjtu.edu.cn",
                downloadSizeBytes = 20L * 1024 * 1024,
                domestic = true
            )
        )

        // ---- 国际（可测上传）----
        add(
            SpeedNode(
                id = "cf_auto",
                nameZh = "Cloudflare 全球（含上传）",
                nameEn = "Cloudflare Global (DL+UL)",
                ispZh = "Cloudflare",
                ispEn = "Cloudflare",
                regionZh = "全球任播 · 可测上传",
                regionEn = "Global anycast · upload included",
                host = "speed.cloudflare.com",
                downloadUrls = cfDownload(25L * 1024 * 1024),
                uploadUrl = CF_UPLOAD,
                pingHost = "1.1.1.1",
                downloadSizeBytes = 25L * 1024 * 1024,
                uploadSizeBytes = 8L * 1024 * 1024
            )
        )
        add(
            SpeedNode(
                id = "google_dns",
                nameZh = "Google DNS 延迟",
                nameEn = "Google DNS latency",
                ispZh = "Google",
                ispEn = "Google",
                regionZh = "全球 · 8.8.8.8",
                regionEn = "Global · 8.8.8.8",
                host = "8.8.8.8",
                downloadUrls = emptyList(),
                uploadUrl = null,
                pingHost = "8.8.8.8",
                latencyOnly = true
            )
        )
        add(
            SpeedNode(
                id = "gateway",
                nameZh = "本地网关（路由器）",
                nameEn = "Local gateway (router)",
                ispZh = "局域网",
                ispEn = "LAN",
                regionZh = "本机 → 网关 · 仅延迟/丢包",
                regionEn = "Device → gateway · latency/loss",
                host = "",
                downloadUrls = emptyList(),
                uploadUrl = null,
                pingHost = "",
                downloadSizeBytes = 0L,
                isGateway = true,
                latencyOnly = true,
                domestic = true
            )
        )
    }

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
