package com.netpulse.app.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.netpulse.app.data.AppUsage
import com.netpulse.app.data.DayUsage
import com.netpulse.app.data.MeterSettings
import com.netpulse.app.data.SpeedFormat
import com.netpulse.app.ui.UsageUiState
import com.netpulse.app.ui.components.GlassCard
import com.netpulse.app.ui.components.PillButton
import com.netpulse.app.ui.theme.Mono
import com.netpulse.app.ui.theme.Palette
import java.text.DateFormat
import java.util.Date

@Composable
fun UsageScreen(state: UsageUiState, settings: MeterSettings, wide: Boolean) {
    val context = LocalContext.current
    val snapshot = state.snapshot
    when {
        !state.hasAccess -> Centered {
            GlassCard(Modifier.widthIn(max = 560.dp), verticalSpacing = 14.dp) {
                Text("Usage access needed", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Android keeps exact Wi-Fi and mobile data numbers, even for time the meter was off. " +
                        "Allow NetPulse to read them: find NetPulse in the list and switch it on.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Palette.TextSecondary,
                )
                PillButton("Grant usage access", { openUsageAccess(context) })
            }
        }
        snapshot == null -> Centered {
            Text(
                if (state.loading) "Reading data usage…" else "Could not read data usage",
                style = MaterialTheme.typography.titleLarge,
                color = Palette.TextSecondary,
            )
        }
        else -> {
            val t = snapshot.today
            val cycleFrom = remember(t.cycleStartMs) {
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(t.cycleStartMs))
            }
            val summary: @Composable (Modifier) -> Unit = { m ->
                BytesCard("Wi-Fi · since $cycleFrom", t.cycleWifi, Palette.Download, m)
                BytesCard("Mobile · since $cycleFrom", t.cycleMobile, Palette.Upload, m)
                LimitCard(t.cycleMobile, settings.monthlyMobileLimitMb, m)
            }
            if (wide) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { summary(Modifier.weight(1f)) }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DailyChart(snapshot.days, Modifier.weight(1f).fillMaxSize())
                        TopApps(snapshot.apps, Modifier.width(380.dp).fillMaxSize(), scroll = true)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    summary(Modifier.fillMaxWidth())
                    DailyChart(snapshot.days, Modifier.fillMaxWidth().height(300.dp))
                    TopApps(snapshot.apps, Modifier.fillMaxWidth(), scroll = false)
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun BytesCard(label: String, bytes: Long, color: Color, modifier: Modifier) {
    val parts = SpeedFormat.bytes(bytes)
    GlassCard(modifier, verticalSpacing = 4.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary, maxLines = 1)
        Text(
            buildAnnotatedString {
                append(parts.value)
                withStyle(SpanStyle(fontSize = 15.sp)) { append("  ${parts.unit}") }
            },
            style = MaterialTheme.typography.headlineSmall,
            color = color,
        )
    }
}

@Composable
private fun LimitCard(used: Long, limitMb: Int, modifier: Modifier) {
    GlassCard(modifier, verticalSpacing = 10.dp) {
        if (limitMb <= 0) {
            Text("Mobile data limit", style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
            Text("Off · set it in Settings", style = MaterialTheme.typography.titleMedium)
            return@GlassCard
        }
        val limit = limitMb * 1_000_000L
        val fraction = (used.toFloat() / limit).coerceIn(0f, 1f)
        val barColor = if (fraction >= 1f) Color(0xFFFF6B5E) else if (fraction >= 0.8f) Palette.Warn else Palette.Upload
        Row {
            Text("Mobile data limit", style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
            Text("${SpeedFormat.bytes(used)} / ${SpeedFormat.bytes(limit)}", style = MaterialTheme.typography.labelMedium)
        }
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color(0x24FFFFFF))) {
            Box(Modifier.fillMaxWidth(fraction).height(10.dp).clip(CircleShape).background(barColor))
        }
        Text("Alert at 80% and 100%", style = MaterialTheme.typography.labelMedium, color = Palette.TextTertiary)
    }
}

@Composable
private fun DailyChart(days: List<DayUsage>, modifier: Modifier) {
    val max = (days.maxOfOrNull { it.wifi + it.mobile } ?: 0L).coerceAtLeast(1L)
    GlassCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Daily usage · 14 days", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Key("Wi-Fi", Palette.Download)
            Box(Modifier.width(14.dp))
            Key("Mobile", Palette.Upload)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics { contentDescription = "Bar chart of Wi-Fi and mobile data per day" }
        ) {
            val gap = 10.dp.toPx()
            val barW = (size.width - gap * (days.size - 1)) / days.size
            val radius = CornerRadius(5.dp.toPx())
            days.forEachIndexed { i, d ->
                val x = i * (barW + gap)
                val wifiH = size.height * d.wifi / max
                val mobileH = size.height * d.mobile / max
                if (mobileH > 0) {
                    drawRoundRect(Palette.Upload, Offset(x, size.height - mobileH), Size(barW, mobileH), radius)
                }
                if (wifiH > 0) {
                    val top = size.height - mobileH - wifiH - if (mobileH > 0) 3.dp.toPx() else 0f
                    drawRoundRect(Palette.Download, Offset(x, top), Size(barW, wifiH), radius)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            days.forEach {
                Text(
                    it.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = Mono),
                    color = Palette.TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Key(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
    }
}

@Composable
private fun TopApps(apps: List<AppUsage>, modifier: Modifier, scroll: Boolean) {
    val top = apps.firstOrNull()?.total?.coerceAtLeast(1L) ?: 1L
    GlassCard(modifier, verticalSpacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Top apps", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text("this cycle", style = MaterialTheme.typography.labelMedium, color = Palette.TextTertiary)
        }
        if (apps.isEmpty()) {
            Text("No app data yet", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        // The narrow layout already scrolls the whole page; nesting another scroll would crash.
        Column(
            if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            apps.forEach { AppRow(it, top) }
        }
    }
}

@Composable
private fun AppRow(app: AppUsage, top: Long) {
    val icon = remember(app.icon) { app.icon?.asImageBitmap() }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)))
            } else {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x29FFFFFF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(app.name.take(1), style = MaterialTheme.typography.labelLarge)
                }
            }
            Text(
                app.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(SpeedFormat.bytes(app.total).toString(), style = MaterialTheme.typography.labelLarge.copy(fontFamily = Mono))
        }
        Row(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0x1FFFFFFF))) {
            val wifiFrac = app.wifi.toFloat() / top
            val mobileFrac = app.mobile.toFloat() / top
            if (wifiFrac > 0f) Box(Modifier.weight(wifiFrac).height(6.dp).background(Palette.Download))
            if (mobileFrac > 0f) Box(Modifier.weight(mobileFrac).height(6.dp).background(Palette.Upload))
            val rest = 1f - wifiFrac - mobileFrac
            if (rest > 0.001f) Box(Modifier.weight(rest).height(6.dp))
        }
    }
}

private fun openUsageAccess(context: Context) {
    val withPackage = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, "package:${context.packageName}".toUri())
    val plain = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    runCatching { context.startActivity(withPackage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { runCatching { context.startActivity(plain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
}
