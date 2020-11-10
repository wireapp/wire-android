package com.wire.android.core.network

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Class which returns information about the network connection state.
 */
class NetworkHandler(private val connectivityManager: ConnectivityManager) {
    @SuppressWarnings("ReturnCount")
    fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}

