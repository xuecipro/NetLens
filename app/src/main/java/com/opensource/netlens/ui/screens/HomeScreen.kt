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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.data.model.AdviceItem
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.severityColor
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.ui.UiState
import com.opensource.netlens.ui.components.AnimatedSignalBar
import com.opensource.netlens.ui.components.GradientHero
import com.opensource.netlens.ui.components.GlowCard
import com.opensource.netlens.ui.components.MetricBadge
import com.opensource.netlens.ui.components.SectionHeader
import com.opensource.netlens.ui.components.StatusChip
import com.opensource.netlens.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenSettings: () -> Unit,
    onOpenSpeed: () -> Unit,
    onOpenDevices: () -> Unit = {},
    onOpenAdvice: () -> Unit = {},
    onNeedPermissions: () -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.actionMessage) {
        val msg = state.actionMessage
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.clearActionMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold)
                        Text(
                            stringResource(R.string.app_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshWifi() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                    }
                    IconButton(onClick = {
                        val text = vm.shareReport()
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(android.content.Intent.createChooser(send, "NetLens"))
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.permissionNeeded) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.permission_title), fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.permission_rationale))
                                Button(onClick = onNeedPermissions) {
                                    Text(stringResource(R.string.permission_grant))
                                }
                            }
                        }
                    }
                }

                item { HeroConnectionCard(state) }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Router,
                            title = stringResource(R.string.home_best_channel),
                            value = bestChannelText(vm),
                            onClick = { /* channel tab via bottom nav */ }
                        )
                        QuickTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Wifi,
                            title = stringResource(R.string.home_connected_devices),
                            value = state.devices.size.toString(),
                            onClick = onOpenDevices
                        )
                    }
                }

                item { LastSpeedHero(state, onOpenSpeed) }

                item {
                    SectionHeader(
                        title = stringResource(R.string.advice_title),
                        subtitle = "一键执行 / One-tap actions"
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
                        AdviceActionCard(
                            advice = state.advice[idx],
                            onExecute = {
                                vm.executeAdvice(
                                    advice = state.advice[idx],
                                    onNavigateToSpeed = onOpenSpeed,
                                    onNavigateToDevices = onOpenDevices
                                )
                            }
                        )
                    }
                    item {
                        OutlinedButton(
                            onClick = onOpenAdvice,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("查看全部建议 / All advice")
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)
        )
    }
}

@Composable
private fun HeroConnectionCard(state: UiState) {
    val conn = state.connection
    GradientHero(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.home_current_wifi),
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(
            conn?.ssid ?: stringResource(R.string.home_not_connected),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        if (conn != null) {
            AnimatedSignalBar(rssi = conn.rssi)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(conn.band.label, Color.White)
                StatusChip("CH ${conn.channel}", Color.White)
                StatusChip("${conn.linkSpeedMbps} Mbps", Color.White)
                StatusChip(conn.security.name, Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${conn.rssi} dBm · ${conn.wifiStandard} · ${conn.vendor}",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "GW ${conn.gateway.ifBlank { "-" }} · IP ${conn.ipAddress}",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Text(
                "连接 WiFi 后即可分析与优化",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun QuickTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    GlowCard(modifier = modifier.clickable(onClick = onClick)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LastSpeedHero(state: UiState, onOpenSpeed: () -> Unit) {
    val speed = state.lastSpeed
    GlowCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.home_last_speed), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (speed != null) {
                StatusChip("${speed.score} ${speed.grade}", MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (speed == null) {
            Text(stringResource(R.string.speed_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onOpenSpeed, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Rounded.Bolt, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.speed_start))
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                MetricBadge(stringResource(R.string.speed_download), Formatters.mbps(speed.downloadMbps))
                MetricBadge(stringResource(R.string.speed_upload), Formatters.mbps(speed.uploadMbps), Color(0xFF0D9488))
                MetricBadge(stringResource(R.string.speed_latency), Formatters.ms(speed.latencyMs), Color(0xFF7C3AED))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onOpenSpeed, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(stringResource(R.string.speed_start))
            }
        }
    }
}

@Composable
fun AdviceActionCard(advice: AdviceItem, onExecute: () -> Unit) {
    val zh = isZh()
    val title = if (zh) advice.titleZh else advice.titleEn
    val detail = if (zh) advice.detailZh else advice.detailEn
    val actionLabel = if (zh) advice.actionLabelZh else advice.actionLabelEn
    val accent = severityColor(advice.severity)
    val hasAction = advice.action != com.opensource.netlens.data.model.AdviceAction.NONE && actionLabel.isNotBlank()

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(accent, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                StatusChip(advice.severity.name, accent)
            }
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (hasAction) {
                Button(
                    onClick = onExecute,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun bestChannelText(vm: MainViewModel): String {
    val best = vm.channelRatings(Band.BAND_24).firstOrNull()?.channel
    val best5 = vm.channelRatings(Band.BAND_5).firstOrNull()?.channel
    return "2.4G ${best ?: "-"} · 5G ${best5 ?: "-"}"
}

@Composable
fun isZh(): Boolean {
    return java.util.Locale.getDefault().language.startsWith("zh")
}
