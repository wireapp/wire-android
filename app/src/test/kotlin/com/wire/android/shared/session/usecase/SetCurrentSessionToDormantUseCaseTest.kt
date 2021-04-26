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

class SetCurrentSessionToDormantUseCaseTest : UnitTest() {

    @MockK
    private lateinit var sessionRepository: SessionRepository

    private lateinit var setCurrentSessionToDormantUseCase: SetCurrentSessionToDormantUseCase


    @Before
    fun setUp() {
        setCurrentSessionToDormantUseCase = SetCurrentSessionToDormantUseCase(sessionRepository)
    }

    @Test
    fun `given run is called, when sessionRepository call successfully the method, then return success`() {
        coEvery { sessionRepository.setCurrentSessionToDormant() } returns Either.Right(Unit)

        val result = runBlocking { setCurrentSessionToDormantUseCase.run(Unit) }

        result shouldSucceed { it shouldBe Unit }
    }

    @Test
    fun `given run is called, when sessionRepository fail to call the method, then propagates the failure`() {
        val failure = mockk<Failure>()

        coEvery { sessionRepository.setCurrentSessionToDormant() } returns Either.Left(failure)

        val result = runBlocking { setCurrentSessionToDormantUseCase.run(Unit) }

        result shouldFail { it shouldBe failure }
    }
}
