package com.wire.android.feature.auth.registration.personal.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.NotFound
import com.wire.android.core.functional.Either
import com.wire.android.feature.auth.activation.ActivationRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class ActivateEmailUseCaseTest : UnitTest() {

    @MockK
    private lateinit var activationRepository: ActivationRepository

    private lateinit var activateEmailUseCase: ActivateEmailUseCase

    @Before
    fun setUp() {
        activateEmailUseCase = ActivateEmailUseCase(activationRepository)
    }

    @Test
    fun `given email and code params, when run is called, then calls repository to activateEmail with correct params`() {
        coEvery { activationRepository.activateEmail(any(), any()) } returns Either.Right(Unit)

        runBlocking { activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE)) }

        coVerify(exactly = 1) { activationRepository.activateEmail(email = TEST_EMAIL, code = TEST_CODE) }
    }

    @Test
    fun `given run is called, when repository returns success, then return success`() {
        coEvery { activationRepository.activateEmail(any(), any()) } returns Either.Right(Unit)

        val response = runBlocking { activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE)) }

        response shouldSucceed {}
    }

    @Test
    fun `given run is called, when repository returns NotFound, then return InvalidEmailCode`() {
        coEvery { activationRepository.activateEmail(any(), any()) } returns Either.Left(NotFound)

        val response = runBlocking { activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE)) }

        response shouldFail { it shouldBe InvalidEmailCode }
    }

    @Test
    fun `given run is called, when repository returns general failure, then return that failure`() {
        val failure = mockk<Failure>()
        coEvery { activationRepository.activateEmail(any(), any()) } returns Either.Left(failure)

        val response = runBlocking { activateEmailUseCase.run(ActivateEmailParams(TEST_EMAIL, TEST_CODE)) }

        response shouldFail { it shouldBe failure }
    }

    companion object {
        private const val TEST_EMAIL = "test@wire.com"
        private const val TEST_CODE = "123456"
    }
}
