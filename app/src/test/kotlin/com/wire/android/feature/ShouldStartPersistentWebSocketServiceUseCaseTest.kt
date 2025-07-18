/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.feature

import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.PersistentWebSocketStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ShouldStartPersistentWebSocketServiceUseCaseTest {

    @Test
    fun givenObservePersistentWebSocketStatusReturnsSuccessAndThereAreUsersWithPersistentFlagOn_whenInvoking_shouldReturnSuccessTrue() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(flowOf(listOf(PersistentWebSocketStatus(userId, true))))
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertInstanceOf(ShouldStartPersistentWebSocketServiceUseCase.Result.Success::class.java, result).also {
                assertEquals(true, it.shouldStartPersistentWebSocketService)
            }
        }

    @Test
    fun givenObservePersistentWebSocketStatusReturnsSuccessAndThereAreNoUsersWithPersistentFlagOn_whenInvoking_shouldReturnSuccessFalse() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(flowOf(listOf(PersistentWebSocketStatus(userId, false))))
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertInstanceOf(ShouldStartPersistentWebSocketServiceUseCase.Result.Success::class.java, result).also {
                assertEquals(false, it.shouldStartPersistentWebSocketService)
            }
        }

    @Test
    fun givenObservePersistentWebSocketStatusReturnsSuccessAndThereAreNoUsers_whenInvoking_shouldReturnSuccessFalse() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(flowOf(emptyList()))
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertInstanceOf(ShouldStartPersistentWebSocketServiceUseCase.Result.Success::class.java, result).also {
                assertEquals(false, it.shouldStartPersistentWebSocketService)
            }
        }

    @Test
    fun givenObservePersistentWebSocketStatusReturnsSuccessAndTheFlowIsEmpty_whenInvoking_shouldReturnSuccessFalse() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(emptyFlow())
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertInstanceOf(ShouldStartPersistentWebSocketServiceUseCase.Result.Success::class.java, result).also {
                assertEquals(false, it.shouldStartPersistentWebSocketService)
            }
        }

    @Test
    fun givenObservePersistentWebSocketStatusReturnsSuccessAndFlowTimesOut_whenInvoking_shouldReturnSuccessFalse() =
        runTest {
            // given
            val sharedFlow = MutableSharedFlow<List<PersistentWebSocketStatus>>() // shared flow doesn't close so we can test the timeout
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(sharedFlow)
                .arrange()
            // when
            val result = useCase.invoke()
            advanceTimeBy(ShouldStartPersistentWebSocketServiceUseCase.TIMEOUT + 1000L)
            // then
            assertInstanceOf(ShouldStartPersistentWebSocketServiceUseCase.Result.Success::class.java, result).also {
                assertEquals(false, it.shouldStartPersistentWebSocketService)
            }
        }

    @Test
    fun givenObservePersistentWebSocketStatusReturnsFailure_whenInvoking_shouldReturnFailure() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusFailure()
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertInstanceOf(ShouldStartPersistentWebSocketServiceUseCase.Result.Failure::class.java, result)
        }

    inner class Arrangement {

        @MockK
        private lateinit var coreLogic: CoreLogic

        val useCase by lazy {
            ShouldStartPersistentWebSocketServiceUseCase(coreLogic)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun arrange() = this to useCase

        fun withObservePersistentWebSocketConnectionStatusSuccess(flow: Flow<List<PersistentWebSocketStatus>>) = apply {
            coEvery { coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(flow)
        }
        fun withObservePersistentWebSocketConnectionStatusFailure() = apply {
            coEvery { coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Failure.StorageFailure
        }
    }

    companion object {
        private val userId = UserId("userId", "domain")
    }
}
