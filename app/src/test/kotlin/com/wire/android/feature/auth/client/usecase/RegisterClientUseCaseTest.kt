package com.wire.android.feature.auth.client.usecase

import com.wire.android.UnitTest
import com.wire.android.core.crypto.model.PreKeyInitialization
import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.CryptoBoxFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.feature.auth.client.datasource.remote.api.ClientResponse
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.crypto.CryptoBoxRepository
import com.wire.android.shared.session.SessionRepository
import io.mockk.CapturingSlot
import io.mockk.slot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class RegisterClientUseCaseTest : UnitTest(){

    private lateinit var registerClientUseCase: RegisterClientUseCase

    @MockK
    private lateinit var localClientGeneratorUseCase: LocalClientGeneratorUseCase

    @MockK
    private lateinit var clientRepository: ClientRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var cryptoBoxRepository: CryptoBoxRepository

    @MockK
    private lateinit var client: Client

    @MockK
    private lateinit var registerClientParams: RegisterClientParams

    @MockK
    private lateinit var localClientGeneratorParams: LocalClientGeneratorParams

    @MockK
    private lateinit var preKeyInitialization : PreKeyInitialization

    private lateinit var localClientGeneratorParamsCaptor: CapturingSlot<LocalClientGeneratorParams>

    @Before
    fun setUp(){
        localClientGeneratorParamsCaptor = slot()
        registerClientUseCase = RegisterClientUseCase(clientRepository, sessionRepository, cryptoBoxRepository, localClientGeneratorUseCase)
        every { registerClientParams.userId } returns USER_ID
    }

    @Test
    fun `given use case is run, when session repository returns failure, then propagate failure`(){
        val failure = mockk<Failure>()
        coEvery { sessionRepository.userAuthorizationToken(USER_ID) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.userAuthorizationToken(USER_ID) }
        coVerify(inverse = true) { cryptoBoxRepository.generatePreKeys() }
        coVerify(inverse = true) { localClientGeneratorUseCase.run(localClientGeneratorParams) }
        coVerify(inverse = true) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) }
    }

    @Test
    fun `given session repository returns token, when generatePreKeyUseCase returns failure, then propagate failure`(){
        val failure = mockk<CryptoBoxFailure>()

        coEvery { sessionRepository.userAuthorizationToken(USER_ID) } returns Either.Right(AUTHORIZATION_TOKEN)
        coEvery { cryptoBoxRepository.generatePreKeys() } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.userAuthorizationToken(USER_ID) }
        coVerify(exactly = 1) { cryptoBoxRepository.generatePreKeys() }
        coVerify(inverse = true) { localClientGeneratorUseCase.run(localClientGeneratorParams) }
        coVerify(inverse = true) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) }
    }

    @Test
    fun `given generatePreKeyUseCase returns preKey, when localClientGeneratorUseCase runs failure, then propagate failure`(){
        val failure = mockk<CryptoBoxFailure>()

        coEvery { sessionRepository.userAuthorizationToken(USER_ID) } returns Either.Right(AUTHORIZATION_TOKEN)
        coEvery { cryptoBoxRepository.generatePreKeys() } returns Either.Right(preKeyInitialization)
        coEvery { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.userAuthorizationToken(USER_ID) }
        coVerify(exactly = 1) { cryptoBoxRepository.generatePreKeys() }
        coVerify(exactly = 1) { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) }
        coVerify(inverse = true) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) }
    }

    @Test
    fun `given localClientGeneratorUseCase returns client, when registerNewClient returns Failure, then propagate Failure`(){
        val failure = mockk<Failure>()

        coEvery { sessionRepository.userAuthorizationToken(USER_ID) } returns Either.Right(AUTHORIZATION_TOKEN)
        coEvery { cryptoBoxRepository.generatePreKeys() } returns Either.Right(preKeyInitialization)
        coEvery { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) } returns Either.Right(client)
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.userAuthorizationToken(USER_ID) }
        coVerify(exactly = 1) { cryptoBoxRepository.generatePreKeys() }
        coVerify(exactly = 1) { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) }
    }

    @Test
    fun `given localClientGeneratorUseCase returns client, when registerNewClient returns BadRequest, then propagate MalformedPreKeys`(){
        val failure = mockk<BadRequest>()

        coEvery { sessionRepository.userAuthorizationToken(USER_ID) } returns Either.Right(AUTHORIZATION_TOKEN)
        coEvery { cryptoBoxRepository.generatePreKeys() } returns Either.Right(preKeyInitialization)
        coEvery { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) } returns Either.Right(client)
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo MalformedPreKeys}
        coVerify(exactly = 1) { sessionRepository.userAuthorizationToken(USER_ID) }
        coVerify(exactly = 1) { cryptoBoxRepository.generatePreKeys() }
        coVerify(exactly = 1) { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) }
    }

    @Test
    fun `given localClientGeneratorUseCase returns client, when registerNewClient returns Forbidden, then propagate DevicesLimitReached`(){
        val failure = mockk<Forbidden>()

        coEvery { sessionRepository.userAuthorizationToken(USER_ID) } returns Either.Right(AUTHORIZATION_TOKEN)
        coEvery { cryptoBoxRepository.generatePreKeys() } returns Either.Right(preKeyInitialization)
        coEvery { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) } returns Either.Right(client)
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo DevicesLimitReached}
        coVerify(exactly = 1) { sessionRepository.userAuthorizationToken(USER_ID) }
        coVerify(exactly = 1) { cryptoBoxRepository.generatePreKeys() }
        coVerify(exactly = 1) { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) }
    }

    @Test
    fun `given use case is run, when registerNewClient runs successfully, then return valid client response`() {
        val clientResponse = mockk<ClientResponse>()
        coEvery { sessionRepository.userAuthorizationToken(USER_ID) } returns Either.Right(AUTHORIZATION_TOKEN)
        coEvery { cryptoBoxRepository.generatePreKeys() } returns Either.Right(preKeyInitialization)
        coEvery { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) } returns Either.Right(client)
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) } returns Either.Right(clientResponse)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldSucceed  { it shouldBeEqualTo clientResponse}

        coVerify(exactly = 1) { sessionRepository.userAuthorizationToken(USER_ID) }
        coVerify(exactly = 1) { cryptoBoxRepository.generatePreKeys() }
        coVerify(exactly = 1) { localClientGeneratorUseCase.run(capture(localClientGeneratorParamsCaptor)) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, client) }
    }

    companion object {
        private const val AUTHORIZATION_TOKEN = "authorizationToken"
        private const val USER_ID = "user-id"
        private const val PASSWORD = "password"
    }
}
