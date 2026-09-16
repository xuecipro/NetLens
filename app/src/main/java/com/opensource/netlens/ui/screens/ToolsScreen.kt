package com.opensource.netlens.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var pingHost by remember { mutableStateOf("1.1.1.1") }
    var dnsHost by remember { mutableStateOf("www.baidu.com") }
    var httpUrl by remember { mutableStateOf("https://www.baidu.com") }

    LaunchedEffect(state.actionMessage) {
        val msg = state.actionMessage
        if (msg != null) {
            snackbar.showSnackbar(msg)
            vm.clearActionMessage()
        }
    }

    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.tools_title), fontWeight = FontWeight.ExtraBold)
                    Text(
                        "局域网 · Ping · DNS · 公网 IP · HTTP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { vm.scanDevices() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ---- LAN ----
            item {
                SectionHeader(
                    title = stringResource(R.string.devices_title),
                    subtitle = stringResource(R.string.devices_found, state.devices.size)
                )
                Button(
                    onClick = { vm.scanDevices() },
                    enabled = !state.isScanningDevices,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.isScanningDevices) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.devices_scanning))
                    } else {
                        Icon(Icons.Rounded.Lan, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_scan))
                    }
                }
            }

            items(state.devices.size) { idx ->
                val d = state.devices[idx]
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.hostname.ifBlank { d.ip },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(d.ip, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${d.mac.ifBlank { "-" }} · ${d.vendor}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (d.isGateway) {
                            Icon(Icons.Rounded.Router, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }

            // ---- Ping ----
            item {
                ToolCard(
                    icon = Icons.Rounded.Speed,
                    title = "Ping 测试",
                    subtitle = state.pingToolText ?: "测主机可达性与延迟"
                ) {
                    OutlinedTextField(
                        value = pingHost,
                        onValueChange = { pingHost = it },
                        singleLine = true,
                        label = { Text("主机 / Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.runPingTool(pingHost) },
                        enabled = !state.isPingToolRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Ping") }
                }
            }

            // ---- DNS ----
            item {
                ToolCard(
                    icon = Icons.Rounded.Dns,
                    title = "DNS 解析",
                    subtitle = state.dnsToolText ?: "查询域名对应 IP"
                ) {
                    OutlinedTextField(
                        value = dnsHost,
                        onValueChange = { dnsHost = it },
                        singleLine = true,
                        label = { Text("域名 / Hostname") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.runDnsTool(dnsHost) },
                        enabled = !state.isDnsToolRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("解析") }
                }
            }

            // ---- HTTP ----
            item {
                ToolCard(
                    icon = Icons.Rounded.Language,
                    title = "网页可达性",
                    subtitle = state.httpToolText ?: "检测 URL 状态码与耗时"
                ) {
                    OutlinedTextField(
                        value = httpUrl,
                        onValueChange = { httpUrl = it },
                        singleLine = true,
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.runHttpCheck(httpUrl) },
                        enabled = !state.isHttpToolRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("检测") }
                }
            }

            // ---- Public IP ----
            item {
                ToolCard(
                    icon = Icons.Rounded.Public,
                    title = "公网 IP",
                    subtitle = state.publicIpText ?: "查看出口公网地址"
                ) {
                    OutlinedButton(
                        onClick = { vm.runPublicIp() },
                        enabled = !state.isPublicIpRunning,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("查询公网 IP") }
                }
            }

            // ---- Network info ----
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("当前网络摘要", fontWeight = FontWeight.SemiBold)
                        val c = state.connection
                        MonoLine("SSID", c?.ssid ?: "-")
                        MonoLine("BSSID", c?.bssid ?: "-")
                        MonoLine("IP", c?.ipAddress ?: "-")
                        MonoLine("Gateway", c?.gateway ?: "-")
                        MonoLine("DNS", c?.dns ?: "-")
                        MonoLine("Band", c?.band?.label ?: "-")
                        MonoLine("CH", c?.channel?.toString() ?: "-")
                        MonoLine("RSSI", c?.rssi?.let { "$it dBm" } ?: "-")
                        MonoLine("Link", c?.linkSpeedMbps?.let { "$it Mbps" } ?: "-")
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun MonoLine(k: String, v: String) {
    Row {
        Text("$k: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(v, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ToolCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}
