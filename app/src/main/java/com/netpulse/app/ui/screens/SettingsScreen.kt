package com.netpulse.app.ui.screens

import android.Manifest
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import com.netpulse.app.NetPulseApp
import com.netpulse.app.R
import com.netpulse.app.service.SpeedTileService
import com.netpulse.app.service.SpeedWidgetProvider
import com.netpulse.app.ui.components.PillButton
import com.netpulse.app.ui.components.Stepper
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.netpulse.app.data.IconStyle
import com.netpulse.app.data.MeterSettings
import com.netpulse.app.service.SpeedIconRenderer
import com.netpulse.app.ui.components.Chip
import com.netpulse.app.ui.components.GlassCard
import com.netpulse.app.ui.components.GlassSwitch
import com.netpulse.app.ui.components.Segmented
import com.netpulse.app.ui.theme.Palette

@Composable
fun SettingsScreen(
    settings: MeterSettings,
    wide: Boolean,
    onIconStyle: (IconStyle) -> Unit,
    onShowDetails: (Boolean) -> Unit,
    onPauseWhenScreenOff: (Boolean) -> Unit,
    onStartOnBoot: (Boolean) -> Unit,
    onBubble: (Boolean) -> Unit,
    onAlerts: (Boolean) -> Unit,
    onDailyLimit: (Int) -> Unit,
    onMonthlyLimit: (Int) -> Unit,
    onBillingDay: (Int) -> Unit,
    onRequestNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val left: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusBarCard(settings.iconStyle, onIconStyle)
            GlassCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalSpacing = 0.dp) {
                ToggleRow("Notification panel details", "Optional · ↓/↑ speed and session data in the panel", settings.showDetails, onShowDetails)
                HorizontalDivider(color = Color(0x1FFFFFFF))
                ToggleRow("Pause when screen is off", "Saves battery on the Tab A9", settings.pauseWhenScreenOff, onPauseWhenScreenOff)
                HorizontalDivider(color = Color(0x1FFFFFFF))
                ToggleRow("Start on boot", "Meter starts automatically after a restart", settings.startOnBoot, onStartOnBoot)
            }
            ExtrasCard(settings.bubbleEnabled, onBubble = { on ->
                if (on && !Settings.canDrawOverlays(context)) openOverlaySettings(context)
                onBubble(on)
            })
            AlertsCard(settings, onAlerts, onDailyLimit, onMonthlyLimit, onBillingDay)
        }
    }

    if (wide) {
        Row(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            left(Modifier.weight(1f))
            SamsungChecklist(settings.bubbleEnabled, onRequestNotifications, Modifier.width(400.dp))
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            left(Modifier.fillMaxWidth())
            SamsungChecklist(settings.bubbleEnabled, onRequestNotifications, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatusBarCard(style: IconStyle, onIconStyle: (IconStyle) -> Unit) {
    val context = LocalContext.current
    // Same renderer the service uses, drawn bigger so the user sees exactly what they get.
    val preview = remember(style) {
        SpeedIconRenderer(context, sizeDp = 64).render(style, 1_920_000, 480_000).asImageBitmap()
    }
    GlassCard(verticalSpacing = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Status bar speed", style = MaterialTheme.typography.titleLarge)
                Text("Always on — the core of the app", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
            }
            Chip("Mandatory", Palette.TextPrimary, dot = false)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x99000000))
                    .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(preview, contentDescription = "Status bar icon preview", modifier = Modifier.size(64.dp), filterQuality = FilterQuality.High)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Icon style", style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
                Segmented(
                    items = IconStyle.entries,
                    selected = style,
                    label = {
                        when (it) {
                            IconStyle.TOTAL -> "Total"
                            IconStyle.DOWNLOAD -> "Download"
                            IconStyle.BOTH -> "Down + Up"
                        }
                    },
                    onSelect = onIconStyle,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FactPill("Unit", "MB/s (auto KB/s)", Modifier.weight(1f))
            FactPill("Refresh", "Every second", Modifier.weight(1f))
        }
    }
}

@Composable
private fun FactPill(label: String, value: String, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x14FFFFFF))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        GlassSwitch(checked, onChange)
    }
}

