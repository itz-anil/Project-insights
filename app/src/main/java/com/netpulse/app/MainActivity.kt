package com.netpulse.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.netpulse.app.service.SpeedMeterService
import com.netpulse.app.ui.MainViewModel
import com.netpulse.app.ui.components.Chip
import com.netpulse.app.ui.components.GlassBackground
import com.netpulse.app.ui.components.PillTabs
import com.netpulse.app.ui.screens.SpeedTestScreen
import com.netpulse.app.ui.screens.UsageScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.netpulse.app.ui.screens.DashboardScreen
import com.netpulse.app.ui.screens.SettingsActions
import com.netpulse.app.ui.screens.SettingsScreen
import com.netpulse.app.ui.theme.isDarkTheme
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.netpulse.app.ui.theme.NetPulseTheme
import com.netpulse.app.ui.theme.Palette

enum class Tab(val title: String, val heading: String) {
    DASHBOARD("Dashboard", "Live"),
    USAGE("Usage", "Data usage"),
    SPEED_TEST("Speed test", "Speed test"),
    SETTINGS("Settings", "Settings"),
}

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        SpeedMeterService.start(this)
        val startTab = if (intent?.getBooleanExtra(EXTRA_OPEN_SETTINGS, false) == true) Tab.SETTINGS else Tab.DASHBOARD
        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            val dark = isDarkTheme(settings.themeMode)
            // Status / navigation bar icons: light on dark, dark on light.
            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                enableEdgeToEdge(
                    statusBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                    navigationBarStyle = if (dark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
                )
                onDispose { }
            }
            NetPulseTheme(dark) { NetPulseRoot(vm, startTab) }
        }
    }

    companion object {
        const val EXTRA_OPEN_SETTINGS = "open_settings"
    }
}

@Composable
private fun NetPulseRoot(vm: MainViewModel, startTab: Tab) {
    val current by vm.current.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val usage by vm.usage.collectAsStateWithLifecycle()
    val test by vm.test.collectAsStateWithLifecycle()
    val testHistory by vm.testHistory.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(startTab) }

    // Re-read system usage when returning to the app (e.g. after granting Usage access).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.refreshToday()
        if (tab == Tab.USAGE) vm.refreshUsage()
    }
    LaunchedEffect(tab) { if (tab == Tab.USAGE) vm.refreshUsage() }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The checklist re-reads the state on resume. */ }
    val requestNotifications = {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(Unit) { requestNotifications() }

    GlassBackground(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
            val wide = maxWidth >= 840.dp
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                val tabs = @Composable {
                    PillTabs(Tab.entries, tab, { it.title }, { tab = it })
                }
                val status = @Composable {
                    Chip(
                        if (running) "Meter running" else "Meter stopped",
                        if (running) Palette.Good else Palette.Warn,
                    )
                }
                if (wide) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.heading, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                        tabs()
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) { status() }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.heading, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
                        status()
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { tabs() }
                }

                when (tab) {
                    Tab.DASHBOARD -> DashboardScreen(current, history, session, connection, today, testHistory.firstOrNull(), settings.refreshSeconds, wide)
                    Tab.USAGE -> UsageScreen(usage, settings, wide)
                    Tab.SPEED_TEST -> SpeedTestScreen(
                        test = test,
                        history = testHistory,
                        wide = wide,
                        onStart = vm::startTest,
                        onStop = vm::stopTest,
                        onClearHistory = { vm.clearTestHistory() },
                    )
                    Tab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        wide = wide,
                        actions = remember(vm) {
                            SettingsActions(
                                onIconStyle = { vm.setIconStyle(it) },
                                onUnit = { vm.setUnit(it) },
                                onRefresh = { vm.setRefreshSeconds(it) },
                                onTheme = { vm.setThemeMode(it) },
                                onShowDetails = { vm.setShowDetails(it) },
                                onPauseWhenScreenOff = { vm.setPauseWhenScreenOff(it) },
                                onStartOnBoot = { vm.setStartOnBoot(it) },
                                onBubble = { vm.setBubble(it) },
                                onAlerts = { vm.setAlerts(it) },
                                onDailyLimit = { vm.setDailyLimitMb(it) },
                                onMonthlyLimit = { vm.setMonthlyLimitMb(it) },
                                onBillingDay = { vm.setBillingDay(it) },
                                onStatusIconsConfirmed = { vm.setStatusIconsConfirmed(it) },
                            )
                        },
                    )
                }
            }
        }
    }
}
