package com.wire.android.feature.auth.client.datasource.remote

import com.wire.android.UnitTest
import com.wire.android.core.exception.NetworkFailure
import com.wire.android.core.exception.NotFound
import com.wire.android.feature.auth.client.datasource.remote.api.ClientApi
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientsOfUsersResponse
import com.wire.android.feature.auth.client.datasource.remote.api.UpdatePreKeysRequest
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.framework.network.connectedNetworkHandler
import com.wire.android.framework.network.mockNetworkError
import com.wire.android.framework.network.mockNetworkResponse
import com.wire.android.shared.user.QualifiedId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ClientRemoteDataSourceTest : UnitTest() {

    @MockK
    private lateinit var clientApi: ClientApi

    @MockK
    private lateinit var clientRegistrationRequest: ClientRegistrationRequest

    @MockK
    private lateinit var clientRemoteMapper: ClientRemoteMapper

    private lateinit var clientRemoteDataSource: ClientRemoteDataSource

    @Before
    fun setUp() {
        clientRemoteDataSource = ClientRemoteDataSource(connectedNetworkHandler, clientApi, clientRemoteMapper)
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
    fun `given remainingPreKeys is called, when requesting from remote data source, then correct auth tokens and client ids are passed`() {
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
    fun `given saveNewPreKeys is called, when requesting update from remote data source, then the correct parameters should be passed`() {
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

    @Test
    fun `given clientAPI fails, when getting clients of users, then failure should be forwarded`() {
        coEvery { clientApi.clientsOfUsers(any(), any()) } returns mockNetworkError(404)

        runBlocking {
            clientRemoteDataSource.clientIdsOfUsers("", listOf())
        }.shouldFail { it shouldBeEqualTo NotFound }
    }

    @Test
    fun `given clientAPI a returns successfully, when getting clients of users, then the remote mapper should map the response`() {
        val apiResponse = mockk<ClientsOfUsersResponse>()
        coEvery { clientApi.clientsOfUsers(any(), any()) } returns mockNetworkResponse(apiResponse)

        runBlocking {
            clientRemoteDataSource.clientIdsOfUsers("", listOf())
        }

        verify(exactly = 1) { clientRemoteMapper.fromClientsOfUsersResponseToMapOfQualifiedClientIds(apiResponse) }
    }

    @Test
    fun `given clientAPI a returns successfully, when getting clients of users, then the mapped result should be returned`() {
        val apiResponse = mockk<ClientsOfUsersResponse>()
        val mappedResult = mockk<Map<QualifiedId, List<String>>>()
        coEvery { clientApi.clientsOfUsers(any(), any()) } returns mockNetworkResponse(apiResponse)
        every { clientRemoteMapper.fromClientsOfUsersResponseToMapOfQualifiedClientIds(apiResponse) } returns mappedResult

        val result = runBlocking {
            clientRemoteDataSource.clientIdsOfUsers("", listOf())
        }

        result shouldBeEqualTo mappedResult
    }

    companion object {
        private const val authorizationToken = "authorizationHeader-key"
        private const val CLIENT_ID = "clientID"
    }
}
