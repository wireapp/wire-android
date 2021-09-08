package com.wire.android.feature.auth.client.datasource

import com.wire.android.UnitTest
import com.wire.android.core.crypto.CryptoBoxClient
import com.wire.android.core.crypto.model.PreKey
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.exception.NetworkFailure
import com.wire.android.core.exception.SQLiteFailure
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.datasource.local.ClientEntity
import com.wire.android.feature.auth.client.datasource.local.ClientLocalDataSource
import com.wire.android.feature.auth.client.datasource.remote.ClientRemoteDataSource
import com.wire.android.feature.auth.client.datasource.remote.api.ClientRegistrationRequest
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.feature.auth.client.datasource.remote.api.PreKeyRequest
import com.wire.android.feature.auth.client.datasource.remote.api.UpdatePreKeysRequest
import com.wire.android.feature.auth.client.mapper.ClientMapper
import com.wire.android.feature.auth.client.mapper.PreKeyMapper
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
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

    @MockK
    private lateinit var clientEntity: ClientEntity

    @MockK
    private lateinit var preKeyInitialization: PreKeyInitialization

    @MockK
    private lateinit var preKeyMapper: PreKeyMapper

    private lateinit var clientDataSource: ClientDataSource

    @Before
    fun setUp() {
        clientDataSource = ClientDataSource(cryptoBoxClient, clientRemoteDataSource, clientLocalDataSource, clientMapper, preKeyMapper)
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
    }

    @Test
    fun `given registerNewClient is called, when remote registration fails, then propagates failure`() {
        val failure = mockk<NetworkFailure>()
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { clientMapper.newRegistrationRequest(USER_ID, PASSWORD, preKeyInitialization) } returns clientRegistrationRequest
        coEvery { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) } returns Either.Left(failure)

        val result = runBlocking { clientDataSource.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) }
        coVerify(inverse = true) { clientLocalDataSource.save(clientEntity) }
    }


    @Test
    fun `given registerNewClient is called and remoteDataSource returns success, when client save fails, then returns Failure`() {
        val failure = mockk<SQLiteFailure>()
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { clientMapper.newRegistrationRequest(USER_ID, PASSWORD, preKeyInitialization) } returns clientRegistrationRequest
        coEvery {
            clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest)
        } returns Either.Right(clientResponse)
        every { clientMapper.fromClientResponseToClientEntity(clientResponse) } returns clientEntity
        coEvery { clientLocalDataSource.save(clientEntity) } returns Either.Left(failure)

        val result = runBlocking { clientDataSource.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }

        result shouldFail { it shouldBeEqualTo failure }

        coVerify(exactly = 1) { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) }
        coVerify(exactly = 1) { clientLocalDataSource.save(clientEntity) }
    }


    @Test
    fun `given registerNewClient is called and remoteDataSource returns success, when client is saved locally, then returns Unit`() {
        every { cryptoBoxClient.createInitialPreKeys() } returns Either.Right(preKeyInitialization)
        every { clientMapper.newRegistrationRequest(USER_ID, PASSWORD, preKeyInitialization) } returns clientRegistrationRequest
        coEvery {
            clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest)
        } returns Either.Right(clientResponse)
        every { clientMapper.fromClientResponseToClientEntity(clientResponse) } returns clientEntity
        coEvery { clientLocalDataSource.save(clientEntity) } returns Either.Right(Unit)

        val result = runBlocking { clientDataSource.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }

        result shouldSucceed { it shouldBeEqualTo clientResponse.id }
        coVerify(exactly = 1) { clientRemoteDataSource.registerNewClient(AUTHORIZATION_TOKEN, clientRegistrationRequest) }
        coVerify(exactly = 1) { clientLocalDataSource.save(clientEntity) }
    }

    @Test
    fun `given updatePreKeysIfNeeded is called, when there are no keys to update, then do not save new pre keys remotely`() {
        every { cryptoBoxClient.createNewPreKeysIfNeeded(any()) } returns Either.Right(listOf())
        every { preKeyMapper.toPreKeyRequest(any()) } returns mockk()
        coEvery { clientRemoteDataSource.remainingPreKeys(any(), any()) } returns Either.Right(listOf())

        runBlocking { clientDataSource.updatePreKeysIfNeeded(AUTHORIZATION_TOKEN, CLIENT_ID) }

        coVerify(inverse = true) { clientRemoteDataSource.saveNewPreKeys(any(), any(), any()) }
    }

    @Test
    fun `given updatePreKeysIfNeeded is called, when talking to cryptoBoxClient, then the last remote keys should be passed`() {
        val remainingPreKeysIds = mockk<List<Int>>()
        coEvery { clientRemoteDataSource.remainingPreKeys(any(), any()) } returns Either.Right(remainingPreKeysIds)
        every { preKeyMapper.toPreKeyRequest(any()) } returns mockk()
        every { cryptoBoxClient.createNewPreKeysIfNeeded(any()) } returns Either.Right(listOf())

        runBlocking { clientDataSource.updatePreKeysIfNeeded(AUTHORIZATION_TOKEN, CLIENT_ID) }

        verify { cryptoBoxClient.createNewPreKeysIfNeeded(remainingPreKeysIds) }
    }

    @Test
    fun `given updatePreKeysIfNeeded is called, when there are keys to update, then saveNewPreKeys is called on remote data source`() {
        coEvery { clientRemoteDataSource.remainingPreKeys(any(), any()) } returns Either.Right(mockk())
        every { preKeyMapper.toPreKeyRequest(any()) } returns mockk()
        every { cryptoBoxClient.createNewPreKeysIfNeeded(any()) } returns Either.Right(listOf(mockk()))
        coEvery { clientRemoteDataSource.saveNewPreKeys(any(), any(), any()) } returns mockk()

        runBlocking { clientDataSource.updatePreKeysIfNeeded(AUTHORIZATION_TOKEN, CLIENT_ID) }

        coVerify { clientRemoteDataSource.saveNewPreKeys(any(), any(), any()) }
    }

    @Test
    fun `given updatePreKeysIfNeeded is called, when pre keys need mapping, then the PreKey mapper is called with the created value`() {
        val createdPreKey: PreKey = mockk()
        every { cryptoBoxClient.createNewPreKeysIfNeeded(any()) } returns Either.Right(listOf(createdPreKey))
        coEvery { clientRemoteDataSource.remainingPreKeys(any(), any()) } returns Either.Right(mockk())
        every { preKeyMapper.toPreKeyRequest(any()) } returns mockk()
        coEvery { clientRemoteDataSource.saveNewPreKeys(any(), any(), any()) } returns mockk()

        runBlocking { clientDataSource.updatePreKeysIfNeeded(AUTHORIZATION_TOKEN, CLIENT_ID) }

        coVerify(exactly = 1) { preKeyMapper.toPreKeyRequest(createdPreKey) }
    }

    @Test
    fun `given updatePreKeysIfNeeded is called, when forwarding to remote data source, then the result of the mapper is returned`() {
        val mappedKeyRequest: PreKeyRequest = mockk()
        every { preKeyMapper.toPreKeyRequest(any()) } returns mappedKeyRequest
        coEvery { clientRemoteDataSource.remainingPreKeys(any(), any()) } returns Either.Right(mockk())
        every { cryptoBoxClient.createNewPreKeysIfNeeded(any()) } returns Either.Right(listOf(mockk()))
        coEvery { clientRemoteDataSource.saveNewPreKeys(any(), any(), any()) } returns mockk()

        runBlocking { clientDataSource.updatePreKeysIfNeeded(AUTHORIZATION_TOKEN, CLIENT_ID) }

        coVerify(exactly = 1) {
            clientRemoteDataSource.saveNewPreKeys(
                AUTHORIZATION_TOKEN,
                CLIENT_ID,
                UpdatePreKeysRequest(listOf(mappedKeyRequest))
            )
        }
    }

    companion object {
        private const val USER_ID = "user-id"
        private const val CLIENT_ID = "client-id"
        private const val PASSWORD = "password"
        private const val AUTHORIZATION_TOKEN = "authorization-token"
    }
}
