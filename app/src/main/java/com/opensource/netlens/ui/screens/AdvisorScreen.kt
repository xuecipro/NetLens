package com.opensource.netlens.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisorScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.networks, state.lastSpeed, state.ping, state.connection) {
        vm.rebuildAdvice()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.advice_title), fontWeight = FontWeight.Bold) })

        Column(Modifier.padding(horizontal = 16.dp)) {
            Button(onClick = { vm.rebuildAdvice(); vm.refreshWifi() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.advice_refresh))
            }
            Spacer(Modifier.height(8.dp))
        }

        if (state.advice.isEmpty()) {
            Text(
                stringResource(R.string.advice_empty),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.advice) { a ->
                    AdviceRow(a)
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        stringResource(R.string.settings_open_source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
