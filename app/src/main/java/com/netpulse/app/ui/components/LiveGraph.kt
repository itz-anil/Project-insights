package com.netpulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netpulse.app.data.SpeedRepository
import com.netpulse.app.data.SpeedSample
import com.netpulse.app.ui.theme.Palette

/** Last 60 seconds of download (filled) and upload (line). */
@Composable
fun LiveGraph(samples: List<SpeedSample>, modifier: Modifier = Modifier) {
    val peak = samples.maxOfOrNull { maxOf(it.downBps, it.upBps) } ?: 0L
    // Never scale below 100 KB/s so an idle connection reads as a flat line, not noise.
    val scale = maxOf(peak * 1.15f, 100_000f)

    Canvas(modifier.semantics { contentDescription = "Download and upload speed, last 60 seconds" }) {
        val grid = Color(0x1AFFFFFF)
        for (i in 1..3) {
            val y = size.height * i / 4f
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }
        if (samples.size < 2) return@Canvas

        val step = size.width / (SpeedRepository.HISTORY_SIZE - 1)
        val startX = size.width - step * (samples.size - 1)
        fun x(i: Int) = startX + step * i
        fun y(v: Long) = size.height - (v / scale) * size.height

        val down = Path().apply {
            samples.forEachIndexed { i, s -> if (i == 0) moveTo(x(i), y(s.downBps)) else lineTo(x(i), y(s.downBps)) }
        }
        val area = Path().apply {
            addPath(down)
            lineTo(x(samples.lastIndex), size.height)
            lineTo(x(0), size.height)
            close()
        }
        val up = Path().apply {
            samples.forEachIndexed { i, s -> if (i == 0) moveTo(x(i), y(s.upBps)) else lineTo(x(i), y(s.upBps)) }
        }
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawPath(area, Brush.verticalGradient(listOf(Palette.Download.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(down, Palette.Download, style = stroke)
        drawPath(up, Palette.Upload, style = stroke)
    }
}
