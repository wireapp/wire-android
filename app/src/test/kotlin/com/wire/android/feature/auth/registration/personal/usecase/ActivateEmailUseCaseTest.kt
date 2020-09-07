package com.wire.android.feature.auth.registration.personal.usecase

import com.wire.android.UnitTest
import com.wire.android.any
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*

class ActivateEmailUseCaseTest : UnitTest() {

    @Mock
    private lateinit var activationRepository: ActivationRepository

    private lateinit var activateEmailUseCase: ActivateEmailUseCase

    @Before
    fun setUp() {
        activateEmailUseCase = ActivateEmailUseCase(activationRepository)
    }

    @Test
    fun `given email and code params, when run is called, then calls repository to activateEmail with correct params`() {
        runBlocking {
            `when`(activationRepository.activateEmail(any(), any())).thenReturn(Either.Right(Unit))

            activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE))

            verify(activationRepository).activateEmail(email = TEST_EMAIL, code = TEST_CODE)
        }
    }

    @Test
    fun `given run is called, when repository returns success, then return success`() {
        runBlocking {
            `when`(activationRepository.activateEmail(any(), any())).thenReturn(Either.Right(Unit))

            val response = activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE))

            response.assertRight()
        }
    }

    @Test
    fun `given run is called, when repository returns NotFound, then return InvalidEmailCode`() {
        runBlocking {
            `when`(activationRepository.activateEmail(any(), any())).thenReturn(Either.Left(NotFound))

            val response = activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE))

            response.assertLeft {
                assertThat(it).isEqualTo(InvalidEmailCode)
            }
        }
    }

    @Test
    fun `given run is called, when repository returns general failure, then return that failure`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            `when`(activationRepository.activateEmail(any(), any())).thenReturn(Either.Left(failure))

            val response = activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE))

            response.assertLeft {
                assertThat(it).isEqualTo(failure)
            }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_CODE = "123456"
    }
}
