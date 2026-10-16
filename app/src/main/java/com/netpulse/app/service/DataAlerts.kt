package com.netpulse.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.netpulse.app.MainActivity
import com.netpulse.app.R
import com.netpulse.app.data.MeterSettings
import com.netpulse.app.data.SettingsRepository
import com.netpulse.app.data.SpeedFormat
import com.netpulse.app.data.TodayUsage
import com.netpulse.app.data.UsageRepository

/** Daily (all networks) and monthly (mobile) data limit alerts. Each fires once per period. */
class DataAlerts(private val context: Context, private val settings: SettingsRepository) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun ensureChannel() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Data limit alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Warns when you reach your daily or monthly data limit."
            }
        )
    }

    suspend fun check(usage: TodayUsage, s: MeterSettings) {
        if (!s.alertsEnabled) return
        val dayKey = UsageRepository.startOfDay(System.currentTimeMillis())

        val daily = s.dailyLimitMb * 1_000_000L
        if (daily > 0 && usage.today >= daily) {
            once("day-$dayKey") {
                post(10, "Daily limit reached", "Used ${SpeedFormat.bytes(usage.today)} today (limit ${SpeedFormat.bytes(daily)}).")
            }
        }

        val monthly = s.monthlyMobileLimitMb * 1_000_000L
        if (monthly > 0) {
            val cycle = usage.cycleStartMs
            if (usage.cycleMobile >= monthly) {
                once("m100-$cycle") {
                    post(11, "Mobile data limit reached", "Used ${SpeedFormat.bytes(usage.cycleMobile)} of ${SpeedFormat.bytes(monthly)} this cycle.")
                }
            } else if (usage.cycleMobile >= monthly * 8 / 10) {
                once("m80-$cycle") {
                    post(11, "80% of mobile data used", "Used ${SpeedFormat.bytes(usage.cycleMobile)} of ${SpeedFormat.bytes(monthly)} this cycle.")
                }
            }
        }
    }

    private suspend fun once(mark: String, block: () -> Unit) {
        if (settings.alertShown(mark)) return
        block()
        settings.markAlertShown(mark)
    }

    private fun post(id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            context, 3, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pulse)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { manager.notify(id, n) }
    }

    companion object {
        const val CHANNEL_ID = "data_alerts"
    }
}
