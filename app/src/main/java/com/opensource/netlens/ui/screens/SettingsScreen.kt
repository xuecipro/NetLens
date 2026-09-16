package com.opensource.netlens.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.BuildConfig
import com.opensource.netlens.R
import com.opensource.netlens.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit = {}) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var theme by remember(state.themeMode) { mutableStateOf(state.themeMode) }
    var interval by remember(state.scanIntervalMs) { mutableStateOf(state.scanIntervalMs) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(Modifier.padding(16.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = theme == "system", onClick = {
                    theme = "system"; vm.setThemeMode("system")
                }, label = { Text(stringResource(R.string.settings_theme_system)) })
                FilterChip(selected = theme == "light", onClick = {
                    theme = "light"; vm.setThemeMode("light")
                }, label = { Text(stringResource(R.string.settings_theme_light)) })
                FilterChip(selected = theme == "dark", onClick = {
                    theme = "dark"; vm.setThemeMode("dark")
                }, label = { Text(stringResource(R.string.settings_theme_dark)) })
            }

            Text(stringResource(R.string.settings_refresh), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                listOf(1000L to R.string.settings_refresh_1s, 3000L to R.string.settings_refresh_3s, 5000L to R.string.settings_refresh_5s, 10000L to R.string.settings_refresh_10s)
                    .forEach { (ms, label) ->
                        FilterChip(selected = interval == ms, onClick = {
                            interval = ms
                            vm.setScanInterval(ms)
                        }, label = { Text(stringResource(label)) })
                    }
            }

            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            LanguageRow(vm)

            Spacer(Modifier.height(8.dp))
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.settings_about), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.settings_open_source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GitHub",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/")))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageRow(vm: MainViewModel) {
    var lang by remember { mutableStateOf(vm.getLanguage()) }
    Column {
        listOf("system" to stringResource(R.string.settings_language_system),
            "zh" to stringResource(R.string.settings_language_zh),
            "en" to stringResource(R.string.settings_language_en)).forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        lang = value
                        vm.setLanguage(value)
                    }
            ) {
                RadioButton(selected = lang == value, onClick = {
                    lang = value
                    vm.setLanguage(value)
                })
                Text(label)
            }
        }
        Text(
            stringResource(R.string.settings_language_system),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
