package com.wire.android.feature.auth.activation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
class SendEmailActivationCodeUseCaseTest : UnitTest() {

    private lateinit var sendEmailActivationCodeUseCase: SendEmailActivationCodeUseCase

    @Mock
    private lateinit var activationRepository: ActivationRepository

    @Mock
    private lateinit var sendEmailActivationCodeParams: SendEmailActivationCodeParams

    @Before
    fun setup() {
        sendEmailActivationCodeUseCase = SendEmailActivationCodeUseCase(activationRepository)
        `when`(sendEmailActivationCodeParams.email).thenReturn(TEST_EMAIL)
    }

    @Test
    fun `given use case is run, when repository returns Forbidden error, then return EmailBlackListed`() = runBlocking {
        `when`(activationRepository.sendEmailActivationCode(TEST_EMAIL)).thenReturn(Either.Left(Forbidden))

        val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

        verify(activationRepository).sendEmailActivationCode(TEST_EMAIL)
        response.assertLeft {
            assertThat(it).isEqualTo(EmailBlacklisted)
        }
    }

    @Test
    fun `given use case is run, when repository returns Conflict error, then return EmailInUse`() = runBlocking {
        `when`(activationRepository.sendEmailActivationCode(TEST_EMAIL)).thenReturn(Either.Left(Conflict))

        val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

        verify(activationRepository).sendEmailActivationCode(TEST_EMAIL)
        response.assertLeft {
            assertThat(it).isEqualTo(EmailInUse)
        }
    }

    @Test
    fun `given use case is run, when repository returns any other error, then return this error`() = runBlocking {
        val mockFailure = mock(Failure::class.java)
        `when`(activationRepository.sendEmailActivationCode(TEST_EMAIL)).thenReturn(Either.Left(mockFailure))

        val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

        verify(activationRepository).sendEmailActivationCode(TEST_EMAIL)
        response.assertLeft {
            assertThat(it).isEqualTo(mockFailure)
        }
    }

    @Test
    fun `given send email activation code use case is executed, when there is no error then return success`() = runBlocking {
        `when`(activationRepository.sendEmailActivationCode(TEST_EMAIL)).thenReturn(Either.Right(Unit))

        val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

        verify(activationRepository).sendEmailActivationCode(TEST_EMAIL)
        response.assertRight {
            assertThat(it).isEqualTo(Unit)
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire"
    }

}
