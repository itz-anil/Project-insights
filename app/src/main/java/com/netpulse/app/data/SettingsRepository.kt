package com.netpulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** What the status bar icon shows. The icon itself is always on (mandatory). */
enum class IconStyle { TOTAL, DOWNLOAD, BOTH }

data class MeterSettings(
    val iconStyle: IconStyle = IconStyle.TOTAL,
    /** Optional: rich notification panel. Off = one silent line. */
    val showDetails: Boolean = true,
    val pauseWhenScreenOff: Boolean = true,
    val startOnBoot: Boolean = true,
    /** Optional speed overlay on top of other apps. */
    val bubbleEnabled: Boolean = false,
    val alertsEnabled: Boolean = true,
    /** All networks, per day. 0 = off. */
    val dailyLimitMb: Int = 2_000,
    /** Mobile data, per billing cycle. 0 = off. */
    val monthlyMobileLimitMb: Int = 5_000,
    /** Day of month (1–28) the mobile billing cycle starts. */
    val billingDay: Int = 1,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val iconStyle = stringPreferencesKey("icon_style")
        val showDetails = booleanPreferencesKey("show_details")
        val pauseWhenScreenOff = booleanPreferencesKey("pause_screen_off")
        val startOnBoot = booleanPreferencesKey("start_on_boot")
        val bubble = booleanPreferencesKey("bubble")
        val alerts = booleanPreferencesKey("alerts")
        val dailyLimit = intPreferencesKey("daily_limit_mb")
        val monthlyLimit = intPreferencesKey("monthly_mobile_limit_mb")
        val billingDay = intPreferencesKey("billing_day")
        val alertMarks = stringPreferencesKey("alert_marks")
    }

    val settings: Flow<MeterSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setIconStyle(style: IconStyle) = edit { it[Keys.iconStyle] = style.name }
    suspend fun setShowDetails(value: Boolean) = edit { it[Keys.showDetails] = value }
    suspend fun setPauseWhenScreenOff(value: Boolean) = edit { it[Keys.pauseWhenScreenOff] = value }
    suspend fun setStartOnBoot(value: Boolean) = edit { it[Keys.startOnBoot] = value }
    suspend fun setBubbleEnabled(value: Boolean) = edit { it[Keys.bubble] = value }
    suspend fun setAlertsEnabled(value: Boolean) = edit { it[Keys.alerts] = value }
    suspend fun setDailyLimitMb(value: Int) = edit { it[Keys.dailyLimit] = value.coerceIn(0, 100_000) }
    suspend fun setMonthlyMobileLimitMb(value: Int) = edit { it[Keys.monthlyLimit] = value.coerceIn(0, 1_000_000) }
    suspend fun setBillingDay(value: Int) = edit { it[Keys.billingDay] = value.coerceIn(1, 28) }

    /** Remembers which alerts were already shown, so each fires once. */
    suspend fun alertShown(mark: String): Boolean =
        context.dataStore.data.first()[Keys.alertMarks]?.split('|')?.contains(mark) == true

    suspend fun markAlertShown(mark: String) = edit {
        val marks = (it[Keys.alertMarks]?.split('|').orEmpty() + mark).takeLast(20)
        it[Keys.alertMarks] = marks.joinToString("|")
    }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private fun Preferences.toSettings(): MeterSettings {
        val d = MeterSettings()
        return MeterSettings(
            iconStyle = this[Keys.iconStyle]
                ?.let { name -> IconStyle.entries.firstOrNull { it.name == name } }
                ?: d.iconStyle,
            showDetails = this[Keys.showDetails] ?: d.showDetails,
            pauseWhenScreenOff = this[Keys.pauseWhenScreenOff] ?: d.pauseWhenScreenOff,
            startOnBoot = this[Keys.startOnBoot] ?: d.startOnBoot,
            bubbleEnabled = this[Keys.bubble] ?: d.bubbleEnabled,
            alertsEnabled = this[Keys.alerts] ?: d.alertsEnabled,
            dailyLimitMb = this[Keys.dailyLimit] ?: d.dailyLimitMb,
            monthlyMobileLimitMb = this[Keys.monthlyLimit] ?: d.monthlyMobileLimitMb,
            billingDay = this[Keys.billingDay] ?: d.billingDay,
        )
    }
}
