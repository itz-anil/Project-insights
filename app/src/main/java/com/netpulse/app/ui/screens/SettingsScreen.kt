@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.netpulse.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.netpulse.app.NetPulseApp
import com.netpulse.app.R
import com.netpulse.app.data.IconStyle
import com.netpulse.app.data.MeterSettings
import com.netpulse.app.data.SpeedUnit
import com.netpulse.app.data.ThemeMode
import com.netpulse.app.service.SpeedIconRenderer
import com.netpulse.app.service.SpeedTileService
import com.netpulse.app.service.SpeedWidgetProvider
import com.netpulse.app.ui.components.Chip
import com.netpulse.app.ui.components.GlassCard
import com.netpulse.app.ui.components.GlassSwitch
import com.netpulse.app.ui.components.PillButton
import com.netpulse.app.ui.components.Segmented
import com.netpulse.app.ui.components.Stepper
import com.netpulse.app.ui.theme.Palette
import java.util.Locale

/** Everything the Settings screen can change. */
class SettingsActions(
    val onIconStyle: (IconStyle) -> Unit,
    val onUnit: (SpeedUnit) -> Unit,
    val onRefresh: (Int) -> Unit,
    val onTheme: (ThemeMode) -> Unit,
    val onShowDetails: (Boolean) -> Unit,
    val onPauseWhenScreenOff: (Boolean) -> Unit,
    val onStartOnBoot: (Boolean) -> Unit,
    val onBubble: (Boolean) -> Unit,
    val onAlerts: (Boolean) -> Unit,
    val onDailyLimit: (Int) -> Unit,
    val onMonthlyLimit: (Int) -> Unit,
    val onBillingDay: (Int) -> Unit,
    val onStatusIconsConfirmed: (Boolean) -> Unit,
)

