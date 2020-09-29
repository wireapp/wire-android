package com.wire.android.shared.session.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.framework.functional.assertLeft
import com.wire.android.framework.functional.assertRight
import com.wire.android.shared.session.SessionRepository
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class HasCurrentSessionUseCaseTest : UnitTest() {

    @Mock
    private lateinit var sessionRepository: SessionRepository

    private lateinit var hasCurrentSessionUseCase: HasCurrentSessionUseCase

    @Before
    fun setUp() {
        hasCurrentSessionUseCase = HasCurrentSessionUseCase(sessionRepository)
    }

    @Test
    fun `given run is called, when sessionRepository successfully returns true, then propagates the result`() {
        testRunWithSuccessfulRepositoryResponse(true)
    }

    @Test
    fun `given run is called, when sessionRepository successfully returns false, then propagates the result`() {
        testRunWithSuccessfulRepositoryResponse(false)
    }

    private fun testRunWithSuccessfulRepositoryResponse(hasSession: Boolean) = runBlocking {
        `when`(sessionRepository.hasCurrentSession()).thenReturn(Either.Right(hasSession))

        hasCurrentSessionUseCase.run(Unit).assertRight {
            assertThat(it).isEqualTo(hasSession)
        }
    }

    @Test
    fun `given run is called, when sessionRepository fails, then propagates the failure`() {
        runBlocking {
            val failure = mock(Failure::class.java)
            `when`(sessionRepository.hasCurrentSession()).thenReturn(Either.Left(failure))

            hasCurrentSessionUseCase.run(Unit).assertLeft {
                assertThat(it).isEqualTo(failure)
            }
        }
    }
}
