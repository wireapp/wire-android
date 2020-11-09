package com.wire.android.core.network

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Class which returns information about the network connection state.
 */
class NetworkHandler(private val connectivityManager: ConnectivityManager) {
    fun isConnected(): Boolean {
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}
