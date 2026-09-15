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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.data.model.LanDevice
import com.opensource.netlens.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(stringResource(R.string.devices_title), fontWeight = FontWeight.ExtraBold)
                    Text(
                        stringResource(R.string.devices_found, state.devices.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            actions = {
                IconButton(onClick = { vm.scanDevices() }, enabled = !state.isScanningDevices) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_scan))
                }
            }
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.devices_found, state.devices.size),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.scanDevices() },
                enabled = !state.isScanningDevices,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isScanningDevices) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.height(0.dp))
                    Text("  " + stringResource(R.string.devices_scanning))
                } else {
                    Text(stringResource(R.string.action_scan))
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (state.devices.isEmpty() && !state.isScanningDevices) {
            Text(
                stringResource(R.string.devices_empty),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.devices, key = { it.ip }) { d ->
                DeviceCard(d)
            }
        }
    }
}

@Composable
fun DeviceCard(d: LanDevice) {
    Card {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        d.hostname.ifBlank { d.ip },
                        fontWeight = FontWeight.SemiBold
                    )
                    if (d.isThisDevice) {
                        Text(stringResource(R.string.devices_this_device), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (d.isGateway) {
                        Text(stringResource(R.string.devices_gateway_tag), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
                Text(d.ip, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${d.mac.ifBlank { stringResource(R.string.unknown) }} · ${d.vendor}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
