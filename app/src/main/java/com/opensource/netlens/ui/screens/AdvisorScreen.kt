package com.opensource.netlens.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.R
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvisorScreen(
    vm: MainViewModel,
    onNavigateToSpeed: () -> Unit = {},
    onNavigateToDevices: () -> Unit = {}
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.networks, state.lastSpeed, state.ping, state.connection) {
        vm.rebuildAdvice()
    }
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
                        Text(stringResource(R.string.advice_title), fontWeight = FontWeight.ExtraBold)
                        Text(
                            "把诊断变成可执行动作",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(Modifier.padding(horizontal = 16.dp)) {
                Button(
                    onClick = { vm.refreshWifi(); vm.rebuildAdvice() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.advice_refresh))
                }
                Spacer(Modifier.height(12.dp))
                SectionHeader(
                    title = "可执行策略",
                    subtitle = "切换 5GHz · 打开路由器 · 改信道步骤 · 测速"
                )
                Spacer(Modifier.height(4.dp))
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.advice) { a ->
                        AdviceActionCard(advice = a, onExecute = {
                            vm.executeAdvice(
                                advice = a,
                                onNavigateToSpeed = onNavigateToSpeed,
                                onNavigateToDevices = onNavigateToDevices
                            )
                        })
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    "提示",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    "Android 不允许 App 直接修改路由器信道/加密。应用会：\n" +
                                        "• 尝试用系统建议 API 切到同名 5/6GHz\n" +
                                        "• 打开路由器后台 / 复制改信道步骤\n" +
                                        "• 一键复测确认效果",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 88.dp)
        )
    }
}
