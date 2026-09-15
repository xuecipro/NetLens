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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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

private data class Tab(val icon: ImageVector, val labelRes: Int, val route: String)

@Composable
fun NetLensAppRoot(vm: MainViewModel, onNeedPermissions: () -> Unit) {
    val navController = rememberNavController()
    val tabs = listOf(
        Tab(Icons.Default.Home, R.string.nav_home, "home"),
        Tab(Icons.Default.Wifi, R.string.nav_wifi, "wifi"),
        Tab(Icons.Default.Router, R.string.nav_channel, "channel"),
        Tab(Icons.Default.NetworkCheck, R.string.nav_devices, "devices"),
        Tab(Icons.Default.Speed, R.string.nav_speed, "speed"),
        Tab(Icons.Default.Lightbulb, R.string.nav_advice, "advice")
    )
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenSpeed = { navController.navigate("speed") },
                    onNeedPermissions = onNeedPermissions
                )
            }
            composable("wifi") { WifiScanScreen(vm = vm) }
            composable("channel") { ChannelScreen(vm = vm) }
            composable("devices") { DevicesScreen(vm = vm) }
            composable("speed") { SpeedTestScreen(vm = vm) }
            composable("advice") { AdvisorScreen(vm = vm) }
            composable("settings") { SettingsScreen(vm = vm) }
        }
    }
}
