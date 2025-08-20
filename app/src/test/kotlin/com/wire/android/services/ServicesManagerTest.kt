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
import app.cash.turbine.test
import com.wire.android.config.TestDispatcherProvider
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesManagerTest {

    val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given ongoing call service running, when stop comes instantly after start, then start the service and stop it after a while`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.FOREGROUND)
                .arrange()
            // when
            servicesManager.startCallService()
            servicesManager.stopCallService()
            // then
            verify(exactly = 1) { arrangement.context.startService(arrangement.callServiceIntent) }
            advanceUntilIdle()
            verify(exactly = 1) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given call service running, when start comes instantly after stop, then do not stop the service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.FOREGROUND)
                .arrange()
            servicesManager.startCallService()
            // when
            servicesManager.stopCallService()
            servicesManager.startCallService()
            // then
            verify(exactly = 1) { arrangement.context.startService(arrangement.callServiceIntent) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given ongoing call, when stop comes some time after start, then start the service and stop it after that`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.FOREGROUND)
                .arrange()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.startCallService()
            advanceTimeBy((ServicesManager.DEBOUNCE_TIME + 50).milliseconds)
            servicesManager.stopCallService()
            // then
            verify(exactly = 1) { arrangement.context.startService(arrangement.callServiceIntent) }
            advanceUntilIdle()
            verify(exactly = 1) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given ongoing call service in foreground, when needs to be stopped, then call stopService`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.FOREGROUND)
                .arrange()
            servicesManager.startCallService()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.stopCallService()
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 1) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given ongoing call service not yet in foreground, when needs to be stopped, then call startService with stop service argument`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.STARTED)
                .arrange()
            servicesManager.startCallService()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.stopCallService()
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given ongoing call service not even started, when needs to be stopped, then do nothing`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.NOT_STARTED)
                .arrange()
            servicesManager.startCallService()
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.startCallService()
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given ongoing call service running, when start called again with the same action, then do not start the service again`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.FOREGROUND)
                .arrange()
            servicesManager.startCallService()
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.startCallService() // start again with the same action
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.callServiceIntent) } // not called again
        }

    @Test
    fun `given ongoing call service not started, when answering call, then start with proper action`() =
        runTest(dispatcherProvider.main()) {
            // given
            val userId = UserId("userId", "domain")
            val conversationId = ConversationId("conversationId", "domain")
            val action = CallService.Action.Start.AnswerCall(userId, conversationId)
            val intentForAction = mockk<Intent>()
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.NOT_STARTED)
                .callServiceIntentForAction(action, intentForAction)
                .arrange()
            advanceUntilIdle()
            // when
            servicesManager.startCallServiceToAnswer(userId, conversationId)
            // then
            verify(exactly = 1) { arrangement.context.startService(intentForAction) }
            verify(exactly = 0) { arrangement.context.startService(arrangement.callServiceIntent) }
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given ongoing call service already started, when answering different call, then start again with proper action`() =
        runTest(dispatcherProvider.main()) {
            // given
            val userId = UserId("userId", "domain")
            val conversationId = ConversationId("conversationId", "domain")
            val action = CallService.Action.Start.AnswerCall(userId, conversationId)
            val intentForAction = mockk<Intent>()
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.NOT_STARTED)
                .callServiceIntentForAction(action, intentForAction)
                .arrange()
            servicesManager.startCallService()
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.startCallServiceToAnswer(userId, conversationId)
            // then
            verify(exactly = 1) { arrangement.context.startService(intentForAction) }
            verify(exactly = 0) { arrangement.context.startService(arrangement.callServiceIntent) }
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given ongoing call service already started, when needs to answer the same call, then do not start again`() =
        runTest(dispatcherProvider.main()) {
            // given
            val userId = UserId("userId", "domain")
            val conversationId = ConversationId("conversationId", "domain")
            val action = CallService.Action.Start.AnswerCall(userId, conversationId)
            val intentForAction = mockk<Intent>()
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(CallService.ServiceState.NOT_STARTED)
                .callServiceIntentForAction(action, intentForAction)
                .arrange()
            servicesManager.startCallServiceToAnswer(userId, conversationId)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.startCallServiceToAnswer(userId, conversationId)
            // then
            verify(exactly = 0) { arrangement.context.startService(intentForAction) }
            verify(exactly = 0) { arrangement.context.startService(arrangement.callServiceIntent) }
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.callServiceIntent) }
        }

    @Test
    fun `given call service running, when call service is stopped by itself, then reset state by emitting stop`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement().arrange()
            servicesManager.callServiceEvents.emit(CallService.Action.Start.Default)
            arrangement.withServiceState(CallService.ServiceState.FOREGROUND)
            advanceUntilIdle()
            servicesManager.callServiceEvents.test {
                // when
                arrangement.withServiceState(CallService.ServiceState.NOT_STARTED)
                advanceUntilIdle()
                // then
                assertEquals(CallService.Action.Stop, awaitItem())
            }
        }

    private inner class Arrangement {

        @MockK(relaxed = true)
        lateinit var context: Context

        private val servicesManager: ServicesManager by lazy { ServicesManager(context, dispatcherProvider) }
        private val serviceStateFlow = MutableStateFlow(CallService.ServiceState.NOT_STARTED)

        @MockK
        lateinit var callServiceIntent: Intent

        @MockK
        lateinit var ongoingCallServiceIntentWithStopArgument: Intent

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkObject(CallService.Companion)
            every { CallService.Companion.serviceState } returns serviceStateFlow
            callServiceIntentForAction(CallService.Action.Start.Default, callServiceIntent)
            callServiceIntentForAction(CallService.Action.Stop, ongoingCallServiceIntentWithStopArgument)
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

        suspend fun withServiceState(state: CallService.ServiceState) = apply {
            serviceStateFlow.emit(state)
        }

        fun callServiceIntentForAction(action: CallService.Action, intent: Intent) = apply {
            every { CallService.Companion.newIntent(context, action) } returns intent
        }

        fun arrange() = this to servicesManager
    }
}
