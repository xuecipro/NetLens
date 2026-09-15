package com.opensource.netlens.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.util.Formatters
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.speed_title), fontWeight = FontWeight.ExtraBold)
                    Text(
                        stringResource(R.string.speed_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.speed_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${stringResource(R.string.speed_server)}: ${stringResource(R.string.speed_server_auto)}", style = MaterialTheme.typography.bodySmall)

            Card {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val score = state.lastSpeed?.score ?: 0
                    SpeedGauge(score = score, grade = state.lastSpeed?.grade ?: "-")
                    Spacer(Modifier.height(8.dp))
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
                            phaseLabel(state.speedPhase) + " " + stringResource(R.string.speed_progress, state.speedProgress),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (state.lastSpeed != null) {
                val s = state.lastSpeed!!
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultTile(
                        Modifier.weight(1f),
                        stringResource(R.string.speed_download),
                        Formatters.mbps(s.downloadMbps),
                        stringResource(R.string.unit_mbps)
                    )
                    ResultTile(
                        Modifier.weight(1f),
                        stringResource(R.string.speed_upload),
                        Formatters.mbps(s.uploadMbps),
                        stringResource(R.string.unit_mbps)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            } else if (state.ping != null) {
                val p = state.ping!!
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResultTile(Modifier.weight(1f), stringResource(R.string.speed_latency), Formatters.ms(p.avgMs), stringResource(R.string.unit_ms))
                    ResultTile(Modifier.weight(1f), stringResource(R.string.speed_jitter), Formatters.ms(p.jitterMs), stringResource(R.string.unit_ms))
                    ResultTile(Modifier.weight(1f), stringResource(R.string.speed_packet_loss), Formatters.percent(p.lossPercent), stringResource(R.string.unit_percent))
                }
            }

            Button(
                onClick = { vm.runSpeedTest() },
                enabled = !state.isTestingSpeed,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isTestingSpeed) stringResource(R.string.status_running)
                    else stringResource(R.string.speed_start)
                )
            }

            if (state.statusMessage.isNotBlank()) {
                Text(state.statusMessage, color = MaterialTheme.colorScheme.error)
            }

            Text(stringResource(R.string.speed_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (state.speedHistory.isEmpty()) {
                Text(stringResource(R.string.speed_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                state.speedHistory.forEach { item ->
                    Card {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${Formatters.mbps(item.downloadMbps)} / ${Formatters.mbps(item.uploadMbps)} Mbps", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Ping ${Formatters.ms(item.latencyMs)} ms · Jitter ${Formatters.ms(item.jitterMs)} ms · Loss ${Formatters.percent(item.packetLossPercent)}%",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("${item.score}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
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
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun SpeedGauge(score: Int, grade: String) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
        Canvas(Modifier.size(180.dp)) {
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
