package com.netpulse.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.netpulse.app.NetPulseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Restarts the meter after a reboot or an app update, if the user allows it. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val pending = goAsync()
        val app = context.applicationContext as NetPulseApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (app.settings.settings.first().startOnBoot) {
                    runCatching { SpeedMeterService.start(context) }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
