/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.debug.securityproviders

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.net.toUri
import com.wire.android.appLogger
import java.io.IOException
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Reports how the device is currently reaching the backend: whether the active network is a VPN and which
 * addresses the backend host resolves to through that very network, which is what a split tunnel or a
 * misbehaving DNS would show up in.
 *
 * Resolution hits the network, so this must be called off the main thread.
 */
class NetworkDiagnosticsProvider(
    private val context: Context,
) {
    operator fun invoke(apiUrl: String): NetworkDiagnostics {
        val host = apiUrl.toUri().host.orEmpty()
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork
            ?: return NetworkDiagnostics(
                isVpn = false,
                networkTypes = emptyList(),
                backendHost = host,
                addresses = AddressResolution.NoActiveNetwork,
            )

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val networkTypes = NETWORK_TYPE_NAMES.filter { (transport, _) -> capabilities?.hasTransport(transport) == true }
            .values
            .toList()
        val isVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        val addresses = when {
            host.isEmpty() -> AddressResolution.Failed
            else -> runCatching { activeNetwork.getAllByName(host) }
                .fold(
                    onSuccess = { resolved -> AddressResolution.Resolved(resolved.mapNotNull { it.toResolvedAddress() }) },
                    onFailure = { error ->
                        if (error is IOException) {
                            appLogger.w("Could not resolve backend host through the active network", error)
                            AddressResolution.Failed
                        } else {
                            throw error
                        }
                    }
                )
        }

        return NetworkDiagnostics(
            isVpn = isVpn,
            networkTypes = networkTypes,
            backendHost = host,
            addresses = addresses,
        )
    }

    private companion object {
        val NETWORK_TYPE_NAMES = linkedMapOf(
            NetworkCapabilities.TRANSPORT_WIFI to "WIFI",
            NetworkCapabilities.TRANSPORT_CELLULAR to "CELLULAR",
            NetworkCapabilities.TRANSPORT_ETHERNET to "ETHERNET",
            NetworkCapabilities.TRANSPORT_BLUETOOTH to "BLUETOOTH",
            NetworkCapabilities.TRANSPORT_WIFI_AWARE to "WIFI_AWARE",
        )
    }
}

data class NetworkDiagnostics(
    val isVpn: Boolean,
    val networkTypes: List<String>,
    val backendHost: String,
    val addresses: AddressResolution,
)

sealed interface AddressResolution {
    data class Resolved(val addresses: List<ResolvedAddress>) : AddressResolution
    data object NoActiveNetwork : AddressResolution
    data object Failed : AddressResolution
}

data class ResolvedAddress(
    val address: String,
    val version: IpVersion,
)

enum class IpVersion { V4, V6 }

private fun InetAddress.toResolvedAddress(): ResolvedAddress? = hostAddress?.let { address ->
    ResolvedAddress(
        address = address,
        version = if (this is Inet6Address) IpVersion.V6 else IpVersion.V4,
    )
}
