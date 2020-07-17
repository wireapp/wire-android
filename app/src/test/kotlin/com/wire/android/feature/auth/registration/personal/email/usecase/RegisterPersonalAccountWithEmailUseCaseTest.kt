package com.wire.android.feature.auth.registration.personal.email.usecase

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.registration.RegistrationRepository
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class RegisterPersonalAccountWithEmailUseCaseTest : UnitTest() {

    @Mock
    private lateinit var repository: RegistrationRepository

    private lateinit var useCase: RegisterPersonalAccountWithEmailUseCase

    @Before
    fun setUp() {
        useCase = RegisterPersonalAccountWithEmailUseCase(repository)
    }

    @Test
    fun `given params, when run is called, then calls repository with correct params`() {
        runBlocking {
            mockRepositoryResponse(Either.Right(Unit))
            useCase.run(params)

            verify(repository).registerPersonalAccountWithEmail(
                name = TEST_NAME, email = TEST_EMAIL, password = TEST_PASSWORD, activationCode = TEST_ACTIVATION_CODE
            )
        }
    }

    @Test
    fun `given run is called, when repository returns Forbidden, then returns UnauthorizedEmail`() {
        runBlocking {
            mockRepositoryResponse(Either.Left(Forbidden))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(UnauthorizedEmail)
            }
        }
    }

    @Test
    fun `given run is called, when repository returns NotFound, then returns InvalidEmailActivationCode`() {
        runBlocking {
            mockRepositoryResponse(Either.Left(NotFound))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(InvalidEmailActivationCode)
            }
        }
    }

    @Test
    fun `given run is called, when repository returns Conflict, then returns EmailInUse`() {
        runBlocking {
            mockRepositoryResponse(Either.Left(Conflict))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(EmailInUse)
            }
        }
    }

    @Test
    fun `given run is called, when repository returns any other failure, then returns that failure`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            mockRepositoryResponse(Either.Left(failure))

            val result = useCase.run(params)

            result.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
        }
    }

    @Test
    fun `given run is called, when repository returns success, then returns success`() {
        runBlocking {
            mockRepositoryResponse(Either.Right(Unit))

            val result = useCase.run(params)

            result.assertRight()
        }
    }

    private fun mockRepositoryResponse(response: Either<Failure, Unit>) {
        runBlocking {
            `when`(repository.registerPersonalAccountWithEmail(any(), any(), any(), any())).thenReturn(response)
        }
    }

    companion object {
        private const val TEST_NAME = "name"
        private const val TEST_EMAIL = "email"
        private const val TEST_PASSWORD = "password"
        private const val TEST_ACTIVATION_CODE = "123456"
        private val params = EmailRegistrationParams(TEST_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ACTIVATION_CODE)
    }
}
