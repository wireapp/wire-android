package com.wire.android.feature.auth.activation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Conflict
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.Forbidden
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class SendEmailActivationCodeUseCaseTest : UnitTest() {

    private lateinit var sendEmailActivationCodeUseCase: SendEmailActivationCodeUseCase

    @MockK
    private lateinit var activationRepository: ActivationRepository

    @MockK
    private lateinit var sendEmailActivationCodeParams: SendEmailActivationCodeParams

    @Before
    fun setup() {
        sendEmailActivationCodeUseCase = SendEmailActivationCodeUseCase(activationRepository)
        every { sendEmailActivationCodeParams.email } returns TEST_EMAIL
    }

    @Test
    fun `given use case is run, when repository returns Forbidden error, then return EmailBlackListed`() {
        coEvery { activationRepository.sendEmailActivationCode(TEST_EMAIL) } returns Either.Left(Forbidden)

        runBlocking {
            val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

            coVerify { activationRepository.sendEmailActivationCode(TEST_EMAIL) }
            response shouldFail { it shouldBe EmailBlacklisted }
        }
    }

    @Test
    fun `given use case is run, when repository returns Conflict error, then return EmailInUse`() {
        coEvery { activationRepository.sendEmailActivationCode(TEST_EMAIL) } returns Either.Left(Conflict)

        runBlocking {
            val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

            coVerify { activationRepository.sendEmailActivationCode(TEST_EMAIL) }
            response shouldFail { it shouldBe EmailInUse }
        }
    }

    @Test
    fun `given use case is run, when repository returns any other error, then return this error`() {
        val mockFailure = mockk<Failure>()
        coEvery { activationRepository.sendEmailActivationCode(TEST_EMAIL) } returns Either.Left(mockFailure)

        runBlocking {
            val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

            coVerify { activationRepository.sendEmailActivationCode(TEST_EMAIL) }
            response shouldFail { it shouldBe mockFailure }
        }
    }

    @Test
    fun `given send email activation code use case is executed, when there is no error then return success`() {
        coEvery { activationRepository.sendEmailActivationCode(TEST_EMAIL) } returns Either.Right(Unit)

        runBlocking {
            val response = sendEmailActivationCodeUseCase.run(sendEmailActivationCodeParams)

            coVerify { activationRepository.sendEmailActivationCode(TEST_EMAIL) }
            response shouldSucceed  { it shouldBe Unit }
        }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire"
    }
}
