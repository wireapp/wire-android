package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientApi
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.framework.network.mockNetworkError
import com.wire.android.framework.network.mockNetworkResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ClientRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var clientApi: ClientApi

    @MockK
    private lateinit var clientRegistrationRequest: ClientRegistrationRequest

    private lateinit var clientRemoteDataSource: ClientRemoteDataSource

    @Before
    fun setUp() {
        clientRemoteDataSource = ClientRemoteDataSource(connectedNetworkHandler, clientApi)
    }

    @Test
    fun `given registerNewClient is called, when api response is successful, then return success`() {
        coEvery { clientApi.registerClient(authorizationToken, clientRegistrationRequest) } returns mockNetworkResponse()

        val result = runBlocking { clientRemoteDataSource.registerNewClient(authorizationToken, clientRegistrationRequest) }

        coVerify(exactly = 1) { clientApi.registerClient(authorizationToken, clientRegistrationRequest) }
        result shouldSucceed {}
    }

    @Test
    fun `given registerNewClient is called, when api response fails, then return a failure`() {
        coEvery { clientApi.registerClient(authorizationToken, clientRegistrationRequest) } returns mockNetworkError()

        val result = runBlocking { clientRemoteDataSource.registerNewClient(authorizationToken, clientRegistrationRequest) }

        coVerify(exactly = 1) { clientApi.registerClient(authorizationToken, clientRegistrationRequest) }
        result shouldFail {}
    }

    companion object {
        private const val authorizationToken = "authorizationHeader-key"
    }
}
