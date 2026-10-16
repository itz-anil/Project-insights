package com.netpulse.app.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Single preferences file shared by settings, alert bookkeeping and speed test history. */
internal val Context.dataStore by preferencesDataStore(name = "settings")
