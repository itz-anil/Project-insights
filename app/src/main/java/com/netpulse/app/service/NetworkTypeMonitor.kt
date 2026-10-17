package com.netpulse.app.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.netpulse.app.data.ConnectionReader
import com.netpulse.app.data.NetworkType

/**
 * Keeps the current network type up to date through a callback, so the meter
 * does not have to ask the system every second (saves a binder call per tick).
 */
class NetworkTypeMonitor(context: Context) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)

    @Volatile
    var type: NetworkType = ConnectionReader(context).currentType()
        private set

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        }

        override fun onLost(network: Network) {
            type = NetworkType.NONE
        }
    }

    fun start() {
        runCatching { cm?.registerDefaultNetworkCallback(callback) }
    }

    fun stop() {
        runCatching { cm?.unregisterNetworkCallback(callback) }
    }
}
