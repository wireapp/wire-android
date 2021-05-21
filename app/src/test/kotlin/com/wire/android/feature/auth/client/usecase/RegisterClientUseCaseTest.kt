package com.wire.android.feature.auth.client.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.BadRequest
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.core.network.auth.accesstoken.AuthenticationManager
import com.wire.android.feature.auth.client.Client
import com.wire.android.feature.auth.client.ClientRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
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
class RegisterClientUseCaseTest : UnitTest() {

    private lateinit var registerClientUseCase: RegisterClientUseCase

    @MockK
    private lateinit var clientRepository: ClientRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var registerClientParams: RegisterClientParams

    @MockK
    private lateinit var session: Session

    @MockK
    private lateinit var client: Client


    @MockK
    private lateinit var authenticationManager: AuthenticationManager

    @Before
    fun setUp() {
        registerClientUseCase = RegisterClientUseCase(clientRepository, sessionRepository, authenticationManager)
        every { registerClientParams.userId } returns USER_ID
        every { registerClientParams.password } returns PASSWORD
    }

    @Test
    fun `given use case is run, when session repository returns failure, then propagate failure`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.userSession(USER_ID) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.userSession(USER_ID) }
        coVerify(inverse = true) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }
    }

    @Test
    fun `given repository returns session, when registerNewClient returns Failure, then propagate Failure`() {
        val failure = mockk<Failure>()

        coEvery { sessionRepository.userSession(USER_ID) } returns Either.Right(session)
        coEvery { authenticationManager.authorizationToken(session) } returns AUTHORIZATION_TOKEN
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo failure}
        coVerify(exactly = 1) { sessionRepository.userSession(USER_ID) }
        coVerify(exactly = 1) { authenticationManager.authorizationToken(session) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }
        coVerify(inverse = true) { clientRepository.saveLocally(client) }
    }

    @Test
    fun `given repository returns session, when registerNewClient returns BadRequest, then propagate MalformedPreKeys`() {
        val failure = mockk<BadRequest>()
        coEvery { sessionRepository.userSession(USER_ID) } returns Either.Right(session)
        coEvery { authenticationManager.authorizationToken(session) } returns AUTHORIZATION_TOKEN
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo MalformedPreKeys}
        coVerify(exactly = 1) { sessionRepository.userSession(USER_ID) }
        coVerify(exactly = 1) { authenticationManager.authorizationToken(session) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }
        coVerify(inverse = true) { clientRepository.saveLocally(client) }
    }

    @Test
    fun `given repository returns session, when registerNewClient returns Forbidden, then propagate DevicesLimitReached`() {
        val failure = mockk<Forbidden>()
        coEvery { sessionRepository.userSession(USER_ID) } returns Either.Right(session)
        coEvery { authenticationManager.authorizationToken(session) } returns AUTHORIZATION_TOKEN
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail { it shouldBeEqualTo DevicesLimitReached}
        coVerify(exactly = 1) { sessionRepository.userSession(USER_ID) }
        coVerify(exactly = 1) { authenticationManager.authorizationToken(session) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }
        coVerify(inverse = true) { clientRepository.saveLocally(client) }
    }


    @Test
    fun `given registerNewClient returns client, when saveLocally runs successfully, then return Unit`() {
        coEvery { sessionRepository.userSession(USER_ID) } returns Either.Right(session)
        coEvery { authenticationManager.authorizationToken(session) } returns AUTHORIZATION_TOKEN
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) } returns Either.Right(client)
        coEvery { clientRepository.saveLocally(client) } returns Either.Right(Unit)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldSucceed  { it shouldBeEqualTo Unit}
        coVerify(exactly = 1) { sessionRepository.userSession(USER_ID) }
        coVerify(exactly = 1) { authenticationManager.authorizationToken(session) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }
        coVerify(exactly = 1) { clientRepository.saveLocally(client) }
    }

    @Test
    fun `given registerNewClient returns client, when saveLocally fails to save client locally, then propagate failure`() {
        val failure = DatabaseFailure()
        coEvery { sessionRepository.userSession(USER_ID) } returns Either.Right(session)
        coEvery { authenticationManager.authorizationToken(session) } returns AUTHORIZATION_TOKEN
        coEvery { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) } returns Either.Right(client)
        coEvery { clientRepository.saveLocally(client) } returns Either.Left(failure)

        val response = runBlocking { registerClientUseCase.run(registerClientParams)}

        response shouldFail   { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { sessionRepository.userSession(USER_ID) }
        coVerify(exactly = 1) { authenticationManager.authorizationToken(session) }
        coVerify(exactly = 1) { clientRepository.registerNewClient(AUTHORIZATION_TOKEN, USER_ID, PASSWORD) }
        coVerify(exactly = 1) { clientRepository.saveLocally(client) }
    }

    companion object {
        private const val AUTHORIZATION_TOKEN = "authorizationToken"
        private const val USER_ID = "user-id"
        private const val PASSWORD = "password"
    }
}
