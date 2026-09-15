package com.opensource.netlens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.data.model.Band
import com.opensource.netlens.data.model.SignalLabels
import com.opensource.netlens.data.model.WifiNetwork
import com.opensource.netlens.data.model.signalColor
import com.opensource.netlens.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScanScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var bandFilter by remember { mutableStateOf<Band?>(null) }
    var onlyCurrent by remember { mutableStateOf(false) }

    val networks = state.networks.filter { n ->
        (bandFilter == null || n.band == bandFilter) && (!onlyCurrent || n.isCurrent)
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.nav_wifi), fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { vm.refreshWifi() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
            }
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.wifi_networks_found, networks.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = bandFilter == null, onClick = { bandFilter = null }, label = {
                    Text(stringResource(R.string.wifi_filter_all))
                })
                FilterChip(selected = bandFilter == Band.BAND_24, onClick = { bandFilter = Band.BAND_24 }, label = {
                    Text(stringResource(R.string.wifi_band_2g))
                })
                FilterChip(selected = bandFilter == Band.BAND_5, onClick = { bandFilter = Band.BAND_5 }, label = {
                    Text(stringResource(R.string.wifi_band_5g))
                })
                FilterChip(selected = bandFilter == Band.BAND_6, onClick = { bandFilter = Band.BAND_6 }, label = {
                    Text(stringResource(R.string.wifi_band_6g))
                })
            }
            Spacer(Modifier.height(8.dp))
            FilterChip(selected = onlyCurrent, onClick = { onlyCurrent = !onlyCurrent }, label = {
                Text(stringResource(R.string.nav_wifi) + " · " + stringResource(R.string.home_current_wifi))
            })
            Spacer(Modifier.height(8.dp))
        }

        if (state.isScanningWifi) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(networks, key = { it.bssid + it.ssid }) { net ->
                WifiNetworkCard(net)
            }
        }
    }
}

@Composable
fun WifiNetworkCard(net: WifiNetwork) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(signalColor(net.signalLevel), CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    net.ssid.ifBlank { stringResource(R.string.unknown) },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (net.isCurrent) {
                    Text(
                        "●",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "${net.rssi} dBm",
                    style = MaterialTheme.typography.labelLarge,
                    color = signalColor(net.signalLevel)
                )
            }
            LinearProgressIndicator(
                progress = { ((net.rssi + 100) / 60f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = signalColor(net.signalLevel)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${net.band.label} · CH ${net.channel}${net.channelWidthMhz?.let { " · ${it}MHz" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(net.security.name, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${net.bssid} · ${net.vendor} · ${net.wifiStandard}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(net.signalLevel.let {
                    when (it) {
                        4 -> R.string.wifi_signal_excellent
                        3 -> R.string.wifi_signal_good
                        2 -> R.string.wifi_signal_fair
                        1 -> R.string.wifi_signal_weak
                        else -> R.string.wifi_signal_dead
                    }
                }),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
