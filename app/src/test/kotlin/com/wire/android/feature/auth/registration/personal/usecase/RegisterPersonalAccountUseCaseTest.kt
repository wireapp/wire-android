package com.wire.android.feature.auth.registration.personal.usecase

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.DatabaseFailure
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.activeusers.ActiveUsersRepository
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
    private lateinit var activeUsersRepository: ActiveUsersRepository

    private lateinit var useCase: RegisterPersonalAccountUseCase

    @Before
    fun setUp() {
        useCase = RegisterPersonalAccountUseCase(registrationRepository, activeUsersRepository)
    }

    @Test
    fun `given run is called, when registrationRepository and activeUsersRepository return success, then returns success`() {
        runBlocking {
            mockRepositoryResponse(Either.Right(TEST_USER_ID))
            `when`(activeUsersRepository.saveActiveUser(TEST_USER_ID)).thenReturn(Either.Right(Unit))

            val result = useCase.run(params)

            verify(registrationRepository).registerPersonalAccount(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)
            verify(activeUsersRepository).saveActiveUser(TEST_USER_ID)
            result.assertRight()
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns Forbidden, then directly returns UnauthorizedEmail`() {
        runBlocking {
            mockRepositoryResponse(Either.Left(Forbidden))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(UnauthorizedEmail)
            }
            verifyNoInteractions(activeUsersRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns NotFound, then directly returns InvalidEmailActivationCode`() {
        runBlocking {
            mockRepositoryResponse(Either.Left(NotFound))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidEmailActivationCode)
            }
            verifyNoInteractions(activeUsersRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns Conflict, then directly returns EmailInUse`() {
        runBlocking {
            mockRepositoryResponse(Either.Left(Conflict))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(EmailInUse)
            }
            verifyNoInteractions(activeUsersRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns any other failure, then directly returns that failure`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            mockRepositoryResponse(Either.Left(failure))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
            verifyNoInteractions(activeUsersRepository)
        }
    }

    @Test
    fun `given run is called, when registrationRepository returns success but activeUsersRepository fails, then returns that failure`() {
        runBlocking {
            mockRepositoryResponse(Either.Right(TEST_USER_ID))
            val failure = DatabaseFailure()
            `when`(activeUsersRepository.saveActiveUser(TEST_USER_ID)).thenReturn(Either.Left(failure))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
        }
    }

    private fun mockRepositoryResponse(response: Either<Failure, String>) {
        runBlocking {
            `when`(registrationRepository.registerPersonalAccount(any(), any(), any(), any())).thenReturn(response)
        }
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "email"
        private const val TEST_PASSWORD = "password"
        private const val TEST_ACTIVATION_CODE = "123456"
        private const val TEST_USER_ID = "2345-sdlfk-234"
        private val params = RegisterPersonalAccountParams(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)
    }
}
