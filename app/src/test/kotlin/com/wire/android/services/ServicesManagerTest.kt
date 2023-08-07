/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class ServicesManagerTest {

    val dispatcherProvider = TestDispatcherProvider()

    // OngoingCallService-related tests
    @Test
    fun `given no ongoing calls, then stop the ongoing call service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given an ongoing call for one user, then start the ongoing call service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.context.startService(arrangement.ongoingCallServiceIntent) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing calls for two users, when one call ends, then keep the ongoing call service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.handleOngoingCall(TEST_USER_ID1, null)
            // then
            verify(exactly = 0) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing calls for two users, when both call ends, then stop the ongoing call service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.handleOngoingCall(TEST_USER_ID1, null)
            servicesManager.handleOngoingCall(TEST_USER_ID2, null)
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntent) }
            verify(exactly = 1) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing calls for two users, when one of them becomes invalid, then keep the ongoing call service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            arrangement.withValidAccountsUpdate(listOf(TEST_SELF_USER2))
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing calls for two users, when both of them becomes invalid, then stop the ongoing call service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            arrangement.withValidAccountsUpdate(listOf())
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing call, when end call comes instantly after start, then do not even start the ongoing call service`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            // when
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            advanceTimeBy((ServicesManager.DEBOUNCE_TIME - 50).milliseconds)
            servicesManager.handleOngoingCall(TEST_USER_ID1, null)
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntent) }
            verify(exactly = 1) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    @Test
    fun `given ongoing call, when end call comes some time after start, then start the ongoing call service and stop it after that`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.FOREGROUND)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            advanceTimeBy((ServicesManager.DEBOUNCE_TIME + 50).milliseconds)
            servicesManager.handleOngoingCall(TEST_USER_ID1, null)
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
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.handleOngoingCall(TEST_USER_ID1, null)
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
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.handleOngoingCall(TEST_USER_ID1, null)
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
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            // when
            servicesManager.handleOngoingCall(TEST_USER_ID1, null)
            // then
            verify(exactly = 0) { arrangement.context.startService(arrangement.ongoingCallServiceIntentWithStopArgument) }
            verify(exactly = 0) { arrangement.context.stopService(arrangement.ongoingCallServiceIntent) }
        }

    // currentlyOngoingCall-related tests

    @Test
    fun `given no ongoing calls, then currentlyOngoingCall emits null`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            // then
            servicesManager.currentlyOngoingCall().test {
                advanceUntilIdle()
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun `given an ongoing call for one user, then currentlyOngoingCall emits that call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            arrangement.clearRecordedCallsForContext() // clear calls recorded when initializing the state
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            // then
            servicesManager.currentlyOngoingCall().test {
                advanceUntilIdle()
                assertEquals(TEST_ONGOING_CALL_DATA1, awaitItem())
            }
        }

    @Test
    fun `given ongoing calls for two users, then currentlyOngoingCall emits the last call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            // then
            servicesManager.currentlyOngoingCall().test {
                advanceUntilIdle()
                assertEquals(TEST_ONGOING_CALL_DATA2, awaitItem())
            }
        }

    @Test
    fun `given ongoing calls for two users, when one call ends, then currentlyOngoingCall emits the remaining call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            // when-then
            servicesManager.currentlyOngoingCall().test {
                servicesManager.handleOngoingCall(TEST_USER_ID1, null)
                advanceUntilIdle()
                assertEquals(TEST_ONGOING_CALL_DATA2, awaitItem())
            }
        }

    @Test
    fun `given ongoing calls for two users, when both call ends, then currentlyOngoingCall emits null`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            // when-then
            servicesManager.currentlyOngoingCall().test {
                servicesManager.handleOngoingCall(TEST_USER_ID1, null)
                servicesManager.handleOngoingCall(TEST_USER_ID2, null)
                advanceUntilIdle()
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun `given ongoing calls for two users, when one of them becomes invalid, then currentlyOngoingCall emits call for remaining user`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            // when-then
            servicesManager.currentlyOngoingCall().test {
                arrangement.withValidAccountsUpdate(listOf(TEST_SELF_USER2))
                advanceUntilIdle()
                assertEquals(TEST_ONGOING_CALL_DATA2, awaitItem())
            }
        }

    @Test
    fun `given ongoing calls for two users, when both of them becomes invalid, then currentlyOngoingCall emits null`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1, TEST_SELF_USER2))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            servicesManager.handleOngoingCall(TEST_USER_ID2, TEST_ONGOING_CALL_DATA2)
            // when-then
            servicesManager.currentlyOngoingCall().test {
                arrangement.withValidAccountsUpdate(listOf())
                advanceUntilIdle()
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun `given ongoing call, when end call comes instantly after start, then currentlyOngoingCall omits that call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            // when-then
            servicesManager.currentlyOngoingCall().test {
                advanceTimeBy((ServicesManager.DEBOUNCE_TIME - 50).milliseconds)
                servicesManager.handleOngoingCall(TEST_USER_ID1, null)
                advanceUntilIdle()
                assertEquals(null, awaitItem())
            }
        }

    @Test
    fun `given ongoing call, when end call comes some time after start, then currentlyOngoingCall emits that call and then null`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, servicesManager) = Arrangement()
                .withServiceState(OngoingCallService.ServiceState.NOT_STARTED)
                .withValidAccountsUpdate(listOf(TEST_SELF_USER1))
                .arrange()
            servicesManager.handleOngoingCall(TEST_USER_ID1, TEST_ONGOING_CALL_DATA1)
            // when-then
            servicesManager.currentlyOngoingCall().test {
                advanceTimeBy((ServicesManager.DEBOUNCE_TIME + 50).milliseconds)
                servicesManager.handleOngoingCall(TEST_USER_ID1, null)
                advanceUntilIdle()
                assertEquals(TEST_ONGOING_CALL_DATA1, awaitItem())
                assertEquals(null, awaitItem())
            }
        }

    private inner class Arrangement {

        @MockK(relaxed = true)
        lateinit var context: Context

        @MockK
        lateinit var coreLogic: CoreLogic

        private var servicesManager: ServicesManager

        @MockK
        lateinit var ongoingCallServiceIntent: Intent

        @MockK
        lateinit var ongoingCallServiceIntentWithStopArgument: Intent

        private val validAccountsFlow = MutableSharedFlow<List<Pair<SelfUser, Team?>>>(replay = Int.MAX_VALUE)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkObject(OngoingCallService.Companion)
            every { OngoingCallService.Companion.newIntent(context) } returns ongoingCallServiceIntent
            every { OngoingCallService.Companion.newIntentToStop(context) } returns ongoingCallServiceIntentWithStopArgument
            coEvery { coreLogic.getGlobalScope().observeValidAccounts() } returns validAccountsFlow
            servicesManager = ServicesManager(context, dispatcherProvider, coreLogic)
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

        suspend fun withValidAccountsUpdate(list: List<SelfUser>): Arrangement = apply {
            validAccountsFlow.emit(list.map { it to null })
        }

        fun withServiceState(state: OngoingCallService.ServiceState) = apply {
            every { OngoingCallService.Companion.serviceState.get() } returns state
        }

        fun arrange() = this to servicesManager
    }

    companion object {
        private val TEST_USER_ID1 = UserId("user1", "domain")
        private val TEST_USER_ID2 = UserId("user2", "domain")
        private val TEST_SELF_USER1 = provideSelfUser(TEST_USER_ID1)
        private val TEST_SELF_USER2 = provideSelfUser(TEST_USER_ID2)
        private val TEST_CONVERSATION_ID1 = ConversationId("conversation1", "conversationDomain")
        private val TEST_CONVERSATION_ID2 = ConversationId("conversation2", "conversationDomain")
        private val TEST_ONGOING_CALL_DATA1 = OngoingCallData(TEST_USER_ID1, TEST_CONVERSATION_ID1, "call1")
        private val TEST_ONGOING_CALL_DATA2 = OngoingCallData(TEST_USER_ID2, TEST_CONVERSATION_ID2, "call2")
        private fun provideSelfUser(id: UserId): SelfUser = SelfUser(
            id = id,
            name = null,
            handle = null,
            email = null,
            phone = null,
            accentId = 0,
            teamId = null,
            connectionStatus = ConnectionState.NOT_CONNECTED,
            previewPicture = null,
            completePicture = null,
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            expiresAt = null
        )
    }
}
