package com.wire.android.framework.network

import com.wire.android.core.network.ApiService
import com.wire.android.core.network.NetworkHandler
import io.mockk.every
import io.mockk.mockk

/**
 * A [NetworkHandler] mock that has always "connected" status. This can be used for convenience in [ApiService] subclass tests.
 */
val connectedNetworkHandler: NetworkHandler
    get() = mockk<NetworkHandler>().also {
        every { it.isConnected } returns true
    }
