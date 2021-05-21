package com.wire.android.feature.auth.client.datasource

import com.wire.android.UnitTest
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.exception.NetworkFailure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.datasource.local.ClientLocalDataSource
import com.wire.android.feature.auth.client.datasource.remote.ClientRemoteDataSource
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.mapper.ClientMapper
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.every
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ClientDataSourceTest : UnitTest() {

    @MockK
    private lateinit var cryptoBoxClient: CryptoBoxClient

    @MockK
    private lateinit var clientRemoteDataSource: ClientRemoteDataSource

    @MockK
    private lateinit var clientLocalDataSource: ClientLocalDataSource

    @MockK
    private lateinit var clientMapper: ClientMapper

    @MockK
    private lateinit var clientRegistrationRequest: ClientRegistrationRequest
    @MockK
    private lateinit var clientResponse: ClientResponse

    private lateinit var clientDataSource: ClientDataSource

    @Before
    fun setUp() {
        clientDataSource = ClientDataSource(cryptoBoxClient, clientRemoteDataSource, clientLocalDataSource, clientMapper)
    }

    @Test
    fun `given registerNewClient is called, when remote registration is successfully done, then return client response`() {
        val client = mockk<Client>()

        val preKey = mockk<PreKey>()
        val createdPreKeys = mockk<List<PreKey>>()
        val preKeyInitialization = mockk<PreKeyInitialization>().also {
            every { it.lastKey } returns preKey
            every { it.createdKeys } returns createdPreKeys
        }

        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { clientMapper.newClient(USER_ID, PASSWORD, preKeyInitialization) } returns client
        every { clientMapper.toClientRegistrationRequest(client) } returns clientRegistrationRequest
        every { clientMapper.fromClientResponseToClient(clientResponse) } returns client
        coEvery {
            clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest)
        } returns Either.Right(clientResponse)

        val result = runBlocking {
            clientDataSource.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD)
        }

        result shouldSucceed { it shouldBeEqualTo client }
        coVerify(exactly = 1) { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) }
        coVerify(exactly = 1) { clientMapper.fromClientResponseToClient(clientResponse) }
    }

    @Test
    fun `given registerNewClient is called, when preKeys generation fails, then propagates cryptobox failure`() {
        val failure = mockk<CryptoBoxFailure>()
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Left(failure)

        val result = runBlocking {
            clientDataSource.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD)
        }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(inverse = true) { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) }
        coVerify(inverse = true) { clientMapper.fromClientResponseToClient(clientResponse) }
    }

    @Test
    fun `given registerNewClient is called, when remote registration fails, then propagates failure`() {
        val failure = mockk<NetworkFailure>()
        val client = mockk<Client>()
        val clientRegistrationRequest = mockk<ClientRegistrationRequest>()
        val preKeyInitialization = mockk<PreKeyInitialization>()
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { clientMapper.newClient(USER_ID, PASSWORD, preKeyInitialization) } returns client
        every { clientMapper.toClientRegistrationRequest(client) } returns clientRegistrationRequest
        coEvery { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) } returns Either.Left(failure)

        val result = runBlocking { clientDataSource.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }

        result shouldFail  { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) }
        coVerify(inverse = true) { clientMapper.fromClientResponseToClient(clientResponse) }
    }

    companion object {
        private const val USER_ID = "user-id"
        private const val PASSWORD = "password"
        private const val AUTHORIZATION_TOKEN = "authorization-token"
    }
}
