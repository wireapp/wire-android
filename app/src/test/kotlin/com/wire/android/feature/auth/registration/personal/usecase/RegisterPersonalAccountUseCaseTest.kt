package com.wire.android.feature.auth.registration.personal.usecase

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.NotFound
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.feature.auth.registration.personal.PersonalAccountRegistrationResult
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class RegisterPersonalAccountUseCaseTest : UnitTest() {

    @Mock
    private lateinit var registrationRepository: RegistrationRepository

    @Mock
    private lateinit var userRepository: UserRepository

    @Mock
    private lateinit var sessionRepository: SessionRepository

    @Mock
    private lateinit var registrationResult: PersonalAccountRegistrationResult

    @Mock
    private lateinit var user: User

    @Mock
    private lateinit var session: Session

    private lateinit var useCase: RegisterPersonalAccountUseCase

    @Before
    fun setUp() {
        useCase = RegisterPersonalAccountUseCase(registrationRepository, userRepository, sessionRepository)
    }

    @Test
    fun `given run is called, when registrationRepo, userRepo and sessionRepo all return success, then returns success`() {
        runBlocking {
            mockRegistrationResponse(Either.Right(registrationResult))
            `when`(registrationResult.user).thenReturn(user)
            `when`(registrationResult.refreshToken).thenReturn(TEST_REFRESH_TOKEN)
            `when`(userRepository.save(user)).thenReturn(Either.Right(Unit))
            `when`(sessionRepository.accessToken(TEST_REFRESH_TOKEN)).thenReturn(Either.Right(session))
            `when`(sessionRepository.save(session)).thenReturn(Either.Right(Unit))

            val result = useCase.run(params)

            verify(registrationRepository).registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)
            verify(userRepository).save(user)
            verify(sessionRepository).accessToken(TEST_REFRESH_TOKEN)
            verify(sessionRepository).save(session, true)
            result.assertRight()
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns Forbidden, then directly returns UnauthorizedEmail`() {
        runBlocking {
            mockRegistrationResponse(Either.Left(Forbidden))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(UnauthorizedEmail)
            }
            verifyNoInteractions(userRepository)
            verifyNoInteractions(sessionRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns NotFound, then directly returns InvalidEmailActivationCode`() {
        runBlocking {
            mockRegistrationResponse(Either.Left(NotFound))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidEmailActivationCode)
            }
            verifyNoInteractions(userRepository)
            verifyNoInteractions(sessionRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns Conflict, then directly returns EmailInUse`() {
        runBlocking {
            mockRegistrationResponse(Either.Left(Conflict))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(EmailInUse)
            }
            verifyNoInteractions(userRepository)
            verifyNoInteractions(sessionRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns any other failure, then directly returns that failure`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            mockRegistrationResponse(Either.Left(failure))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verifyNoInteractions(userRepository)
            verifyNoInteractions(sessionRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns invalid user, then returns UserInfoMissing failure`() {
        runBlocking {
            `when`(registrationResult.user).thenReturn(User.EMPTY)
            mockRegistrationResponse(Either.Right(registrationResult))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(UserInfoMissing)
            }
        }
        verifyNoInteractions(userRepository)
        verifyNoInteractions(sessionRepository)
    }

    @Test
    fun `given run is called, when registrationRepository returns invalid refresh token, then returns RefreshTokenMissing failure`() {
        runBlocking {
            `when`(registrationResult.user).thenReturn(user)
            `when`(registrationResult.refreshToken).thenReturn(String.EMPTY)
            mockRegistrationResponse(Either.Right(registrationResult))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(RefreshTokenMissing)
            }
            verifyNoInteractions(userRepository)
            verifyNoInteractions(sessionRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns valid data but then userRepository fails, then returns that failure`() {
        runBlocking {
            `when`(registrationResult.user).thenReturn(user)
            `when`(registrationResult.refreshToken).thenReturn(TEST_REFRESH_TOKEN)
            mockRegistrationResponse(Either.Right(registrationResult))
            val failure = DatabaseFailure()
            `when`(userRepository.save(user)).thenReturn(Either.Left(failure))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verifyNoInteractions(sessionRepository)
        }
    }

    @Test
    fun `given run is called, when regRepo & userRepo are OK but sessionRepo cannot fetch token, then returns SessionCannotBeCreated`() {
        runBlocking {
            `when`(registrationResult.user).thenReturn(user)
            `when`(registrationResult.refreshToken).thenReturn(TEST_REFRESH_TOKEN)
            mockRegistrationResponse(Either.Right(registrationResult))
            `when`(userRepository.save(user)).thenReturn(Either.Right(Unit))

            val failure = mock(Failure::class.java)
            `when`(sessionRepository.accessToken(TEST_REFRESH_TOKEN)).thenReturn(Either.Left(failure))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(SessionCannotBeCreated)
            }
            verify(userRepository).save(user)
            verify(sessionRepository).accessToken(TEST_REFRESH_TOKEN)
            verify(sessionRepository, never()).save(any(), anyBoolean())
        }
    }

    @Test
    fun `given run is called, when registrationRepo & userRepo are successful but sessionRepo cannot save session, then returns failure`() {
        runBlocking {
            `when`(registrationResult.user).thenReturn(user)
            `when`(registrationResult.refreshToken).thenReturn(TEST_REFRESH_TOKEN)
            mockRegistrationResponse(Either.Right(registrationResult))
            `when`(userRepository.save(user)).thenReturn(Either.Right(Unit))

            `when`(sessionRepository.accessToken(TEST_REFRESH_TOKEN)).thenReturn(Either.Right(session))
            val failure = mock(Failure::class.java)
            `when`(sessionRepository.save(session)).thenReturn(Either.Left(failure))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verify(userRepository).save(user)
            verify(sessionRepository).accessToken(TEST_REFRESH_TOKEN)
            verify(sessionRepository).save(session, true)
        }
    }

    private suspend fun mockRegistrationResponse(response: Either<Failure, PersonalAccountRegistrationResult>) {
        `when`(registrationRepository.registerPersonalAccount(any(), any(), any(), any())).thenReturn(response)
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "email"
        private const val TEST_PASSWORD = "password"
        private const val TEST_ACTIVATION_CODE = "123456"
        private const val TEST_REFRESH_TOKEN = "refresh-token"

        private val params = RegisterPersonalAccountParams(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)
    }
}
