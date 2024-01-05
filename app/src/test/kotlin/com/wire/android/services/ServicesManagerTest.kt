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
package com.wire.android.services

import android.content.Context
import android.content.Intent
import com.wire.android.config.TestDispatcherProvider
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesManagerTest {

    val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given ongoing call service running, when stop comes instantly after start, then do not even start the service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .arrange()
            // when
            servicesManager.startOngoingCallService()
            advanceTimeBy((ServicesManager.DEBOUNCE_TIME - 50).milliseconds)
            servicesManager.stopOngoingCallService()
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntent) }
            verify(exactly = 1) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing call, when stop comes some time after start, then start the service and stop it after that`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .arrange()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.startOngoingCallService()
            advanceTimeBy((ServicesManager.DEBOUNCE_TIME + 50).milliseconds)
            servicesManager.stopOngoingCallService()
            // then
            verify(exactly = 1) { arrangement.context.startService(arrangement.ongoingCallServiceIntent) }
            verify(exactly = 1) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing call service in foreground, when needs to be stopped, then call stopService`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .arrange()
            servicesManager.startOngoingCallService()
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.stopOngoingCallService()
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 1) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing call service not yet in foreground, when needs to be stopped, then call startService with stop service argument`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.STARTED)
                .arrange()
            servicesManager.startOngoingCallService()
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.stopOngoingCallService()
            // then
            verify(exactly = 1) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing call service not even started, when needs to be stopped, then do nothing`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .arrange()
            servicesManager.startOngoingCallService()
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.startOngoingCallService()
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    private inner class Arrangement {

        @MockK(relaxed = true)
        lateinit var context: Context

        private val servicesManager: ServicesManager by lazy { ServicesManager(context, dispatcherProvider) }

        @MockK
        lateinit var ongoingCallServiceIntent: Intent

        @MockK
        lateinit var ongoingCallServiceIntentWithStopArgument: Intent

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkObject(OngoingCallService.Companion)
            every { OngoingCallService.Companion.newIntent(context) } returns ongoingCallServiceIntent
            every { OngoingCallService.Companion.newIntentToStop(context) } returns ongoingCallServiceIntentWithStopArgument
        }

        fun clearRecordedCallsForContext() {
            clearMocks(
                context,
                answers = false,
                recordedCalls = true,
                childMocks = false,
                verificationMarks = false,
                exclusionRules = false
            )
        }

        fun withServiceState(state: OngoingCallService.ServiceState) = apply {
            every { OngoingCallService.Companion.serviceState.get() } returns state
            every { OngoingCallService.serviceState.get() } returns state
        }

        fun arrange() = this to servicesManager
    }
}
