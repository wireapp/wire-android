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

class CheckCurrentSessionExistsUseCaseTest : UnitTest() {

    @MockK
    private lateinit var sessionRepository: SessionRepository

    private lateinit var checkCurrentSessionExistsUseCase: CheckCurrentSessionExistsUseCase

    @Before
    fun setUp() {
        checkCurrentSessionExistsUseCase = CheckCurrentSessionExistsUseCase(sessionRepository)
    }

    @Test
    fun `given run is called, when sessionRepository successfully returns true, then propagates the result`() =
        testRunWithSuccessfulRepositoryResponse(true)

    @Test
    fun `given run is called, when sessionRepository successfully returns false, then propagates the result`() =
        testRunWithSuccessfulRepositoryResponse(false)

    private fun testRunWithSuccessfulRepositoryResponse(exists: Boolean) {
        coEvery { sessionRepository.doesCurrentSessionExist() } returns Either.Right(exists)

        runBlocking {
            checkCurrentSessionExistsUseCase.run(Unit) shouldSucceed { it shouldBe exists }
        }
    }

    @Test
    fun `given run is called, when sessionRepository fails, then propagates the failure`() {
        val failure = mockk<Failure>()
        coEvery { sessionRepository.doesCurrentSessionExist() } returns Either.Left(failure)

        runBlocking {
            checkCurrentSessionExistsUseCase.run(Unit) shouldFail { it shouldBe failure }
        }
    }
}
