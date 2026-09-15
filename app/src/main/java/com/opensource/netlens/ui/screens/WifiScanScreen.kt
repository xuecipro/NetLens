package com.opensource.netlens.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.opensource.netlens.data.model.WifiNetwork
import com.opensource.netlens.data.model.signalColor
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.ui.components.AnimatedSignalBar
import com.opensource.netlens.ui.components.StatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiScanScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    var bandFilter by remember { mutableStateOf<Band?>(null) }

    val networks = state.networks.filter { bandFilter == null || it.band == bandFilter }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.nav_wifi), fontWeight = FontWeight.ExtraBold)
                    Text(
                        stringResource(R.string.wifi_networks_found, networks.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { vm.refreshWifi() }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
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

        if (state.isScanningWifi) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(networks, key = { it.bssid + it.ssid }) { net ->
                PrettyWifiCard(net, isCurrent = net.isCurrent)
            }
        }
    }
}

@Composable
fun PrettyWifiCard(net: WifiNetwork, isCurrent: Boolean) {
    val color = signalColor(net.signalLevel)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrent) 3.dp else 1.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (net.signalLevel >= 2) Icons.Rounded.Wifi else Icons.Rounded.WifiOff,
                    contentDescription = null,
                    tint = color
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    net.ssid.ifBlank { stringResource(R.string.unknown) },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isCurrent) {
                    StatusChip("当前", MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(6.dp))
                Text("${net.rssi}", fontWeight = FontWeight.Bold, color = color)
            }
            AnimatedSignalBar(rssi = net.rssi)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(net.band.label, MaterialTheme.colorScheme.secondary)
                Text("CH ${net.channel}${net.channelWidthMhz?.let { " · ${it}MHz" } ?: ""}",
                    style = MaterialTheme.typography.labelMedium)
                StatusChip(net.security.name, color)
            }
            Text(
                "${net.bssid} · ${net.vendor} · ${net.wifiStandard}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
