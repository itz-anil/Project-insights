package com.netpulse.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netpulse.app.data.SpeedFormat
import com.netpulse.app.data.SpeedTestResult
import com.netpulse.app.data.TestPhase
import com.netpulse.app.data.TestProgress
import com.netpulse.app.service.label
import com.netpulse.app.ui.components.GlassCard
import com.netpulse.app.ui.components.PillButton
import com.netpulse.app.ui.theme.Mono
import com.netpulse.app.ui.theme.Palette
import java.text.DateFormat
import java.util.Date
import kotlin.math.ln
import kotlin.math.min

@Composable
fun SpeedTestScreen(
    test: TestProgress,
    history: List<SpeedTestResult>,
    wide: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClearHistory: () -> Unit,
) {
    val main: @Composable (Modifier) -> Unit = { m -> TestCard(test, onStart, onStop, m) }
    if (wide) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            main(Modifier.weight(1f).fillMaxSize())
            HistoryCard(history, onClearHistory, Modifier.width(400.dp).fillMaxSize(), scroll = true)
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            main(Modifier.fillMaxWidth())
            HistoryCard(history, onClearHistory, Modifier.fillMaxWidth(), scroll = false)
        }
    }
}

@Composable
private fun TestCard(test: TestProgress, onStart: () -> Unit, onStop: () -> Unit, modifier: Modifier) {
    val running = test.phase in setOf(TestPhase.PING, TestPhase.DOWNLOAD, TestPhase.UPLOAD)
    val shown = when (test.phase) {
        TestPhase.DOWNLOAD, TestPhase.UPLOAD -> test.liveBps
        TestPhase.DONE -> test.downBps ?: 0
        else -> 0
    }
    val gaugeColor = if (test.phase == TestPhase.UPLOAD) Palette.Upload else Palette.Download
    val status = when (test.phase) {
        TestPhase.IDLE -> "Ready"
        TestPhase.PING -> "Measuring ping…"
        TestPhase.DOWNLOAD -> "Download"
        TestPhase.UPLOAD -> "Upload"
        TestPhase.DONE -> "Download · done"
        TestPhase.ERROR -> test.error ?: "Test failed"
    }

    GlassCard(modifier, verticalSpacing = 18.dp) {
        Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.TopCenter) {
            Gauge(shown, gaugeColor, Modifier.size(width = 360.dp, height = 230.dp))
            Column(
                Modifier.padding(top = 78.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val parts = SpeedFormat.detailed(shown)
                Text(parts.value, style = MaterialTheme.typography.displayLarge)
                Text(
                    "${parts.unit} · $status",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (test.phase == TestPhase.ERROR) Palette.Warn else Palette.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultTile("DOWNLOAD", test.downBps?.let { SpeedFormat.detailed(it).toString() }, Palette.Download, Modifier.weight(1f))
            ResultTile("UPLOAD", test.upBps?.let { SpeedFormat.detailed(it).toString() }, Palette.Upload, Modifier.weight(1f))
            ResultTile("PING", test.pingMs?.let { "$it ms" }, Palette.TextPrimary, Modifier.weight(1f))
            ResultTile("JITTER", test.jitterMs?.let { "$it ms" }, Palette.TextPrimary, Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            if (running) {
                PillButton("Stop", onStop, primary = false)
            } else {
                PillButton(if (test.phase == TestPhase.IDLE) "Start test" else "Run again", onStart)
            }
        }
        Text(
            "Uses Cloudflare's speed test servers. A test can use 50–200 MB of data on a fast connection.",
            style = MaterialTheme.typography.bodyMedium,
            color = Palette.TextTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 240° arc on a log scale, so both 0.5 MB/s and 50 MB/s move the needle visibly. */
@Composable
private fun Gauge(bps: Long, color: Color, modifier: Modifier) {
    val mb = bps / 1_000_000.0
    val target = min(1.0, ln(1 + mb) / ln(1 + 100.0)).toFloat()
    val fraction by animateFloatAsState(target, tween(300), label = "gauge")
    val track = Palette.FillStrong
    Canvas(modifier.semantics { contentDescription = "Speed gauge" }) {
        val stroke = 20.dp.toPx()
        // A 240° arc is 0.75 × its diameter tall, so size it to fit the height.
        val diameter = min(size.width - stroke, (size.height - stroke) / 0.75f)
        val topLeft = Offset((size.width - diameter) / 2, stroke / 2)
        val arcSize = Size(diameter, diameter)
        drawArc(track, 150f, 240f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        if (fraction > 0.001f) {
            drawArc(color, 150f, 240f * fraction, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun ResultTile(label: String, value: String?, color: Color, modifier: Modifier) {
    GlassCard(modifier, corner = 20.dp, contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalSpacing = 4.dp) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.TextSecondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text(
            value ?: "—",
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = Mono),
            color = color,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryCard(history: List<SpeedTestResult>, onClear: () -> Unit, modifier: Modifier, scroll: Boolean) {
    val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
    GlassCard(modifier, verticalSpacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("History", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            if (history.isNotEmpty()) PillButton("Clear", onClear, primary = false)
        }
        if (history.isEmpty()) {
            Text("Your results will appear here.", style = MaterialTheme.typography.bodyMedium, color = Palette.TextSecondary)
        }
        Column(
            if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            history.forEach { r ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row {
                        Text(format.format(Date(r.timestampMs)), style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
                        Text(r.network.label(), style = MaterialTheme.typography.labelMedium, color = Palette.TextSecondary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("↓ ${SpeedFormat.detailed(r.downBps)}", style = MaterialTheme.typography.labelLarge.copy(fontFamily = Mono), color = Palette.Download)
                        Text("↑ ${SpeedFormat.detailed(r.upBps)}", style = MaterialTheme.typography.labelLarge.copy(fontFamily = Mono), color = Palette.Upload)
                        Text("${r.pingMs} ms", style = MaterialTheme.typography.labelLarge.copy(fontFamily = Mono))
                    }
                }
            }
        }
    }
}
