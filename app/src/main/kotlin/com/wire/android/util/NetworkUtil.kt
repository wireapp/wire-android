package com.wire.android.util

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import javax.inject.Inject

class NetworkUtil @Inject constructor(private val context: Context) {

    fun getNetworkStatus(): NetworkStatus {
        val connectivityManager: ConnectivityManager = context.getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            val onVpn = capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            val isNotMetered = capabilities == null || capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            NetworkStatus(onVpn, !isNotMetered)
        } else {
            NetworkStatus(isOnVpn = false, isMetered = false)
        }
    }
}
