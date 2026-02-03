/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class IsWebSocketConnectionUnhealthyUseCaseTest {

    @Test
    fun givenObservePersistentWebSocketStatusReturnsFailure_whenInvoking_thenShouldReturnFailure() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusFailure()
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isFailure)
        }

    @Test
    fun givenFlowTimesOut_whenInvoking_thenShouldReturnFailure() =
        runTest {
            // given
            val sharedFlow = MutableSharedFlow<List<PersistentWebSocketStatus>>()
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(sharedFlow)
                .arrange()
            // when
            val result = useCase.invoke()
            advanceTimeBy(IsWebSocketConnectionUnhealthyUseCase.TIMEOUT + 1000L)
            // then
            assertTrue(result.isFailure)
        }

    @Test
    fun givenNoUsersWithPersistentWebSocketEnabled_whenInvoking_thenShouldReturnSuccessFalse() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(listOf(PersistentWebSocketStatus(userId, false)))
                )
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    @Test
    fun givenEmptyUserList_whenInvoking_thenShouldReturnSuccessFalse() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(flowOf(emptyList()))
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    @Test
    fun givenUserWithPersistentWebSocketAndNoEventsYet_whenInvoking_thenShouldReturnSuccessFalse() =
        runTest {
            // given
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(listOf(PersistentWebSocketStatus(userId, true)))
                )
                .withLastWebSocketEventInstant(userId, null)
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    @Test
    fun givenUserWithPersistentWebSocketAndRecentEvent_whenInvoking_thenShouldReturnSuccessFalse() =
        runTest {
            // given
            val recentInstant = Clock.System.now() - 1.hours
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(listOf(PersistentWebSocketStatus(userId, true)))
                )
                .withLastWebSocketEventInstant(userId, recentInstant)
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    @Test
    fun givenUserWithPersistentWebSocketAndEventExactlyAtThreshold_whenInvoking_thenShouldReturnSuccessFalse() =
        runTest {
            // given
            // Add 1 minute buffer to account for time elapsed between test setup and use case execution
            val thresholdInstant = Clock.System.now() - IsWebSocketConnectionUnhealthyUseCase.UNHEALTHY_THRESHOLD + 1.minutes
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(listOf(PersistentWebSocketStatus(userId, true)))
                )
                .withLastWebSocketEventInstant(userId, thresholdInstant)
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    @Test
    fun givenUserWithPersistentWebSocketAndOldEvent_whenInvoking_thenShouldReturnSuccessTrue() =
        runTest {
            // given
            val oldInstant = Clock.System.now() - 13.hours
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(listOf(PersistentWebSocketStatus(userId, true)))
                )
                .withLastWebSocketEventInstant(userId, oldInstant)
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull())
        }

    @Test
    fun givenMultipleUsersOneWithOldEvent_whenInvoking_thenShouldReturnSuccessTrue() =
        runTest {
            // given
            val recentInstant = Clock.System.now() - 1.hours
            val oldInstant = Clock.System.now() - 13.hours
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(
                        listOf(
                            PersistentWebSocketStatus(userId, true),
                            PersistentWebSocketStatus(userId2, true)
                        )
                    )
                )
                .withLastWebSocketEventInstant(userId, recentInstant)
                .withLastWebSocketEventInstant(userId2, oldInstant)
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(true, result.getOrNull())
        }

    @Test
    fun givenMultipleUsersAllWithRecentEvents_whenInvoking_thenShouldReturnSuccessFalse() =
        runTest {
            // given
            val recentInstant1 = Clock.System.now() - 1.hours
            val recentInstant2 = Clock.System.now() - 6.hours
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(
                        listOf(
                            PersistentWebSocketStatus(userId, true),
                            PersistentWebSocketStatus(userId2, true)
                        )
                    )
                )
                .withLastWebSocketEventInstant(userId, recentInstant1)
                .withLastWebSocketEventInstant(userId2, recentInstant2)
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    @Test
    fun givenMixOfEnabledAndDisabledUsersWithOldEventOnlyForDisabled_whenInvoking_thenShouldReturnSuccessFalse() =
        runTest {
            // given
            val recentInstant = Clock.System.now() - 1.hours
            val oldInstant = Clock.System.now() - 13.hours
            val (_, useCase) = Arrangement()
                .withObservePersistentWebSocketConnectionStatusSuccess(
                    flowOf(
                        listOf(
                            PersistentWebSocketStatus(userId, true),
                            PersistentWebSocketStatus(userId2, false)
                        )
                    )
                )
                .withLastWebSocketEventInstant(userId, recentInstant)
                .withLastWebSocketEventInstant(userId2, oldInstant)
                .arrange()
            // when
            val result = useCase.invoke()
            // then
            assertTrue(result.isSuccess)
            assertEquals(false, result.getOrNull())
        }

    inner class Arrangement {

        @MockK
        private lateinit var coreLogic: CoreLogic

        val useCase by lazy {
            IsWebSocketConnectionUnhealthyUseCase(coreLogic)
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

        fun withLastWebSocketEventInstant(userId: UserId, instant: Instant?) = apply {
            every { coreLogic.getSessionScope(userId).getLastWebSocketEventInstant() } returns instant
        }
    }

    companion object {
        private val userId = UserId("userId", "domain")
        private val userId2 = UserId("userId2", "domain")
    }
}
