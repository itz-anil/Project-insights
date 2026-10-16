package com.netpulse.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.netpulse.app.MainActivity
import com.netpulse.app.R
import com.netpulse.app.data.NetworkType
import com.netpulse.app.data.SessionTotals
import com.netpulse.app.data.SpeedFormat

class MeterNotifications(private val context: Context) {

    private val manager = context.getSystemService(NotificationManager::class.java)

    private val openApp = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun ensureChannel() {
        // IMPORTANCE_LOW: silent, but still shows its icon in the status bar (MIN would hide it).
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_meter_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.channel_meter_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun placeholder() = base().setSmallIcon(R.drawable.ic_stat_pulse)
        .setContentTitle(context.getString(R.string.app_name))
        .build()

    fun build(
        icon: Bitmap,
        downBps: Long,
        upBps: Long,
        network: NetworkType,
        session: SessionTotals,
        showDetails: Boolean,
    ) = base().setSmallIcon(IconCompat.createWithBitmap(icon)).apply {
        if (showDetails) {
            val down = SpeedFormat.detailed(downBps)
            val up = SpeedFormat.detailed(upBps)
            setContentTitle("↓ $down    ↑ $up")
            setContentText(
                "${network.label()} · Session ↓ ${SpeedFormat.bytes(session.downBytes)}  ↑ ${SpeedFormat.bytes(session.upBytes)}"
            )
        } else {
            setContentTitle("${context.getString(R.string.app_name)} · ${SpeedFormat.detailed(downBps + upBps)}")
        }
    }.build()

    fun notify(notification: android.app.Notification) {
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun base() = NotificationCompat.Builder(context, CHANNEL_ID)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(false)
        .setLocalOnly(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(openApp)

    companion object {
        const val CHANNEL_ID = "speed_meter"
        const val NOTIFICATION_ID = 1001
    }
}

fun NetworkType.label() = when (this) {
    NetworkType.WIFI -> "Wi-Fi"
    NetworkType.MOBILE -> "Mobile data"
    NetworkType.ETHERNET -> "Ethernet"
    NetworkType.OTHER -> "Connected"
    NetworkType.NONE -> "Offline"
}