@Composable
fun SettingsScreen(settings: MeterSettings, wide: Boolean, actions: SettingsActions) {
    val left: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusBarCard(settings, actions)
            AppearanceCard(settings.themeMode, actions.onTheme)
            BehaviourCard(settings, actions)
            ExtrasCard(settings.bubbleEnabled, actions.onBubble)
            AlertsCard(settings, actions)
        }
    }
    val right: @Composable (Modifier) -> Unit = { m ->
        Column(m, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SamsungChecklist(settings, actions.onStatusIconsConfirmed)
            AboutCard()
        }
    }

    if (wide) {
        Row(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            left(Modifier.weight(1f))
            right(Modifier.width(400.dp))
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            left(Modifier.fillMaxWidth())
            right(Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------- Status bar

@Composable
private fun StatusBarCard(settings: MeterSettings, actions: SettingsActions) {
    val context = LocalContext.current
    // Same renderer the service uses, drawn bigger so the user sees exactly what they get.
    val preview = remember(settings.iconStyle, settings.unit) {
        SpeedIconRenderer(context, sizeDp = 64)
            .render(settings.iconStyle, 1_920_000, 480_000, settings.unit)
            .asImageBitmap()
    }
    GlassCard(verticalSpacing = 18.dp) {
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
                    .background(Palette.IconWell)
                    .border(1.dp, Palette.Hairline, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    preview,
                    contentDescription = "Status bar icon preview",
                    modifier = Modifier.size(64.dp),
                    filterQuality = FilterQuality.High,
                    colorFilter = ColorFilter.tint(Palette.IconInk),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LabeledSegment("Icon style") {
                    Segmented(
                        items = IconStyle.entries,
                        selected = settings.iconStyle,
                        label = {
                            when (it) {
                                IconStyle.TOTAL -> "Total"
                                IconStyle.DOWNLOAD -> "Download"
                                IconStyle.BOTH -> "Down + Up"
                            }
                        },
                        onSelect = actions.onIconStyle,
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            LabeledSegment("Unit") {
                Segmented(
                    items = SpeedUnit.entries,
                    selected = settings.unit,
                    label = { if (it == SpeedUnit.MBYTES) "MB/s" else "Mbps" },
                    onSelect = actions.onUnit,
                )
            }
            LabeledSegment("Refresh") {
                Segmented(
                    items = listOf(1, 2, 3),
                    selected = settings.refreshSeconds,
                    label = { "${it}s" },
                    onSelect = actions.onRefresh,
                )
            }
        }
        Text(
            if (settings.unit == SpeedUnit.MBYTES) {
                "MB/s = megabytes per second (like download sizes). Below 1 MB/s it shows KB/s."
            } else {
                "Mbps = megabits per second (like internet plans). 1 MB/s = 8 Mbps."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextTertiary,
        )
    }
}

@Composable
private fun LabeledSegment(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
        content()
    }
}

// ---------------------------------------------------------------- Appearance

@Composable
private fun AppearanceCard(mode: ThemeMode, onTheme: (ThemeMode) -> Unit) {
    GlassCard(verticalSpacing = 14.dp) {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        Segmented(
            items = ThemeMode.entries,
            selected = mode,
            label = {
                when (it) {
                    ThemeMode.SYSTEM -> "Auto"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                }
            },
            onSelect = onTheme,
        )
        Text(
            "Auto follows the tablet: Settings › Display › Light / Dark mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextTertiary,
        )
    }
}

// ---------------------------------------------------------------- Behaviour

@Composable
private fun BehaviourCard(settings: MeterSettings, actions: SettingsActions) {
    GlassCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp), verticalSpacing = 0.dp) {
        ToggleRow(
            "Notification panel details",
            if (settings.showDetails) "On · ↓/↑ speed and today’s usage in the panel"
            else "Off · only a slim empty line in the panel, hidden on the lock screen",
            settings.showDetails,
            actions.onShowDetails,
        )
        Text(
            "Android draws the status bar speed from this notification, so it cannot disappear " +
                "from the panel completely — swiping it away would also remove the speed. " +
                "With details off it stays as small as Android allows.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextTertiary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        HorizontalDivider(color = Palette.Hairline)
        ToggleRow("Pause when screen is off", "Saves battery on the Tab A9", settings.pauseWhenScreenOff, actions.onPauseWhenScreenOff)
        HorizontalDivider(color = Palette.Hairline)
        ToggleRow("Start on boot", "Meter starts automatically after a restart", settings.startOnBoot, actions.onStartOnBoot)
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

// ---------------------------------------------------------------- Extras

@Composable
private fun ExtrasCard(bubbleEnabled: Boolean, onBubble: (Boolean) -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val overlayOk = remember(refresh) { Settings.canDrawOverlays(context) }

    GlassCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalSpacing = 0.dp) {
        Text("Extras", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        ToggleRow(
            "Floating bubble",
            if (bubbleEnabled && !overlayOk) "Waiting for “Display over other apps” permission"
            else "Optional · ↓/↑ speed on top of videos and games (drag to move)",
            bubbleEnabled,
        ) { on ->
            if (on && !Settings.canDrawOverlays(context)) openOverlaySettings(context)
            onBubble(on)
        }
        HorizontalDivider(color = Palette.Hairline)
        ActionRow(
            title = "Quick Settings tile",
            subtitle = "Live speed in the notification shade; tap it to toggle the bubble",
            action = "Add tile",
            onAction = { requestTile(context) },
        )
        HorizontalDivider(color = Palette.Hairline)
        ActionRow(
            title = "Home screen widget",
            subtitle = "4×2 glass widget with download, upload and today’s usage",
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

// ---------------------------------------------------------------- Alerts

@Composable
private fun AlertsCard(settings: MeterSettings, actions: SettingsActions) {
    GlassCard(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalSpacing = 0.dp) {
        ToggleRow("Data limit alerts", "Notify at 80% and 100% (needs Usage access)", settings.alertsEnabled, actions.onAlerts)
        HorizontalDivider(color = Palette.Hairline)
        Stepper(
            label = "Daily limit · all networks",
            value = limitLabel(settings.dailyLimitMb),
            onMinus = { actions.onDailyLimit((settings.dailyLimitMb - 500).coerceAtLeast(0)) },
            onPlus = { actions.onDailyLimit(settings.dailyLimitMb + 500) },
        )
        Stepper(
            label = "Monthly limit · mobile",
            value = limitLabel(settings.monthlyMobileLimitMb),
            onMinus = { actions.onMonthlyLimit((settings.monthlyMobileLimitMb - 1_000).coerceAtLeast(0)) },
            onPlus = { actions.onMonthlyLimit(settings.monthlyMobileLimitMb + 1_000) },
        )
        Stepper(
            label = "Billing cycle starts on day",
            value = settings.billingDay.toString(),
            onMinus = { actions.onBillingDay(if (settings.billingDay <= 1) 28 else settings.billingDay - 1) },
            onPlus = { actions.onBillingDay(if (settings.billingDay >= 28) 1 else settings.billingDay + 1) },
        )
    }
}

private fun limitLabel(mb: Int): String =
    if (mb <= 0) "Off" else String.format(Locale.US, if (mb % 1000 == 0) "%.0f GB" else "%.1f GB", mb / 1000.0)

// ---------------------------------------------------------------- Samsung checklist

@Composable
private fun SamsungChecklist(settings: MeterSettings, onStatusIconsConfirmed: (Boolean) -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val notificationsOk = remember(refresh) { notificationsAllowed(context) }
    val batteryOk = remember(refresh) { batteryUnrestricted(context) }
    val usageOk = remember(refresh) { (context.applicationContext as NetPulseApp).usage.hasAccess() }
    val overlayOk = remember(refresh) { Settings.canDrawOverlays(context) }
    val total = if (settings.bubbleEnabled) 5 else 4
    val done = listOf(notificationsOk, batteryOk, settings.statusIconsConfirmed, usageOk).count { it } +
        if (settings.bubbleEnabled && overlayOk) 1 else 0

    GlassCard(verticalSpacing = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Samsung setup", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Chip("$done / $total", if (done == total) Palette.Good else Palette.Warn)
        }
        Text(
            "One UI can hide or stop the meter. Fix these once.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextSecondary,
        )
        CheckItem(
            done = notificationsOk,
            title = "Allow notifications",
            detail = "The speed icon lives in a notification. Turn on “Allow notifications”.",
            action = "Open notification settings",
            onAction = { openAppNotificationSettings(context) },
        )
        CheckItem(
            done = batteryOk,
            title = "Battery: Unrestricted",
            detail = "Also add Netpulse to Battery › Background usage limits › Never sleeping apps.",
            action = "Allow background use",
            onAction = { requestBatteryExemption(context) },
        )
        CheckItem(
            done = settings.statusIconsConfirmed,
            title = "Show all status bar icons",
            detail = "Settings › Notifications › Advanced settings › Show notification icons › All. " +
                "Otherwise One UI may show only a dot or a count.",
            action = "Open Settings",
            onAction = { startSafely(context, Intent(Settings.ACTION_SETTINGS)) },
            secondaryAction = if (settings.statusIconsConfirmed) "Undo" else "Mark as done",
            onSecondary = { onStatusIconsConfirmed(!settings.statusIconsConfirmed) },
            showActionsWhenDone = true,
        )
        CheckItem(
            done = usageOk,
            title = "Usage access",
            detail = "Needed for data usage, per-app usage and limit alerts. Find Netpulse and switch it on.",
            action = "Open usage access",
            onAction = { openUsageAccess(context) },
        )
        if (settings.bubbleEnabled) {
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
private fun CheckItem(
    done: Boolean,
    title: String,
    detail: String,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondary: () -> Unit = {},
    showActionsWhenDone: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(22.dp)
                .then(
                    if (done) Modifier.background(Palette.Good, CircleShape)
                    else Modifier.border(2.dp, Palette.Warn, CircleShape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Text("✓", color = Palette.OnSelected, style = MaterialTheme.typography.labelSmall)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
            if (!done || showActionsWhenDone) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!done) PillButton(action, onAction, primary = false)
                    if (secondaryAction != null) PillButton(secondaryAction, onSecondary, primary = !done)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- About

@Composable
private fun AboutCard() {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"
    }
    GlassCard(verticalSpacing = 10.dp) {
        Text(context.getString(R.string.app_name), style = MaterialTheme.typography.titleLarge)
        Text("Version $version", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        Text(
            "No ads, no tracking. All data stays on this tablet; the internet is only used for speed tests.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextTertiary,
        )
        PillButton("App info", {
            startSafely(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
        }, primary = false)
    }
}

// ---------------------------------------------------------------- System helpers

private fun notificationsAllowed(context: Context): Boolean {
    val permission = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return permission && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

private fun batteryUnrestricted(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

private fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    startSafely(context, intent)
}

/** Shows the system “Let app always run in background?” dialog; falls back to App info. */
@SuppressLint("BatteryLife")
private fun requestBatteryExemption(context: Context) {
    val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
    val ok = runCatching { context.startActivity(direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    if (!ok) {
        startSafely(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
    }
}

private fun openUsageAccess(context: Context) {
    val withPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:${context.packageName}".toUri())
    val ok = runCatching { context.startActivity(withPackage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    if (!ok) startSafely(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
}

private fun openOverlaySettings(context: Context) {
    startSafely(
        context,
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()),
    )
}

private fun requestTile(context: Context) {
    val manual = "Swipe down twice › tap ✎ (Edit) › drag the Netpulse tile into place"
    if (Build.VERSION.SDK_INT < 33) {
        Toast.makeText(context, manual, Toast.LENGTH_LONG).show()
        return
    }
    val sbm = context.getSystemService(StatusBarManager::class.java)
    val requested = runCatching {
        sbm.requestAddTileService(
            ComponentName(context, SpeedTileService::class.java),
            context.getString(R.string.tile_label),
            android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_stat_pulse),
            context.mainExecutor,
        ) { result ->
            val message = when (result) {
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED -> "Tile added"
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED -> "The tile is already in Quick Settings"
                StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED -> null
                else -> manual
            }
            message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }.isSuccess
    if (!requested) Toast.makeText(context, manual, Toast.LENGTH_LONG).show()
}

private fun requestWidget(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val ok = manager.isRequestPinAppWidgetSupported &&
        runCatching { manager.requestPinAppWidget(ComponentName(context, SpeedWidgetProvider::class.java), null, null) }
            .getOrDefault(false)
    if (!ok) {
        Toast.makeText(context, "Long-press the home screen › Widgets › Netpulse", Toast.LENGTH_LONG).show()
    }
}

private fun startSafely(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}
