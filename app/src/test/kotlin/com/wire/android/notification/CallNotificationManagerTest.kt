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
package com.wire.android.notification

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.notification.CallNotificationManager.Companion.DEBOUNCE_TIME
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class CallNotificationManagerTest {

    val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given no incoming calls, then hide notification`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, callNotificationManager) = Arrangement()
                .arrange()
            callNotificationManager.handleIncomingCallNotifications(listOf(), TEST_USER_ID1)
            advanceUntilIdle()
            // then
            verify(exactly = 0) {
                arrangement.notificationManager.notify(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID, any())
            }
            verify(exactly = 1) {
                arrangement.notificationManager.cancel(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID)
            }
        }

    @Test
    fun `given no outgoing calls, when handling notifications, then hide outgoing call notification`() =
        runTest(dispatcherProvider.main()) {
            // given
            val (arrangement, callNotificationManager) = Arrangement()
                .arrange()
            callNotificationManager.handleOutgoingCallNotifications(listOf(), TEST_USER_ID1)
            advanceUntilIdle()
            // then
            verify(exactly = 0) {
                arrangement.notificationManager.notify(NotificationConstants.CALL_OUTGOING_NOTIFICATION_ID, any())
            }
            verify(exactly = 1) {
                arrangement.notificationManager.cancel(NotificationConstants.CALL_OUTGOING_NOTIFICATION_ID)
            }
        }

    @Test
    fun `given an incoming call for one user, then show notification for that call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, TEST_USER_ID1, TEST_CALL1)
                .arrange()
            arrangement.clearRecordedCallsForNotificationManager() // clear first empty list recorded call
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL1), TEST_USER_ID1)
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(any(), notification) }
            verify(exactly = 0) { arrangement.notificationManager.cancel(any()) }
        }
    @Test
    fun `given an outgoing call for one user, when handling notifications,, then show notification for that call`() =
        runTest(dispatcherProvider.main()) {
            val notification = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withOutgoingNotificationForUserAndCall(notification, TEST_USER_ID1, TEST_CALL1.conversationId, TEST_CALL1.conversationName!!)
                .arrange()

            arrangement.clearRecordedCallsForNotificationManager() // clear first empty list recorded call
            callNotificationManager.handleOutgoingCallNotifications(listOf(TEST_CALL1), TEST_USER_ID1)
            advanceUntilIdle()

            verify(exactly = 1) {
                arrangement.notificationManager.notify(NotificationConstants.CALL_OUTGOING_NOTIFICATION_ID, notification)
            }
            verify(exactly = 0) {
                arrangement.notificationManager.cancel(NotificationConstants.CALL_OUTGOING_NOTIFICATION_ID)
            }
        }

    @Test
    fun `given incoming calls for two users, then show notification for the first call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, TEST_USER_ID1, TEST_CALL1)
                .withIncomingNotificationForUserAndCall(notification2, TEST_USER_ID2, TEST_CALL2)
                .arrange()
            arrangement.clearRecordedCallsForNotificationManager() // clear first empty list recorded call
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL1), TEST_USER_ID1)
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL2), TEST_USER_ID2)
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(any(), notification1) }
            verify(exactly = 0) { arrangement.notificationManager.notify(any(), notification2) }
            verify(exactly = 0) { arrangement.notificationManager.cancel(any()) }
        }

    @Test
    fun `given incoming calls for two users, when one call ends, then show notification for another call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, TEST_USER_ID1, TEST_CALL1)
                .withIncomingNotificationForUserAndCall(notification2, TEST_USER_ID2, TEST_CALL2)
                .arrange()
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL1), TEST_USER_ID1)
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL2), TEST_USER_ID2)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForNotificationManager() // clear calls recorded when initializing the state
            // when
            callNotificationManager.handleIncomingCallNotifications(listOf(), TEST_USER_ID1)
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(any(), notification1) }
            verify(exactly = 1) { arrangement.notificationManager.notify(any(), notification2) }
            verify(exactly = 0) { arrangement.notificationManager.cancel(any()) }
        }

    @Test
    fun `given incoming calls for two users, when both call ends, then hide notification`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, TEST_USER_ID1, TEST_CALL1)
                .withIncomingNotificationForUserAndCall(notification2, TEST_USER_ID2, TEST_CALL2)
                .arrange()
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL1), TEST_USER_ID1)
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL2), TEST_USER_ID2)
            advanceUntilIdle()
            arrangement.clearRecordedCallsForNotificationManager() // clear calls recorded when initializing the state
            // when
            callNotificationManager.handleIncomingCallNotifications(listOf(), TEST_USER_ID1)
            callNotificationManager.handleIncomingCallNotifications(listOf(), TEST_USER_ID2)
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(any(), notification1) }
            verify(exactly = 0) { arrangement.notificationManager.notify(any(), notification2) }
            verify(exactly = 1) { arrangement.notificationManager.cancel(any()) }
        }

    @Test
    fun `given incoming call, when end call comes instantly after start, then do not even show notification`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, TEST_USER_ID1, TEST_CALL1)
                .arrange()
            // when
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL1), TEST_USER_ID1)
            advanceTimeBy((DEBOUNCE_TIME - 50).milliseconds)
            callNotificationManager.handleIncomingCallNotifications(listOf(), TEST_USER_ID1)
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(any(), notification) }
            verify(exactly = 1) { arrangement.notificationManager.cancel(NotificationConstants.CALL_INCOMING_NOTIFICATION_ID) }
        }

    @Test
    fun `given incoming call, when end call comes some time after start, then first show notification and then hide`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, TEST_USER_ID1, TEST_CALL1)
                .arrange()
            arrangement.clearRecordedCallsForNotificationManager() // clear first empty list recorded call
            // when
            callNotificationManager.handleIncomingCallNotifications(listOf(TEST_CALL1), TEST_USER_ID1)
            advanceTimeBy((DEBOUNCE_TIME + 50).milliseconds)
            callNotificationManager.handleIncomingCallNotifications(listOf(), TEST_USER_ID1)
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(any(), notification) }
            verify(exactly = 1) { arrangement.notificationManager.cancel(any()) }
        }

    private inner class Arrangement {

        @MockK
        lateinit var context: Context

        @MockK
        lateinit var notificationManager: NotificationManagerCompat

        @MockK
        lateinit var callNotificationBuilder: CallNotificationBuilder

        private var callNotificationManager: CallNotificationManager

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(NotificationManagerCompat::from)
            every { NotificationManagerCompat.from(any()) } returns notificationManager
            callNotificationManager = CallNotificationManager(context, dispatcherProvider, callNotificationBuilder)
        }

        fun clearRecordedCallsForNotificationManager() {
            clearMocks(
                notificationManager,
                answers = false,
                recordedCalls = true,
                childMocks = false,
                verificationMarks = false,
                exclusionRules = false
            )
        }

        fun withIncomingNotificationForUserAndCall(notification: Notification, forUser: UserId, forCall: Call) = apply {
            every { callNotificationBuilder.getIncomingCallNotification(eq(forCall), eq(forUser)) } returns notification
        }
        fun withOutgoingNotificationForUserAndCall(
            notification: Notification,
            forUser: UserId,
            conversationId: ConversationId,
            conversationName: String
        ) = apply {
            every {
                callNotificationBuilder.getOutgoingCallNotification(
                    eq(conversationId),
                    eq(forUser),
                    eq(conversationName)
                )
            } returns notification
        }

        fun arrange() = this to callNotificationManager
    }

    companion object {
        private val TEST_USER_ID1 = UserId("user1", "domain")
        private val TEST_USER_ID2 = UserId("user2", "domain")
        private val TEST_CONVERSATION_ID1 = ConversationId("conversation1", "conversationDomain")
        private val TEST_CONVERSATION_ID2 = ConversationId("conversation2", "conversationDomain")
        private val TEST_CALL1 = provideCall(TEST_CONVERSATION_ID1)
        private val TEST_CALL2 = provideCall(TEST_CONVERSATION_ID2)
        private fun provideCall(
            conversationId: ConversationId = TEST_CONVERSATION_ID1,
            status: CallStatus = CallStatus.INCOMING,
        ) = Call(
            conversationId = conversationId,
            status = status,
            callerId = UserId("caller", "domain").toString(),
            participants = listOf(),
            isMuted = true,
            isCameraOn = false,
            isCbrEnabled = false,
            maxParticipants = 0,
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.ONE_ON_ONE,
            callerName = "otherUsername",
            callerTeamName = "team_1"
        )
    }
}
