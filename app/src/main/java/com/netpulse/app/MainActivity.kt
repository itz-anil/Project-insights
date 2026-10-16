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
import com.netpulse.app.ui.screens.ComingSoonScreen
import com.netpulse.app.ui.screens.DashboardScreen
import com.netpulse.app.ui.screens.SettingsScreen
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
        setContent { NetPulseTheme { NetPulseRoot(vm) } }
    }
}

@Composable
private fun NetPulseRoot(vm: MainViewModel) {
    val current by vm.current.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val running by vm.running.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val connection by vm.connection.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.DASHBOARD) }

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
                    Tab.DASHBOARD -> DashboardScreen(current, history, session, connection, wide)
                    Tab.USAGE -> ComingSoonScreen(
                        "Data usage",
                        listOf(
                            "Daily, weekly and monthly Wi-Fi vs mobile data",
                            "Per-app usage (needs Usage access)",
                            "Mobile data limit with alerts",
                        ),
                    )
                    Tab.SPEED_TEST -> ComingSoonScreen(
                        "Speed test",
                        listOf(
                            "Download, upload, ping and jitter",
                            "History of past tests",
                            "Floating bubble, Quick Settings tile and home widget",
                        ),
                    )
                    Tab.SETTINGS -> SettingsScreen(
                        settings = settings,
                        wide = wide,
                        onIconStyle = { vm.setIconStyle(it) },
                        onShowDetails = { vm.setShowDetails(it) },
                        onPauseWhenScreenOff = { vm.setPauseWhenScreenOff(it) },
                        onStartOnBoot = { vm.setStartOnBoot(it) },
                        onRequestNotifications = requestNotifications,
                    )
                }
            }
        }
    }
}
