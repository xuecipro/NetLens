package com.opensource.netlens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opensource.netlens.ui.MainViewModel
import com.opensource.netlens.ui.screens.AdvisorScreen
import com.opensource.netlens.ui.screens.ChannelScreen
import com.opensource.netlens.ui.screens.DevicesScreen
import com.opensource.netlens.ui.screens.HomeScreen
import com.opensource.netlens.ui.screens.SettingsScreen
import com.opensource.netlens.ui.screens.SpeedTestScreen
import com.opensource.netlens.ui.screens.WifiScanScreen
import com.opensource.netlens.ui.theme.NetLensTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        vm.onPermissionResult(hasAllPermissions())
        if (hasAllPermissions()) vm.refreshWifi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            NetLensTheme(themeMode = state.themeMode) {
                NetLensAppRoot(vm = vm, onNeedPermissions = { requestPerms() })
            }
        }
        if (!hasAllPermissions()) requestPerms()
        else vm.onPermissionResult(true)
    }

    private fun neededPermissions(): List<String> = listOfNotNull(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.NEARBY_WIFI_DEVICES else null
    )

    private fun hasAllPermissions(): Boolean =
        neededPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestPerms() {
        val missing = neededPermissions().filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
        else vm.onPermissionResult(true)
    }
}

private data class TabSpec(val icon: ImageVector, val labelRes: Int)

private enum class MainTab {
    HOME, WIFI, CHANNEL, DEVICES, SPEED, ADVICE;

    companion object {
        val specs = mapOf(
            HOME to TabSpec(Icons.Rounded.Dashboard, R.string.nav_home),
            WIFI to TabSpec(Icons.Rounded.Wifi, R.string.nav_wifi),
            CHANNEL to TabSpec(Icons.Rounded.Router, R.string.nav_channel),
            DEVICES to TabSpec(Icons.Rounded.NetworkCheck, R.string.nav_devices),
            SPEED to TabSpec(Icons.Rounded.Speed, R.string.nav_speed),
            ADVICE to TabSpec(Icons.Rounded.Lightbulb, R.string.nav_advice)
        )
    }
}

@Composable
fun NetLensAppRoot(vm: MainViewModel, onNeedPermissions: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(MainTab.HOME.ordinal) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    fun go(t: MainTab) {
        showSettings = false
        tab = t.ordinal
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { t ->
                    val spec = MainTab.specs[t]!!
                    NavigationBarItem(
                        selected = !showSettings && tab == t.ordinal,
                        onClick = { go(t) },
                        icon = { Icon(spec.icon, contentDescription = null) },
                        label = { Text(stringResource(spec.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        val modifier = Modifier.padding(padding)
        if (showSettings) {
            SettingsScreen(vm = vm, onBack = { showSettings = false })
        } else {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    (slideInHorizontally { it / 8 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 8 } + fadeOut())
                },
                label = "tab",
                modifier = modifier
            ) { selected ->
                when (MainTab.entries[selected]) {
                    MainTab.HOME -> HomeScreen(
                        vm = vm,
                        onOpenSettings = { showSettings = true },
                        onOpenSpeed = { go(MainTab.SPEED) },
                        onOpenDevices = { go(MainTab.DEVICES) },
                        onOpenAdvice = { go(MainTab.ADVICE) },
                        onNeedPermissions = onNeedPermissions
                    )
                    MainTab.WIFI -> WifiScanScreen(
                        vm = vm,
                        onOpenSettings = { showSettings = true }
                    )
                    MainTab.CHANNEL -> ChannelScreen(vm = vm)
                    MainTab.DEVICES -> DevicesScreen(vm = vm)
                    MainTab.SPEED -> SpeedTestScreen(vm = vm)
                    MainTab.ADVICE -> AdvisorScreen(
                        vm = vm,
                        onNavigateToSpeed = { go(MainTab.SPEED) },
                        onNavigateToDevices = { go(MainTab.DEVICES) }
                    )
                }
            }
        }
    }
}