@Composable
private fun SamsungChecklist(bubbleEnabled: Boolean, onRequestNotifications: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val notificationsOk = remember(refresh) { notificationsAllowed(context) }
    val batteryOk = remember(refresh) { batteryUnrestricted(context) }
    val usageOk = remember(refresh) { (context.applicationContext as NetPulseApp).usage.hasAccess() }
    val overlayOk = remember(refresh) { Settings.canDrawOverlays(context) }

    GlassCard(modifier, verticalSpacing = 16.dp) {
        Text("Samsung setup", style = MaterialTheme.typography.titleLarge, color = Palette.Warn)
        Text(
            "One UI can hide or stop the meter. Fix these once.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary,
        )
        CheckItem(
            done = notificationsOk,
            title = "Allow notifications",
            detail = "The speed icon lives in a notification.",
            action = "Allow",
            onAction = {
                if (Build.VERSION.SDK_INT >= 33) onRequestNotifications() else openAppNotificationSettings(context)
            },
        )
        CheckItem(
            done = batteryOk,
            title = "Battery: Unrestricted",
            detail = "Also add NetPulse to Battery › Background usage limits › Never sleeping apps.",
            action = "Open battery settings",
            onAction = { openBatterySettings(context) },
        )
        CheckItem(
            done = null,
            title = "Show all status bar icons",
            detail = "Settings › Notifications › Advanced settings › Show notification icons › All (otherwise One UI may show only a count).",
            action = "Open notification settings",
            onAction = { openAppNotificationSettings(context) },
        )
        CheckItem(
            done = usageOk,
            title = "Usage access",
            detail = "Needed for daily/monthly data usage, per-app usage and limit alerts.",
            action = "Grant usage access",
            onAction = { startSafely(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )
        if (bubbleEnabled) {
            CheckItem(
                done = overlayOk,
                title = "Display over other apps",
                detail = "Needed for the floating bubble.",
                action = "Allow",
                onAction = { openOverlaySettings(context) },
            )
        }
    }
}

@Composable
private fun ExtrasCard(bubbleEnabled: Boolean, onBubble: (Boolean) -> Unit) {
    val context = LocalContext.current
    GlassCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalSpacing = 0.dp) {
        ToggleRow("Floating bubble", "Optional · ↓/↑ speed on top of videos and games (drag to move)", bubbleEnabled, onBubble)
        HorizontalDivider(color = Color(0x1FFFFFFF))
        ActionRow(
            title = "Quick Settings tile",
            subtitle = "Live speed in the notification shade; tap it to toggle the bubble",
            action = "Add tile",
            onAction = { requestTile(context) },
        )
        HorizontalDivider(color = Color(0x1FFFFFFF))
        ActionRow(
            title = "Home screen widget",
            subtitle = "4×2 glass widget with download, upload and today's usage",
            action = "Add widget",
            onAction = { requestWidget(context) },
        )
    }
}

@Composable
private fun ActionRow(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        PillButton(action, onAction, primary = false)
    }
}

@Composable
private fun AlertsCard(
    settings: MeterSettings,
    onAlerts: (Boolean) -> Unit,
    onDailyLimit: (Int) -> Unit,
    onMonthlyLimit: (Int) -> Unit,
    onBillingDay: (Int) -> Unit,
) {
    GlassCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalSpacing = 0.dp) {
        ToggleRow("Data limit alerts", "Notify when a limit is reached (needs Usage access)", settings.alertsEnabled, onAlerts)
        HorizontalDivider(color = Color(0x1FFFFFFF))
        Stepper(
            label = "Daily limit · all networks",
            value = limitLabel(settings.dailyLimitMb),
            onMinus = { onDailyLimit((settings.dailyLimitMb - 500).coerceAtLeast(0)) },
            onPlus = { onDailyLimit(settings.dailyLimitMb + 500) },
        )
        Stepper(
            label = "Monthly limit · mobile",
            value = limitLabel(settings.monthlyMobileLimitMb),
            onMinus = { onMonthlyLimit((settings.monthlyMobileLimitMb - 1_000).coerceAtLeast(0)) },
            onPlus = { onMonthlyLimit(settings.monthlyMobileLimitMb + 1_000) },
        )
        Stepper(
            label = "Billing cycle starts on day",
            value = settings.billingDay.toString(),
            onMinus = { onBillingDay(if (settings.billingDay <= 1) 28 else settings.billingDay - 1) },
            onPlus = { onBillingDay(if (settings.billingDay >= 28) 1 else settings.billingDay + 1) },
        )
    }
}

private fun limitLabel(mb: Int): String =
    if (mb <= 0) "Off" else String.format(java.util.Locale.US, if (mb % 1000 == 0) "%.0f GB" else "%.1f GB", mb / 1000.0)

private fun openOverlaySettings(context: Context) {
    startSafely(
        context,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
    )
}

private fun requestTile(context: Context) {
    if (Build.VERSION.SDK_INT >= 33) {
        val sbm = context.getSystemService(StatusBarManager::class.java)
        runCatching {
            sbm.requestAddTileService(
                ComponentName(context, SpeedTileService::class.java),
                context.getString(R.string.tile_label),
                android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_pulse),
                context.mainExecutor,
            ) { }
        }
    } else {
        Toast.makeText(context, "Pull down Quick Settings › Edit (pencil) › drag NetPulse in", Toast.LENGTH_LONG).show()
    }
}

private fun requestWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ok = manager.isRequestPinAppWidgetSupported &&
        runCatching { manager.requestPinAppWidget(ComponentName(context, SpeedWidgetProvider::class.java), null, null) }
            .getOrDefault(false)
    if (!ok) {
        Toast.makeText(context, "Long-press the home screen › Widgets › NetPulse", Toast.LENGTH_LONG).show()
    }
}

/** [done] null = cannot be detected, show as a manual step. */
@Composable
private fun CheckItem(done: Boolean?, title: String, detail: String, action: String, onAction: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(22.dp)
                .then(
                    if (done == true) Modifier.background(Palette.Good, CircleShape)
                    else Modifier.border(2.dp, Palette.Warn, CircleShape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done == true) Text("✓", color = Palette.OnSelected, style = MaterialTheme.typography.labelSmall)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
            if (done != true) {
                Text(
                    action,
                    style = MaterialTheme.typography.labelLarge,
                    color = Palette.Download,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onAction)
                        .heightIn(min = 44.dp)
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

private fun batteryUnrestricted(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    startSafely(context, intent)
}

private fun openBatterySettings(context: Context) {
    // App info is where One UI shows Battery › Unrestricted for this app.
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
    startSafely(context, intent)
}

private fun startSafely(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}
