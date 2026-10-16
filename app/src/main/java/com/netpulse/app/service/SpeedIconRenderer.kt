package com.netpulse.app.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.netpulse.app.R
import com.netpulse.app.data.IconStyle
import com.netpulse.app.data.SpeedFormat
import kotlin.math.roundToInt

/**
 * Draws the speed as text into a square bitmap used as the notification's small icon.
 * Android tints status bar icons, so only the alpha channel matters: draw in white.
 */
class SpeedIconRenderer(context: Context, sizeDp: Int = 24) {

    private val sizePx = (sizeDp * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(48)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans) ?: Typeface.DEFAULT_BOLD
        fontVariationSettings = "'wght' 800"
    }

    fun render(style: IconStyle, downBps: Long, upBps: Long): Bitmap {
        val bitmap = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bitmap)
        val s = sizePx.toFloat()
        when (style) {
            IconStyle.TOTAL -> valueAndUnit(canvas, s, SpeedFormat.compact(downBps + upBps), "")
            IconStyle.DOWNLOAD -> valueAndUnit(canvas, s, SpeedFormat.compact(downBps), "↓")
            IconStyle.BOTH -> {
                drawFitted(canvas, "↓" + SpeedFormat.compact(downBps).value, s * 0.27f, s * 0.46f, s)
                drawFitted(canvas, "↑" + SpeedFormat.compact(upBps).value, s * 0.75f, s * 0.46f, s)
            }
        }
        return bitmap
    }

    private fun valueAndUnit(canvas: Canvas, s: Float, parts: SpeedFormat.Parts, prefix: String) {
        drawFitted(canvas, parts.value, s * 0.33f, s * 0.62f, s)
        drawFitted(canvas, prefix + parts.unit, s * 0.81f, s * 0.34f, s)
    }

    /** Draws [text] centred on [centerY], as tall as [height] but never wider than [width]. */
    private fun drawFitted(canvas: Canvas, text: String, centerY: Float, height: Float, width: Float) {
        paint.textSize = height
        val measured = paint.measureText(text)
        if (measured > width) paint.textSize = height * width / measured
        val fm = paint.fontMetrics
        // Centre on the cap height region rather than the full ascent/descent box.
        val baseline = centerY - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, width / 2f, baseline, paint)
    }
}
