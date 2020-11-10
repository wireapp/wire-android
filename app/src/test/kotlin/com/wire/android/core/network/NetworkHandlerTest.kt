package com.wire.android.core.network

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.wire.android.UnitTest
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class NetworkHandlerTest : UnitTest() {

    @MockK
    private lateinit var connectivityManager: ConnectivityManager

    @MockK
    private lateinit var network: Network

    @MockK
    private lateinit var networkCapabilities: NetworkCapabilities

    private lateinit var networkHandler: NetworkHandler

    @Before
    fun setup() {
        networkHandler = NetworkHandler(connectivityManager)
    }

    @Test
    fun `given connectivityManager, when active network is not available, then isConnected should be false`() {
        every { connectivityManager.activeNetwork } returns null
        networkHandler.isConnected() shouldBe false
    }

    @Test
    fun `given connectivityManager, when network capabilities is not available, then isConnected should be false`() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null
        networkHandler.isConnected() shouldBe false
    }

    @Test
    fun `given connectivityManager, when network capabilities has wifi, then isConnected should be true`() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true

        networkHandler.isConnected() shouldBe true
    }

    @Test
    fun `given connectivityManager, when network capabilities has mobile data, then isConnected should be true`() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true

        networkHandler.isConnected() shouldBe true
    }

    @Test
    fun `given connectivityManager, when network capabilities does not have wifi or mobile data, then isConnected should be false`() {
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
        every { networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false

        networkHandler.isConnected() shouldBe false
    }
}
