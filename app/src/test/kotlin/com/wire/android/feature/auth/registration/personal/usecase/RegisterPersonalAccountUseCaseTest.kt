package com.wire.android.feature.auth.registration.personal.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.feature.auth.registration.personal.PersonalAccountRegistrationResult
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class RegisterPersonalAccountUseCaseTest : UnitTest() {

    @MockK
    private lateinit var registrationRepository: RegistrationRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var sessionRepository: SessionRepository

    @MockK
    private lateinit var registrationResult: PersonalAccountRegistrationResult

    @MockK
    private lateinit var user: User

    @MockK
    private lateinit var session: Session

    private lateinit var useCase: RegisterPersonalAccountUseCase

    @Before
    fun setUp() {
        useCase = RegisterPersonalAccountUseCase(registrationRepository, userRepository, sessionRepository)
    }

    @Test
    fun `given run is called, when registrationRepo, userRepo and sessionRepo all return success, then returns success`() {
        mockRegistrationResponse(Either.Right(registrationResult))
        every { registrationResult.user } returns user
        every { registrationResult.refreshToken } returns TEST_REFRESH_TOKEN
        coEvery { userRepository.save(user) } returns Either.Right(Unit)
        coEvery { sessionRepository.newAccessToken(TEST_REFRESH_TOKEN) } returns Either.Right(session)
        coEvery { sessionRepository.save(session) } returns Either.Right(Unit)

        val result = runBlocking { useCase.run(params) }

        coVerify(exactly = 1) {
            registrationRepository.registerPersonalAccount(
                name = TEST_NAME,
                email = TEST_EMAIL,
                username = TEST_USERNAME,
                password = TEST_PASSWORD,
                activationCode = TEST_ACTIVATION_CODE
            )
        }
        coVerify(exactly = 1) { userRepository.save(user) }
        coVerify(exactly = 1) { sessionRepository.newAccessToken(TEST_REFRESH_TOKEN) }
        coVerify(exactly = 1) { sessionRepository.save(session, true) }
        result shouldSucceed { it shouldBe Unit }
    }

    @Test
    fun `given run is called, when registrationRepository returns Forbidden, then directly returns UnauthorizedEmail`() {
        mockRegistrationResponse(Either.Left(Forbidden))

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe UnauthorizedEmail }
        verify { userRepository wasNot Called }
        verify { sessionRepository wasNot Called }
    }

    @Test
    fun `given run is called, when registrationRepository returns NotFound, then directly returns InvalidEmailActivationCode`() {
        mockRegistrationResponse(Either.Left(NotFound))

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe InvalidEmailActivationCode }
        verify { userRepository wasNot Called }
        verify { sessionRepository wasNot Called }
    }

    @Test
    fun `given run is called, when registrationRepository returns Conflict, then directly returns EmailInUse`() {
        mockRegistrationResponse(Either.Left(Conflict))

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe EmailInUse }
        verify { userRepository wasNot Called }
        verify { sessionRepository wasNot Called }
    }

    @Test
    fun `given run is called, when registrationRepository returns any other failure, then directly returns that failure`() {
        val failure = mockk<Failure>()
        mockRegistrationResponse(Either.Left(failure))

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe failure }
        verify { userRepository wasNot Called }
        verify { sessionRepository wasNot Called }
    }

    @Test
    fun `given run is called, when registrationRepository returns null user, then returns UserInfoMissing failure`() {
        every { registrationResult.user } returns null
        mockRegistrationResponse(Either.Right(registrationResult))

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe UserInfoMissing }
        verify { userRepository wasNot Called }
        verify { sessionRepository wasNot Called }
    }

    @Test
    fun `given run is called, when registrationRepository returns null refresh token, then returns RefreshTokenMissing failure`() {
        every { registrationResult.user } returns user
        every { registrationResult.refreshToken } returns null
        mockRegistrationResponse(Either.Right(registrationResult))

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe RefreshTokenMissing }
        verify { userRepository wasNot Called }
        verify { sessionRepository wasNot Called }
    }

    @Test
    fun `given run is called, when registrationRepository returns valid data but then userRepository fails, then returns that failure`() {
        every { registrationResult.user } returns user
        every { registrationResult.refreshToken } returns TEST_REFRESH_TOKEN
        mockRegistrationResponse(Either.Right(registrationResult))
        val failure = DatabaseFailure()
        coEvery { userRepository.save(user) } returns Either.Left(failure)

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe failure }
        verify { sessionRepository wasNot Called }
    }

    @Test
    fun `given run is called, when regRepo & userRepo are OK but sessionRepo cannot fetch token, then returns SessionCannotBeCreated`() {
        every { registrationResult.user } returns user
        every { registrationResult.refreshToken } returns TEST_REFRESH_TOKEN
        mockRegistrationResponse(Either.Right(registrationResult))
        coEvery { userRepository.save(user) } returns Either.Right(Unit)
        val failure = mockk<Failure>()
        coEvery { sessionRepository.newAccessToken(TEST_REFRESH_TOKEN) } returns Either.Left(failure)

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe SessionCannotBeCreated }
        coVerify(exactly = 1) { userRepository.save(user) }
        coVerify(exactly = 1) { sessionRepository.newAccessToken(TEST_REFRESH_TOKEN) }
        coVerify(inverse = true) { sessionRepository.save(any(), any()) }
    }

    @Test
    fun `given run is called, when registrationRepo & userRepo are successful but sessionRepo cannot save session, then returns failure`() {
        every { registrationResult.user } returns user
        every { registrationResult.refreshToken } returns TEST_REFRESH_TOKEN
        mockRegistrationResponse(Either.Right(registrationResult))
        coEvery { userRepository.save(user) } returns Either.Right(Unit)
        coEvery { sessionRepository.newAccessToken(TEST_REFRESH_TOKEN) } returns Either.Right(session)
        val failure = mockk<Failure>()
        coEvery { sessionRepository.save(session) } returns Either.Left(failure)

        val result = runBlocking { useCase.run(params) }

        result shouldFail { it shouldBe failure }
        coVerify(exactly = 1) { userRepository.save(user) }
        coVerify(exactly = 1) { sessionRepository.newAccessToken(TEST_REFRESH_TOKEN) }
        coVerify(exactly = 1) { sessionRepository.save(session, true) }
    }

    private fun mockRegistrationResponse(response: Either<Failure, PersonalAccountRegistrationResult>) {
        coEvery { registrationRepository.registerPersonalAccount(any(), any(), any(), any(), any()) } returns response
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "email"
        private const val TEST_USERNAME = "username"
        private const val TEST_PASSWORD = "password"
        private const val TEST_ACTIVATION_CODE = "123456"
        private const val TEST_REFRESH_TOKEN = "refresh-token"

        private val params = RegisterPersonalAccountParams(TEST_NAME, TEST_EMAIL, TEST_USERNAME, TEST_PASSWORD, TEST_ACTIVATION_CODE)
    }
}
