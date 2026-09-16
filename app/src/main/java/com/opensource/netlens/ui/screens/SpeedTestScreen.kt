package com.opensource.netlens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.BuildConfig
import com.opensource.netlens.R
import com.opensource.netlens.data.speed.SpeedNode
import com.opensource.netlens.data.speed.ThroughputUnits
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.ui.components.StatusChip
import com.opensource.netlens.util.Formatters

/**
 * Multi-node speed test UI (v1.1). Node picker is the first block so it is
 * impossible to miss after installing the new APK.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val isZh = java.util.Locale.getDefault().language.startsWith("zh")
    val node = vm.selectedSpeedNode()

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ---- Header with version badge ----
        item {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.speed_title),
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "v${BuildConfig.VERSION_NAME} · MULTI-NODE 节点版",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        if (isZh) "本页为多节点测速（非旧版单节点）"
                        else "Multi-node speed test page (not the old single-node UI)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        if (isZh) "若仍看到「测速节点: 自动（Cloudflare）」旧界面，请卸载后重装此 APK"
                        else "If you still see the old 'Auto (Cloudflare)' UI, uninstall and reinstall this APK",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---- Node picker FIRST ----
        item {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isZh) "选择测速节点" else "Select speed node",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                StatusChip(
                    "${vm.availableSpeedNodes().size} 节点",
                    MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (isZh) "当前：${node.nameZh}（${node.regionZh}）"
                else "Current: ${node.nameEn} (${node.regionEn})",
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
        }

        items(vm.availableSpeedNodes(), key = { it.id }) { n ->
            NodeDetailCard(
                node = n,
                selected = n.id == state.selectedSpeedNodeId,
                isZh = isZh,
                onSelect = { vm.setSpeedNode(n.id) }
            )
        }

        // ---- Start button ----
        item {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { vm.runSpeedTest() },
                enabled = !state.isTestingSpeed,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isTestingSpeed) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.status_running))
                } else {
                    Icon(Icons.Rounded.Speed, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isZh) "开始测速 · ${node.nameZh}"
                        else "Start · ${node.nameEn}"
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ---- Progress / Results ----
        if (state.isTestingSpeed) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            phaseLabel(state.speedPhase),
                            fontWeight = FontWeight.SemiBold
                        )
                        LinearProgressIndicator(
                            progress = { state.speedProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                        )
                        Text(
                            stringResource(R.string.speed_progress, state.speedProgress),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        state.lastSpeed?.let { s ->
            item {
                Column(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (isZh) "测速结果（节点：${s.serverLabel}）"
                        else "Result (node: ${s.serverLabel})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    DualSpeedCard(
                        title = stringResource(R.string.speed_download),
                        mbps = s.downloadMbps,
                        isZh = isZh,
                        accent = Color(0xFF1B6EF3)
                    )
                    DualSpeedCard(
                        title = stringResource(R.string.speed_upload),
                        mbps = s.uploadMbps,
                        isZh = isZh,
                        accent = Color(0xFF0D9488)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStat(Modifier.weight(1f), stringResource(R.string.speed_latency), Formatters.ms(s.latencyMs), "ms")
                        MiniStat(Modifier.weight(1f), stringResource(R.string.speed_jitter), Formatters.ms(s.jitterMs), "ms")
                        MiniStat(Modifier.weight(1f), stringResource(R.string.speed_packet_loss), Formatters.percent(s.packetLossPercent), "%")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${s.score}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(s.grade, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.speed_score), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        state.statusMessage.takeIf { it.isNotBlank() }?.let { msg ->
            item {
                Text(
                    msg,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Text(
                stringResource(R.string.speed_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.speedHistory.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.speed_no_history),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.speedHistory) { h ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(h.serverLabel, fontWeight = FontWeight.SemiBold)
                            Text(
                                ThroughputUnits.formatDual(h.downloadMbps, isZh),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "↑ ${ThroughputUnits.fmt(h.uploadMbps)} Mbps · ${Formatters.ms(h.latencyMs)} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text("${h.score}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
fun NodeDetailCard(
    node: SpeedNode,
    selected: Boolean,
    isZh: Boolean,
    onSelect: () -> Unit
) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (node.isGateway) Icons.Rounded.Router else Icons.Rounded.Public,
                    contentDescription = null,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isZh) node.nameZh else node.nameEn,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    "ISP  ${if (isZh) node.ispZh else node.ispEn}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    if (isZh) node.regionZh else node.regionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (node.host.isNotBlank()) {
                    Text(
                        "Host  ${node.host}  ·  ${node.protocol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (node.pingHost.isNotBlank()) {
                    Text(
                        "Ping  ${node.pingHost}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(4.dp))
                if (node.isGateway) {
                    StatusChip(if (isZh) "仅延迟 / 丢包" else "Latency / loss only", Color(0xFF7C3AED))
                } else {
                    val mb = (node.downloadSizeBytes / 1024 / 1024).toInt()
                    StatusChip(
                        if (isZh) "下载样本 ~${mb}MB · 可测上下行" else "~${mb}MB sample · DL/UL",
                        MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun DualSpeedCard(title: String, mbps: Double, isZh: Boolean, accent: Color) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${ThroughputUnits.fmt(mbps)} Mbps",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                "${ThroughputUnits.fmt(ThroughputUnits.mbpsToMBs(mbps))} MB/s",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accent.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun MiniStat(modifier: Modifier = Modifier, title: String, value: String, unit: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun phaseLabel(phase: String): String = when (phase) {
    "download" -> stringResource(R.string.speed_testing_download)
    "upload" -> stringResource(R.string.speed_testing_upload)
    "latency" -> stringResource(R.string.speed_testing_latency)
    "done" -> stringResource(R.string.status_done)
    "error" -> stringResource(R.string.status_error)
    else -> stringResource(R.string.status_running)
}
