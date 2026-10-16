package com.netpulse.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.telephony.TelephonyManager

data class ConnectionInfo(
    val type: NetworkType = NetworkType.NONE,
    val validated: Boolean = false,
    val metered: Boolean = false,
    /** dBm, or null when the system does not report it. */
    val signalDbm: Int? = null,
    val wifiBandMhz: Int? = null,
    val wifiLinkMbps: Int? = null,
    val estDownKbps: Int? = null,
    val estUpKbps: Int? = null,
    val carrier: String? = null,
)

/** Reads connection details without location permission (so no Wi-Fi name). */
class ConnectionReader(context: Context) {
    private val app = context.applicationContext
    private val cm = app.getSystemService(ConnectivityManager::class.java)
    private val wifi = app.getSystemService(WifiManager::class.java)
    private val telephony = app.getSystemService(TelephonyManager::class.java)

    fun currentType(): NetworkType = cm?.getNetworkCapabilities(cm.activeNetwork).toType()

    @Suppress("DEPRECATION")
    fun read(): ConnectionInfo {
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork) ?: return ConnectionInfo()
        val type = caps.toType()
        val signal = if (Build.VERSION.SDK_INT >= 29) {
            caps.signalStrength.takeIf { it != Int.MIN_VALUE && it != 0 }
        } else null
        val wifiInfo = if (type == NetworkType.WIFI) wifi?.connectionInfo else null
        return ConnectionInfo(
            type = type,
            validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED),
            metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            signalDbm = signal ?: wifiInfo?.rssi?.takeIf { it > -127 && it < 0 },
            wifiBandMhz = wifiInfo?.frequency?.takeIf { it > 0 },
            wifiLinkMbps = wifiInfo?.linkSpeed?.takeIf { it > 0 },
            estDownKbps = caps.linkDownstreamBandwidthKbps.takeIf { it > 0 },
            estUpKbps = caps.linkUpstreamBandwidthKbps.takeIf { it > 0 },
            carrier = if (type == NetworkType.MOBILE) {
                telephony?.networkOperatorName?.takeIf { it.isNotBlank() }
            } else null,
        )
    }

    private fun NetworkCapabilities?.toType(): NetworkType = when {
        this == null -> NetworkType.NONE
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
        else -> NetworkType.OTHER
    }
}
