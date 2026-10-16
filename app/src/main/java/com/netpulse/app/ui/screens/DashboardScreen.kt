package com.netpulse.app.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netpulse.app.data.ConnectionInfo
import com.netpulse.app.data.NetworkType
import com.netpulse.app.data.SessionTotals
import com.netpulse.app.data.SpeedFormat
import com.netpulse.app.data.SpeedRepository
import com.netpulse.app.data.SpeedSample
import com.netpulse.app.data.SpeedTestResult
import com.netpulse.app.data.TodayUsage
import com.netpulse.app.service.label
import com.netpulse.app.ui.components.GlassCard
import com.netpulse.app.ui.components.LiveGraph
import com.netpulse.app.ui.theme.Palette

@Composable
fun DashboardScreen(
    current: SpeedSample?,
    history: List<SpeedSample>,
    session: SessionTotals,
    connection: ConnectionInfo,
    today: TodayUsage?,
    lastTest: SpeedTestResult?,
    refreshSeconds: Int,
    wide: Boolean,
) {
    val graphTitle = "Last ${SpeedRepository.HISTORY_SIZE * refreshSeconds} seconds"
    val down = SpeedFormat.detailed(current?.downBps ?: 0)
    val up = SpeedFormat.detailed(current?.upBps ?: 0)
    // System numbers (needs Usage access) are more complete than what the meter saw itself.
    val usedLabel = if (today != null) "Used today" else "This session"
    val used = SpeedFormat.bytes(today?.today ?: (session.downBytes + session.upBytes))
    val ping = SpeedFormat.Parts(lastTest?.pingMs?.toString() ?: "—", "ms")

    val stats: @Composable (Modifier) -> Unit = { m ->
        StatCard("Download", down, Palette.Download, m)
        StatCard("Upload", up, Palette.Upload, m)
        StatCard(usedLabel, used, Palette.TextPrimary, m)
        StatCard("Ping · last test", ping, Palette.TextPrimary, m)
    }

    if (wide) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) { stats(Modifier.weight(1f)) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                GraphCard(graphTitle, history, Modifier.weight(1f).fillMaxSize())
                ConnectionCard(connection, Modifier.width(320.dp))
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard("Download", down, Palette.Download, Modifier.weight(1f))
                StatCard("Upload", up, Palette.Upload, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                StatCard(usedLabel, used, Palette.TextPrimary, Modifier.weight(1f))
                StatCard("Ping · last test", ping, Palette.TextPrimary, Modifier.weight(1f))
            }
            GraphCard(graphTitle, history, Modifier.fillMaxWidth().height(300.dp))
            ConnectionCard(connection, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatCard(label: String, parts: SpeedFormat.Parts, color: Color, modifier: Modifier) {
    GlassCard(modifier, verticalSpacing = 4.dp) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
        Text(
            buildAnnotatedString {
                append(parts.value)
                withStyle(SpanStyle(fontSize = 15.sp)) { append("  ${parts.unit}") }
            },
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun GraphCard(title: String, history: List<SpeedSample>, modifier: Modifier) {
    GlassCard(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Legend("Download", Palette.Download)
            Box(Modifier.width(14.dp))
            Legend("Upload", Palette.Upload)
        }
        LiveGraph(history, Modifier.fillMaxWidth().weight(1f))
    }
}

@Composable
private fun Legend(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(width = 14.dp, height = 3.dp).background(color, RoundedCornerShape(2.dp)))
        Text(text, style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
    }
}

@Composable
private fun ConnectionCard(info: ConnectionInfo, modifier: Modifier) {
    GlassCard(modifier) {
        Text(info.type.label(), style = MaterialTheme.typography.titleLarge)
        if (info.type == NetworkType.NONE) {
            Text("No active connection", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
            return@GlassCard
        }
        InfoRow("Internet", if (info.validated) "Working" else "No internet", if (info.validated) Palette.Good else Palette.Warn)
        info.carrier?.let { InfoRow("Carrier", it) }
        info.signalDbm?.let { dbm ->
            val (word, color) = signalQuality(dbm, info.type)
            InfoRow("Signal", "$dbm dBm · $word", color)
        }
        info.wifiBandMhz?.let { InfoRow("Band", if (it >= 5900) "6 GHz" else if (it >= 4900) "5 GHz" else "2.4 GHz") }
        info.wifiLinkMbps?.let { InfoRow("Link speed", "$it Mbps") }
        info.estDownKbps?.let { InfoRow("Est. bandwidth", "${it / 1000} Mbps") }
        InfoRow("Metered", if (info.metered) "Yes" else "No")
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = Palette.TextPrimary) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor)
    }
}

@Composable
private fun signalQuality(dbm: Int, type: NetworkType): Pair<String, Color> {
    val (good, fair) = if (type == NetworkType.WIFI) -60 to -75 else -90 to -105
    return when {
        dbm >= good -> "Strong" to Palette.Good
        dbm >= fair -> "Fair" to Palette.Warn
        else -> "Weak" to Palette.Upload
    }
}
