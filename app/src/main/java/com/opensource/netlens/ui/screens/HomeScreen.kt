package com.opensource.netlens.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.data.model.SignalLabels
import com.opensource.netlens.data.model.signalColor
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenSpeed: () -> Unit,
    onNeedPermissions: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.app_tagline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { vm.refreshWifi() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
                IconButton(onClick = {
                    val text = vm.shareReport()
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(android.content.Intent.createChooser(send, "NetLens"))
                }) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.action_share))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.permissionNeeded) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.permission_title), fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.permission_rationale))
                            Button(onClick = onNeedPermissions) {
                                Text(stringResource(R.string.permission_grant))
                            }
                        }
                    }
                }
            }

            item {
                CurrentWifiCard(state = state)
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_best_channel),
                        value = bestChannelText(vm),
                        subtitle = stringResource(R.string.channel_title)
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.home_connected_devices),
                        value = state.devices.size.toString(),
                        subtitle = stringResource(R.string.devices_title)
                    )
                }
            }

            item {
                LastSpeedCard(speed = state.lastSpeed, onClick = onOpenSpeed)
            }

            item {
                Text(
                    stringResource(R.string.advice_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (state.advice.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.advice_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.advice.take(4).size) { idx ->
                    val a = state.advice[idx]
                    AdviceRow(a)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun bestChannelText(vm: MainViewModel): String {
    val ratings = vm.channelRatings(com.opensource.netlens.data.model.Band.BAND_24)
    val best = ratings.firstOrNull()?.channel
    val ratings5 = vm.channelRatings(com.opensource.netlens.data.model.Band.BAND_5)
    val best5 = ratings5.firstOrNull()?.channel
    return buildString {
        append("2.4G: ")
        append(best?.toString() ?: "-")
        append(" · 5G: ")
        append(best5?.toString() ?: "-")
    }
}

@Composable
fun CurrentWifiCard(state: com.opensource.netlens.ui.UiState) {
    val conn = state.connection
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.home_current_wifi),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                conn?.ssid ?: stringResource(R.string.home_not_connected),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (conn != null) {
                SignalBar(rssi = conn.rssi, modifier = Modifier.fillMaxWidth().height(8.dp))
                Text(
                    "${Formatters.rssiLabel(conn.rssi)} · ${conn.band.label} · CH ${conn.channel}" +
                        " · ${conn.linkSpeedMbps} ${stringResource(R.string.unit_mbps)}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${stringResource(R.string.wifi_gateway)}: ${conn.gateway.ifBlank { "-" }}" +
                        " · ${stringResource(R.string.wifi_ip)}: ${conn.ipAddress}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    "${stringResource(R.string.wifi_vendor)}: ${conn.vendor} · ${conn.wifiStandard} · ${conn.security}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SignalBar(rssi: Int, modifier: Modifier = Modifier) {
    val level = SignalLabels.level(rssi)
    val color = signalColor(level)
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = Color.White.copy(alpha = 0.35f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
        )
        val frac = ((rssi + 100) / 60f).coerceIn(0.05f, 1f)
        drawRoundRect(
            color = color,
            size = androidx.compose.ui.geometry.Size(w * frac, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2, h / 2)
        )
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, subtitle: String) {
    Card(modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LastSpeedCard(speed: com.opensource.netlens.data.model.SpeedResult?, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.home_last_speed), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (speed != null) {
                    Text(
                        "${speed.score} ${speed.grade}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (speed == null) {
                Text(stringResource(R.string.speed_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onClick) { Text(stringResource(R.string.speed_start)) }
            } else {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    MetricCell(stringResource(R.string.speed_download), "${Formatters.mbps(speed.downloadMbps)} Mbps")
                    MetricCell(stringResource(R.string.speed_upload), "${Formatters.mbps(speed.uploadMbps)} Mbps")
                    MetricCell(stringResource(R.string.speed_latency), "${Formatters.ms(speed.latencyMs)} ms")
                }
                OutlinedButton(onClick = onClick) { Text(stringResource(R.string.speed_start)) }
            }
        }
    }
}

@Composable
fun MetricCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun AdviceRow(a: com.opensource.netlens.data.model.AdviceItem) {
    val title = if (isZh()) a.titleZh else a.titleEn
    val detail = if (isZh()) a.detailZh else a.detailEn
    val color = com.opensource.netlens.data.model.severityColor(a.severity)
    Card {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun isZh(): Boolean {
    val lang = java.util.Locale.getDefault().language
    return lang.startsWith("zh")
}
