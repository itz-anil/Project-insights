package com.netpulse.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** What the status bar icon shows. The icon itself is always on (mandatory). */
enum class IconStyle { TOTAL, DOWNLOAD, BOTH }

data class MeterSettings(
    val iconStyle: IconStyle = IconStyle.TOTAL,
    /** Optional: rich notification panel (↓/↑ and session data). Off = one silent line. */
    val showDetails: Boolean = true,
    val pauseWhenScreenOff: Boolean = true,
    val startOnBoot: Boolean = true,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val iconStyle = stringPreferencesKey("icon_style")
        val showDetails = booleanPreferencesKey("show_details")
        val pauseWhenScreenOff = booleanPreferencesKey("pause_screen_off")
        val startOnBoot = booleanPreferencesKey("start_on_boot")
    }

    val settings: Flow<MeterSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun setIconStyle(style: IconStyle) = edit { it[Keys.iconStyle] = style.name }
    suspend fun setShowDetails(value: Boolean) = edit { it[Keys.showDetails] = value }
    suspend fun setPauseWhenScreenOff(value: Boolean) = edit { it[Keys.pauseWhenScreenOff] = value }
    suspend fun setStartOnBoot(value: Boolean) = edit { it[Keys.startOnBoot] = value }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    private fun Preferences.toSettings(): MeterSettings {
        val defaults = MeterSettings()
        return MeterSettings(
            iconStyle = this[Keys.iconStyle]
                ?.let { name -> IconStyle.entries.firstOrNull { it.name == name } }
                ?: defaults.iconStyle,
            showDetails = this[Keys.showDetails] ?: defaults.showDetails,
            pauseWhenScreenOff = this[Keys.pauseWhenScreenOff] ?: defaults.pauseWhenScreenOff,
            startOnBoot = this[Keys.startOnBoot] ?: defaults.startOnBoot,
        )
    }
}
