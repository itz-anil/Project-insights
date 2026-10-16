package com.netpulse.app.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.netpulse.app.MainActivity
import com.netpulse.app.NetPulseApp
import com.netpulse.app.R
import com.netpulse.app.data.NetworkType
import com.netpulse.app.data.SpeedFormat

/** 4×2 home screen widget. The meter service pushes fresh numbers every couple of seconds. */
class SpeedWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val app = context.applicationContext as NetPulseApp
        val sample = app.speed.current.value
        SpeedWidget.push(
            context,
            downBps = sample?.downBps ?: 0,
            upBps = sample?.upBps ?: 0,
            network = sample?.network ?: NetworkType.NONE,
            todayBytes = app.usage.today.value?.today,
        )
        runCatching { SpeedMeterService.start(context) }
    }
}

object SpeedWidget {
    fun hasWidgets(context: Context): Boolean =
        AppWidgetManager.getInstance(context).getAppWidgetIds(component(context)).isNotEmpty()

    fun push(context: Context, downBps: Long, upBps: Long, network: NetworkType, todayBytes: Long?) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(component(context))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_speed).apply {
            setTextViewText(R.id.widget_down, SpeedFormat.detailed(downBps).toString())
            setTextViewText(R.id.widget_up, SpeedFormat.detailed(upBps).toString())
            setTextViewText(
                R.id.widget_today,
                todayBytes?.let { "Today ${SpeedFormat.bytes(it)}" } ?: context.getString(R.string.app_name),
            )
            setTextViewText(R.id.widget_network, network.label())
            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context,
                    1,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        }
        manager.updateAppWidget(ids, views)
    }

    private fun component(context: Context) = ComponentName(context, SpeedWidgetProvider::class.java)
}
