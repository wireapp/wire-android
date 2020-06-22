package com.wire.android.framework.network

import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import org.mockito.Mockito

/**
 * A [NetworkHandler] mock that has always "connected" status. This can be used for convenience in [ApiService] subclass tests.
 */
val connectedNetworkHandler: NetworkHandler
    get() = Mockito.mock(NetworkHandler::class.java).apply {
        Mockito.`when`(this.isConnected).thenReturn(true)
    }