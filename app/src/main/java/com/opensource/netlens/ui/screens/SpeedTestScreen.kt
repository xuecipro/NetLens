package com.opensource.netlens.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.data.speed.SpeedNode
import com.opensource.netlens.data.speed.ThroughputUnits
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.ui.components.StatusChip
import com.opensource.netlens.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val isZh = java.util.Locale.getDefault().language.startsWith("zh")
    val node = vm.selectedSpeedNode()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.speed_title), fontWeight = FontWeight.ExtraBold)
                        Text(
                            "多节点测速 · Mbps / MB/s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.speed_server),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isZh) "点击下方节点切换；当前：${node.nameZh}"
                    else "Tap a node to switch. Current: ${node.nameEn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        items(vm.availableSpeedNodes(), key = { it.id }) { n ->
            NodeCard(
                node = n,
                selected = n.id == state.selectedSpeedNodeId,
                isZh = isZh,
                onSelect = { vm.setSpeedNode(n.id) }
            )
        }

        item {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val score = state.lastSpeed?.score ?: 0
                    SpeedGauge(score = score, grade = state.lastSpeed?.grade ?: "-")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.speed_score),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (state.isTestingSpeed) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { state.speedProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            phaseLabel(state.speedPhase) + " " +
                                stringResource(R.string.speed_progress, state.speedProgress),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (state.lastSpeed != null) {
            item {
                val s = state.lastSpeed!!
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "${stringResource(R.string.speed_server)}: ${s.serverLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DualUnitTile(
                        title = stringResource(R.string.speed_download),
                        mbps = s.downloadMbps,
                        isZh = isZh,
                        accent = Color(0xFF1B6EF3)
                    )
                    DualUnitTile(
                        title = stringResource(R.string.speed_upload),
                        mbps = s.uploadMbps,
                        isZh = isZh,
                        accent = Color(0xFF0D9488)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResultTile(
                            Modifier.weight(1f),
                            stringResource(R.string.speed_latency),
                            Formatters.ms(s.latencyMs),
                            stringResource(R.string.unit_ms)
                        )
                        ResultTile(
                            Modifier.weight(1f),
                            stringResource(R.string.speed_jitter),
                            Formatters.ms(s.jitterMs),
                            stringResource(R.string.unit_ms)
                        )
                        ResultTile(
                            Modifier.weight(1f),
                            stringResource(R.string.speed_packet_loss),
                            Formatters.percent(s.packetLossPercent),
                            stringResource(R.string.unit_percent)
                        )
                    }
                    Button(
                        onClick = { vm.runSpeedTest() },
                        enabled = !state.isTestingSpeed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (state.isTestingSpeed) stringResource(R.string.status_running)
                            else "${stringResource(R.string.speed_start)} · ${if (isZh) node.nameZh else node.nameEn}"
                        )
                    }
                }
            }
        } else if (state.ping != null) {
            item {
                val p = state.ping!!
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResultTile(Modifier.weight(1f), stringResource(R.string.speed_latency), Formatters.ms(p.avgMs), stringResource(R.string.unit_ms))
                        ResultTile(Modifier.weight(1f), stringResource(R.string.speed_jitter), Formatters.ms(p.jitterMs), stringResource(R.string.unit_ms))
                        ResultTile(Modifier.weight(1f), stringResource(R.string.speed_packet_loss), Formatters.percent(p.lossPercent), stringResource(R.string.unit_percent))
                    }
                    Button(
                        onClick = { vm.runSpeedTest() },
                        enabled = !state.isTestingSpeed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (state.isTestingSpeed) stringResource(R.string.status_running)
                            else "${stringResource(R.string.speed_start)} · ${if (isZh) node.nameZh else node.nameEn}"
                        )
                    }
                }
            }
        } else {
            item {
                Column(Modifier.padding(16.dp)) {
                    Button(
                        onClick = { vm.runSpeedTest() },
                        enabled = !state.isTestingSpeed,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("${stringResource(R.string.speed_start)} · ${if (isZh) node.nameZh else node.nameEn}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.speed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.statusMessage.isNotBlank()) {
            item {
                Text(
                    state.statusMessage,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        item {
            Text(
                stringResource(R.string.speed_history),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.speedHistory.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.speed_no_history),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(state.speedHistory.size) { idx ->
                val item = state.speedHistory[idx]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.serverLabel, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            Text(
                                ThroughputUnits.formatDual(item.downloadMbps, isZh),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "↑ ${ThroughputUnits.fmt(item.uploadMbps)} Mbps · ping ${Formatters.ms(item.latencyMs)} ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${item.score}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun NodeCard(
    node: SpeedNode,
    selected: Boolean,
    isZh: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (node.isGateway) Icons.Rounded.Router else Icons.Rounded.Public,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isZh) node.nameZh else node.nameEn,
                        fontWeight = FontWeight.Bold,
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
                    "ISP: ${if (isZh) node.ispZh else node.ispEn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (isZh) node.regionZh else node.regionEn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (node.host.isNotBlank()) {
                    Text(
                        "Host: ${node.host} · ${node.protocol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (node.pingHost.isNotBlank()) {
                    Text(
                        "Ping: ${node.pingHost}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!node.downloadUrl.isBlank()) {
                    val mb = node.downloadSizeBytes / 1024 / 1024
                    StatusChip(
                        if (isZh) "样本约 ${mb}MB" else "~${mb}MB sample",
                        MaterialTheme.colorScheme.secondary
                    )
                } else if (node.isGateway) {
                    StatusChip(
                        if (isZh) "仅延迟/丢包" else "Latency/loss only",
                        MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun DualUnitTile(title: String, mbps: Double, isZh: Boolean, accent: Color) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${ThroughputUnits.fmt(mbps)} Mbps",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent
            )
            Text(
                "${ThroughputUnits.fmt(ThroughputUnits.mbpsToMBs(mbps))} MB/s",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent.copy(alpha = 0.85f)
            )
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

@Composable
fun ResultTile(modifier: Modifier = Modifier, title: String, value: String, unit: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SpeedGauge(score: Int, grade: String) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
        Canvas(Modifier.size(170.dp)) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val startAngle = 140f
            val sweep = 260f
            drawArc(
                color = track,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            )
            val frac = score.coerceIn(0, 100) / 100f
            drawArc(
                color = primary,
                startAngle = startAngle,
                sweepAngle = sweep * frac,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                topLeft = Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$score", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(grade, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
