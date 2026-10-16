package com.netpulse.app.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.Locale

data class TodayUsage(
    val wifi: Long,
    val mobile: Long,
    val cycleWifi: Long,
    val cycleMobile: Long,
    val cycleStartMs: Long,
) {
    val today: Long get() = wifi + mobile
}

data class DayUsage(val label: String, val wifi: Long, val mobile: Long)

data class AppUsage(
    val name: String,
    val packageName: String?,
    val icon: Bitmap?,
    val wifi: Long,
    val mobile: Long,
) {
    val total: Long get() = wifi + mobile
}

data class UsageSnapshot(
    val today: TodayUsage,
    val days: List<DayUsage>,
    val apps: List<AppUsage>,
)

/**
 * Real data usage from the system's NetworkStatsManager (needs "Usage access").
 * Unlike the live meter, this also counts traffic from when the app was not running.
 */
class UsageRepository(context: Context) {
    private val app = context.applicationContext
    private val nsm = app.getSystemService(NetworkStatsManager::class.java)
    private val pm = app.packageManager

    private val _today = MutableStateFlow<TodayUsage?>(null)
    /** Latest quick numbers, refreshed by the meter service and the UI. */
    val today: StateFlow<TodayUsage?> = _today.asStateFlow()

    fun hasAccess(): Boolean {
        val ops = app.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Cheap: four system queries. Call off the main thread. */
    fun refreshToday(billingDay: Int): TodayUsage? {
        if (!hasAccess()) return null.also { _today.value = null }
        val now = System.currentTimeMillis()
        val dayStart = startOfDay(now)
        val cycleStart = cycleStart(now, billingDay)
        return TodayUsage(
            wifi = device(WIFI, dayStart, now),
            mobile = device(MOBILE, dayStart, now),
            cycleWifi = device(WIFI, cycleStart, now),
            cycleMobile = device(MOBILE, cycleStart, now),
            cycleStartMs = cycleStart,
        ).also { _today.value = it }
    }

    /** Full picture for the Usage screen: 14 days plus top apps this cycle. */
    fun snapshot(billingDay: Int, days: Int = 14): UsageSnapshot? {
        val today = refreshToday(billingDay) ?: return null
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(now) }
        cal.add(Calendar.DAY_OF_MONTH, -(days - 1))
        val list = (0 until days).map {
            val start = cal.timeInMillis
            val label = String.format(Locale.US, "%d", cal.get(Calendar.DAY_OF_MONTH))
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val end = minOf(cal.timeInMillis, now)
            DayUsage(label, device(WIFI, start, end), device(MOBILE, start, end))
        }
        return UsageSnapshot(today, list, apps(today.cycleStartMs, now))
    }

    private fun apps(start: Long, end: Long): List<AppUsage> {
        val totals = HashMap<Int, LongArray>()
        listOf(WIFI, MOBILE).forEachIndexed { index, type ->
            val stats = runCatching { nsm?.querySummary(type, null, start, end) }.getOrNull() ?: return@forEachIndexed
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                totals.getOrPut(bucket.uid) { LongArray(2) }[index] += bucket.rxBytes + bucket.txBytes
            }
            stats.close()
        }
        return totals.entries
            .map { (uid, bytes) -> describe(uid, bytes[0], bytes[1]) }
            .groupBy { it.name }
            .map { (_, same) -> same.reduce { a, b -> a.copy(wifi = a.wifi + b.wifi, mobile = a.mobile + b.mobile) } }
            .filter { it.total > 0 }
            .sortedByDescending { it.total }
            .take(10)
    }

    private fun describe(uid: Int, wifi: Long, mobile: Long): AppUsage {
        val special = when {
            uid == NetworkStats.Bucket.UID_REMOVED -> "Removed apps"
            uid == NetworkStats.Bucket.UID_TETHERING -> "Hotspot & tethering"
            uid < Process.FIRST_APPLICATION_UID -> "Android system"
            else -> null
        }
        if (special != null) return AppUsage(special, null, null, wifi, mobile)
        val pkg = pm.getPackagesForUid(uid)?.firstOrNull()
        val info = pkg?.let { runCatching { pm.getApplicationInfo(it, 0) }.getOrNull() }
        val name = info?.let { pm.getApplicationLabel(it).toString() } ?: pkg ?: "App $uid"
        val icon = info?.let { runCatching { pm.getApplicationIcon(it).toBitmap(96, 96) }.getOrNull() }
        return AppUsage(name, pkg, icon, wifi, mobile)
    }

    private fun device(type: Int, start: Long, end: Long): Long = runCatching {
        nsm?.querySummaryForDevice(type, null, start, end)?.let { it.rxBytes + it.txBytes } ?: 0L
    }.getOrDefault(0L)

    companion object {
        @Suppress("DEPRECATION")
        private const val WIFI = ConnectivityManager.TYPE_WIFI
        @Suppress("DEPRECATION")
        private const val MOBILE = ConnectivityManager.TYPE_MOBILE

        fun startOfDay(ms: Long): Long = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        /** Start of the billing cycle that contains [ms]. */
        fun cycleStart(ms: Long, billingDay: Int): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(ms) }
            val day = billingDay.coerceIn(1, 28)
            if (cal.get(Calendar.DAY_OF_MONTH) < day) cal.add(Calendar.MONTH, -1)
            cal.set(Calendar.DAY_OF_MONTH, day)
            return cal.timeInMillis
        }
    }
}

