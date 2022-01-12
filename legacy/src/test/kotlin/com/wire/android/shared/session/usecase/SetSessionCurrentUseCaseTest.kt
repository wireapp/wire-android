package com.wire.android.shared.session.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import com.wire.android.shared.session.SessionRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.junit.Before
import org.junit.Test

class SetSessionCurrentUseCaseTest : UnitTest() {

    @MockK
    private lateinit var sessionRepository: SessionRepository

    private lateinit var SetSessionCurrentUseCase: SetSessionCurrentUseCase


    @Before
    fun setUp() {
        SetSessionCurrentUseCase = SetSessionCurrentUseCase(sessionRepository)
    }

    @Test
    fun `given run is called, when sessionRepository calls successfully the method, then return success`() {
        val userId = "user-id"
        val params = SetSessionCurrentUseCaseParams(userId)
        coEvery { sessionRepository.setSessionCurrent(userId) } returns Either.Right(Unit)

        val result = runBlocking { SetSessionCurrentUseCase.run(params) }

        result shouldSucceed { it shouldBe Unit }
    }

    @Test
    fun `given run is called, when sessionRepository fails to call the method, then propagates the failure`() {
        val userId = "user-id"
        val failure = mockk<Failure>()
        val params = SetSessionCurrentUseCaseParams(userId)

        coEvery { sessionRepository.setSessionCurrent(userId) } returns Either.Left(failure)

        val result = runBlocking { SetSessionCurrentUseCase.run(params) }

        result shouldFail { it shouldBe failure }
    }
}
