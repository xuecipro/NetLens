package com.opensource.netlens.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.opensource.netlens.data.action.NetworkActionExecutor
import com.opensource.netlens.data.advisor.NetworkAdvisor
import com.opensource.netlens.data.devices.DeviceScanner
import com.opensource.netlens.data.model.AdviceAction
import com.opensource.netlens.data.model.AdviceItem
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.ChannelRating
import com.opensource.netlens.data.model.CurrentConnection
import com.opensource.netlens.data.model.LanDevice
import com.opensource.netlens.data.model.PingStats
import com.opensource.netlens.data.model.SpeedResult
import com.opensource.netlens.data.model.WifiNetwork
import com.opensource.netlens.data.speed.PingEngine
import com.opensource.netlens.data.speed.SpeedNode
import com.opensource.netlens.data.speed.SpeedNodes
import com.opensource.netlens.data.speed.SpeedTestEngine
import com.opensource.netlens.data.wifi.WifiRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val permissionNeeded: Boolean = true,
    val connection: CurrentConnection? = null,
    val networks: List<WifiNetwork> = emptyList(),
    val devices: List<LanDevice> = emptyList(),
    val lastSpeed: SpeedResult? = null,
    val speedHistory: List<SpeedResult> = emptyList(),
    val ping: PingStats? = null,
    val advice: List<AdviceItem> = emptyList(),
    val isScanningWifi: Boolean = false,
    val isScanningDevices: Boolean = false,
    val isTestingSpeed: Boolean = false,
    val speedPhase: String = "",
    val speedProgress: Int = 0,
    val statusMessage: String = "",
    val selectedBand: Band? = null,
    val scanIntervalMs: Long = 3000L,
    val themeMode: String = "system",
    val actionMessage: String? = null,
    val selectedSpeedNodeId: String = "cf_auto"
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val wifiRepo = WifiRepository(app)
    private val deviceScanner = DeviceScanner(app)
    private val pingEngine = PingEngine()
    private val speedEngine = SpeedTestEngine()
    private val actionExecutor = NetworkActionExecutor(app)

    private val prefs = app.getSharedPreferences("netlens_prefs", android.content.Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(UiState(scanIntervalMs = prefs.getLong("scan_interval", 3000L), themeMode = prefs.getString("theme", "system") ?: "system"))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var wifiJob: Job? = null
    private var connJob: Job? = null

    init {
        startWifiObserving()
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(permissionNeeded = !granted) }
        if (granted) refreshWifi()
    }

    fun startWifiObserving() {
        wifiJob?.cancel()
        connJob?.cancel()
        wifiJob = viewModelScope.launch {
            wifiRepo.observeNetworks(_state.value.scanIntervalMs).collect { list ->
                _state.update { it.copy(networks = list, isScanningWifi = false) }
                rebuildAdvice()
            }
        }
        connJob = viewModelScope.launch {
            wifiRepo.observeConnection().collect { conn ->
                _state.update { it.copy(connection = conn) }
                rebuildAdvice()
            }
        }
    }

    fun refreshWifi() {
        viewModelScope.launch {
            _state.update { it.copy(isScanningWifi = true) }
            wifiRepo.triggerScan()
            val nets = wifiRepo.scanNetworks()
            val conn = wifiRepo.currentConnection()
            _state.update {
                it.copy(
                    networks = nets,
                    connection = conn,
                    isScanningWifi = false,
                    statusMessage = ""
                )
            }
            rebuildAdvice()
        }
    }

    fun scanDevices() {
        viewModelScope.launch {
            _state.update { it.copy(isScanningDevices = true, statusMessage = "Scanning LAN…") }
            try {
                val devices = deviceScanner.scan()
                _state.update {
                    it.copy(devices = devices, isScanningDevices = false, statusMessage = "")
                }
                rebuildAdvice()
            } catch (e: Exception) {
                _state.update {
                    it.copy(isScanningDevices = false, statusMessage = e.message ?: "Scan failed")
                }
            }
        }
    }

    fun setSpeedNode(id: String) {
        _state.update { it.copy(selectedSpeedNodeId = id) }
    }

    fun selectedSpeedNode(): SpeedNode = SpeedNodes.byId(_state.value.selectedSpeedNodeId)

    fun availableSpeedNodes(): List<SpeedNode> = SpeedNodes.ALL

    fun connectToNetwork(network: WifiNetwork) {
        val result = actionExecutor.connectToNetwork(network)
        handleActionResult(result)
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500)
            refreshWifi()
        }
    }

    fun switchApChannel(channel: Int) {
        val conn = _state.value.connection
        val result = actionExecutor.switchChannel(channel, conn?.ssid, conn?.gateway)
        handleActionResult(result)
    }

    private fun handleActionResult(result: NetworkActionExecutor.ActionResult) {
        when (result) {
            is NetworkActionExecutor.ActionResult.Success -> {
                val msg = if (java.util.Locale.getDefault().language.startsWith("zh"))
                    result.messageZh else result.messageEn
                _state.update { it.copy(actionMessage = msg) }
                actionExecutor.toast(msg)
            }
            is NetworkActionExecutor.ActionResult.Navigation -> {
                runCatching {
                    getApplication<Application>().startActivity(result.intent)
                }
                _state.update { it.copy(actionMessage = "已打开系统设置 / Opened system settings") }
            }
            is NetworkActionExecutor.ActionResult.Error -> {
                val msg = if (java.util.Locale.getDefault().language.startsWith("zh"))
                    result.messageZh else result.messageEn
                _state.update { it.copy(actionMessage = msg) }
                actionExecutor.toast(msg)
            }
            is NetworkActionExecutor.ActionResult.NeedsPermission -> {
                _state.update { it.copy(actionMessage = "需要相关权限 / Permission required") }
            }
        }
    }

    fun runSpeedTest() {
        if (_state.value.isTestingSpeed) return
        val node = selectedSpeedNode()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isTestingSpeed = true,
                    speedProgress = 0,
                    speedPhase = "latency",
                    statusMessage = ""
                )
            }
            try {
                val pingHost = node.pingHost.ifBlank {
                    _state.value.connection?.gateway?.ifBlank { "1.1.1.1" } ?: "1.1.1.1"
                }
                val ping = pingEngine.measure(host = pingHost, count = 10)
                _state.update {
                    it.copy(ping = ping, speedPhase = "download", speedProgress = 5)
                }

                val download = if (node.downloadUrl.isBlank()) {
                    0.0
                } else {
                    speedEngine.download(
                        sizeBytes = node.downloadSizeBytes,
                        urlOverride = node.downloadUrl
                    ) { p ->
                        _state.update {
                            it.copy(speedProgress = 5 + (p.percent * 45) / 100)
                        }
                    }
                }

                _state.update { it.copy(speedPhase = "upload", speedProgress = 55) }

                val upload = try {
                    if (node.uploadUrl.isNullOrBlank()) 0.0
                    else speedEngine.upload(
                        sizeBytes = node.uploadSizeBytes,
                        urlOverride = node.uploadUrl
                    ) { p ->
                        _state.update {
                            it.copy(speedProgress = 55 + (p.percent * 40) / 100)
                        }
                    }
                } catch (_: Exception) {
                    0.0
                }

                val (score, grade) = SpeedTestEngine.scoreOf(
                    downloadMbps = download,
                    uploadMbps = upload,
                    latencyMs = ping.avgMs,
                    jitterMs = ping.jitterMs,
                    lossPercent = ping.lossPercent
                )

                val label = if (java.util.Locale.getDefault().language.startsWith("zh"))
                    node.nameZh else node.nameEn

                val result = SpeedResult(
                    downloadMbps = download,
                    uploadMbps = upload,
                    latencyMs = ping.avgMs,
                    jitterMs = ping.jitterMs,
                    packetLossPercent = ping.lossPercent,
                    serverLabel = label,
                    timestamp = System.currentTimeMillis(),
                    score = if (node.downloadUrl.isBlank()) {
                        SpeedTestEngine.scoreOf(0.0, 0.0, ping.avgMs, ping.jitterMs, ping.lossPercent).first
                    } else score,
                    grade = if (node.downloadUrl.isBlank()) {
                        SpeedTestEngine.scoreOf(0.0, 0.0, ping.avgMs, ping.jitterMs, ping.lossPercent).second
                    } else grade
                )

                _state.update {
                    it.copy(
                        lastSpeed = result,
                        speedHistory = (listOf(result) + it.speedHistory).take(20),
                        ping = ping,
                        isTestingSpeed = false,
                        speedPhase = "done",
                        speedProgress = 100
                    )
                }
                rebuildAdvice()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isTestingSpeed = false,
                        speedPhase = "error",
                        statusMessage = e.message ?: "Speed test failed"
                    )
                }
            }
        }
    }

    fun setSelectedBand(band: Band?) {
        _state.update { it.copy(selectedBand = band) }
    }

    fun rebuildAdvice() {
        val s = _state.value
        val advice = NetworkAdvisor.analyze(
            connection = s.connection,
            networks = s.networks,
            speed = s.lastSpeed,
            ping = s.ping
        )
        _state.update { it.copy(advice = advice) }
    }

    fun clearActionMessage() {
        _state.update { it.copy(actionMessage = null) }
    }

    /**
     * Run an executable optimization strategy from an advice card.
     * @param onNavigateToSpeed optional callback when the action is "run speed test"
     * @param onNavigateToDevices optional callback when the action is "scan devices"
     */
    fun executeAdvice(
        advice: AdviceItem,
        onNavigateToSpeed: () -> Unit = {},
        onNavigateToDevices: () -> Unit = {}
    ) {
        val s = _state.value
        when (advice.action) {
            AdviceAction.RUN_SPEED_TEST -> {
                onNavigateToSpeed()
                runSpeedTest()
                _state.update { it.copy(actionMessage = "已开始测速 / Speed test started") }
            }
            AdviceAction.RUN_DEVICE_SCAN -> {
                onNavigateToDevices()
                scanDevices()
                _state.update { it.copy(actionMessage = "正在扫描局域网 / Scanning LAN…") }
            }
            else -> {
                val result = actionExecutor.execute(
                    action = advice.action,
                    networks = s.networks,
                    currentSsid = s.connection?.ssid,
                    currentBand = s.connection?.band,
                    param = advice.actionParam ?: s.connection?.gateway,
                    targetBssid = advice.targetBssid,
                    targetSsid = advice.targetSsid
                )
                when (result) {
                    is NetworkActionExecutor.ActionResult.Success -> {
                        val msg = if (java.util.Locale.getDefault().language.startsWith("zh"))
                            result.messageZh else result.messageEn
                        _state.update { it.copy(actionMessage = msg) }
                        actionExecutor.toast(msg)
                        // Refresh so the user sees post-action state
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(1200)
                            refreshWifi()
                        }
                    }
                    is NetworkActionExecutor.ActionResult.Navigation -> {
                        runCatching {
                            getApplication<Application>().startActivity(result.intent)
                        }.onFailure {
                            _state.update {
                                it.copy(actionMessage = "无法打开系统页面 / Cannot open settings")
                            }
                        }
                        _state.update {
                            it.copy(actionMessage = "已打开系统设置 / Opened system settings")
                        }
                    }
                    is NetworkActionExecutor.ActionResult.Error -> {
                        val msg = if (java.util.Locale.getDefault().language.startsWith("zh"))
                            result.messageZh else result.messageEn
                        _state.update { it.copy(actionMessage = msg) }
                        actionExecutor.toast(msg)
                    }
                    is NetworkActionExecutor.ActionResult.NeedsPermission -> {
                        _state.update { it.copy(actionMessage = "需要相关权限 / Permission required") }
                    }
                }
            }
        }
        rebuildAdvice()
    }

    fun setScanInterval(ms: Long) {
        prefs.edit().putLong("scan_interval", ms).apply()
        _state.update { it.copy(scanIntervalMs = ms) }
        startWifiObserving()
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme", mode).apply()
        _state.update { it.copy(themeMode = mode) }
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
    }

    fun getLanguage(): String = prefs.getString("language", "system") ?: "system"

    fun channelRatings(band: Band): List<ChannelRating> =
        WifiRepository.channelRatings(_state.value.networks, band)

    fun is5Supported(): Boolean = wifiRepo.is5GhzSupported()
    fun is6Supported(): Boolean = wifiRepo.is6GhzSupported()

    fun shareReport(): String {
        val s = _state.value
        val conn = s.connection
        val speed = s.lastSpeed
        buildString {
            appendLine("NetLens Report")
            appendLine("==============")
            appendLine("SSID: ${conn?.ssid ?: "-"}")
            appendLine("BSSID: ${conn?.bssid ?: "-"}")
            appendLine("RSSI: ${conn?.rssi ?: "-"} dBm")
            appendLine("Band: ${conn?.band?.label ?: "-"}  Channel: ${conn?.channel ?: "-"}")
            appendLine("Link: ${conn?.linkSpeedMbps ?: "-"} Mbps  Std: ${conn?.wifiStandard ?: "-"}")
            appendLine("Security: ${conn?.security ?: "-"}")
            appendLine("IP/GW/DNS: ${conn?.ipAddress ?: "-"} / ${conn?.gateway ?: "-"} / ${conn?.dns ?: "-"}")
            appendLine("Networks scanned: ${s.networks.size}")
            if (speed != null) {
                appendLine("Download: ${"%.1f".format(speed.downloadMbps)} Mbps")
                appendLine("Upload: ${"%.1f".format(speed.uploadMbps)} Mbps")
                appendLine("Latency: ${"%.1f".format(speed.latencyMs)} ms")
                appendLine("Jitter: ${"%.1f".format(speed.jitterMs)} ms")
                appendLine("Loss: ${"%.1f".format(speed.packetLossPercent)}%")
                appendLine("Score: ${speed.score} (${speed.grade})")
            }
            appendLine("Advice:")
            s.advice.forEach { a ->
                appendLine("- [${a.severity}] ${a.titleZh} / ${a.titleEn}")
            }
        }.also { report ->
            val clip = android.content.ClipData.newPlainText("NetLens Report", report)
            val cm = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(clip)
        }
        return buildShareText()
    }

    private fun buildShareText(): String {
        val s = _state.value
        val conn = s.connection
        val speed = s.lastSpeed
        return buildString {
            appendLine("NetLens 网络报告 / Report")
            appendLine("SSID: ${conn?.ssid ?: "-"}")
            appendLine("RSSI: ${conn?.rssi ?: "-"} dBm | ${conn?.band?.label ?: "-"} CH${conn?.channel ?: "-"}")
            if (speed != null) {
                appendLine("↓ ${"%.1f".format(speed.downloadMbps)} Mbps  ↑ ${"%.1f".format(speed.uploadMbps)} Mbps")
                appendLine("Ping ${"%.1f".format(speed.latencyMs)} ms  Jitter ${"%.1f".format(speed.jitterMs)} ms  Loss ${"%.1f".format(speed.packetLossPercent)}%")
                appendLine("Score ${speed.score} (${speed.grade})")
            }
            s.advice.take(5).forEach { appendLine("• ${it.titleZh} / ${it.titleEn}") }
        }
    }
}
