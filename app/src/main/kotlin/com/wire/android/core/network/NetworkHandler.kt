package com.wire.android.core.network

import android.content.Context

/**
 * Class which returns information about the network connection state.
 */
class NetworkHandler(private val context: Context) {
    val isConnected get() = true //TODO: check new api for connectivity
}
