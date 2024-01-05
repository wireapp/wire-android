/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

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
