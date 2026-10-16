package com.netpulse.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.netpulse.app.R
import com.netpulse.app.data.SpeedFormat
import kotlin.math.roundToInt

/** Small draggable speed pill drawn over other apps. Main thread only. */
class FloatingBubble(private val context: Context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private var view: TextView? = null
    private val density = context.resources.displayMetrics.density

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = (16 * density).roundToInt()
        y = (120 * density).roundToInt()
    }

    fun sync(enabled: Boolean, downBps: Long, upBps: Long) {
        if (enabled && Settings.canDrawOverlays(context)) {
            if (view == null) show()
            view?.text = format(downBps, upBps)
        } else {
            hide()
        }
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun show() {
        val pad = (10 * density).roundToInt()
        val text = TextView(context).apply {
            setPadding(pad + pad / 2, pad - 2, pad + pad / 2, pad - 2)
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = ResourcesCompat.getFont(context, R.font.jetbrains_mono)
            fontVariationSettings = "'wght' 700"
            background = GradientDrawable().apply {
                cornerRadius = 999f
                setColor(0xD9101422.toInt())
                setStroke((1 * density).roundToInt(), 0x4DFFFFFF)
            }
            elevation = 6 * density
        }
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        text.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - touchX).roundToInt()
                    params.y = startY + (e.rawY - touchY).roundToInt()
                    runCatching { wm.updateViewLayout(v, params) }
                    true
                }
                else -> false
            }
        }
        runCatching { wm.addView(text, params) }.onSuccess { view = text }
    }

    private fun format(downBps: Long, upBps: Long): CharSequence {
        val down = "↓" + SpeedFormat.compact(downBps).let { "${it.value} ${it.unit}" }
        val up = "↑" + SpeedFormat.compact(upBps).let { "${it.value} ${it.unit}" }
        return SpannableStringBuilder().apply {
            append(down, ForegroundColorSpan(0xFF6CB8FF.toInt()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append("  ")
            append(up, ForegroundColorSpan(0xFFFFB340.toInt()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
