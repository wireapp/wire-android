package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientApi
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.UpdatePreKeysRequest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.framework.network.mockNetworkError
import com.wire.android.framework.network.mockNetworkResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
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

    @Test
    fun `given remainingPreKeys is called, when api response succeeds, then return the successful result`() {
        val remainingPreKeysResponse = mockk<List<Int>>()

        coEvery { clientApi.remainingPreKeys(authorizationToken, CLIENT_ID) } returns mockNetworkResponse(remainingPreKeysResponse)

        runBlocking { clientRemoteDataSource.remainingPreKeys(authorizationToken, CLIENT_ID) }
            .shouldSucceed { it shouldBeEqualTo remainingPreKeysResponse }
    }

    @Test
    fun `given remainingPreKeys is called, when api response fails, then return a failure`() {
        coEvery { clientApi.remainingPreKeys(authorizationToken, CLIENT_ID) } returns mockNetworkError()

        runBlocking { clientRemoteDataSource.remainingPreKeys(authorizationToken, CLIENT_ID) }
            .shouldFail {}
    }

    @Test
    fun `given remainingPreKeys is called, when requesting from remote data source, then correct parameters should be used`() {
        coEvery { clientApi.remainingPreKeys(authorizationToken, CLIENT_ID) } returns mockNetworkResponse()

        runBlocking { clientRemoteDataSource.remainingPreKeys(authorizationToken, CLIENT_ID) }

        coVerify(exactly = 1) { clientApi.remainingPreKeys(authorizationToken, CLIENT_ID) }
    }

    @Test
    fun `given saveNewPreKeys is called, when api response succeeds, then return success`() {
        coEvery { clientApi.updatePreKeys(any(), any(), any()) } returns mockNetworkResponse()

        runBlocking { clientRemoteDataSource.saveNewPreKeys(authorizationToken, CLIENT_ID, mockk()) }
            .shouldSucceed {}
    }

    @Test
    fun `given saveNewPreKeys is called, when requesting update from remote data source, then the correct parameters should be used`() {
        coEvery { clientApi.updatePreKeys(any(), any(), any()) } returns mockNetworkResponse()

        val requestedBody = mockk<UpdatePreKeysRequest>()
        runBlocking { clientRemoteDataSource.saveNewPreKeys(authorizationToken, CLIENT_ID, requestedBody) }

        coVerify(exactly = 1) { clientApi.updatePreKeys(authorizationToken, CLIENT_ID, requestedBody) }
    }

    @Test
    fun `given saveNewPreKeys is called, when api response fails, then return a failure`() {
        coEvery { clientApi.updatePreKeys(any(), any(), any()) } returns mockNetworkError()

        runBlocking { clientRemoteDataSource.saveNewPreKeys(authorizationToken, CLIENT_ID, mockk()) }
            .shouldFail()
    }

    companion object {
        private const val authorizationToken = "authorizationHeader-key"
        private const val CLIENT_ID = "clientID"
    }
}
