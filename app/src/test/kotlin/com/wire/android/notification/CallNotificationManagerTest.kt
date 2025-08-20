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

import android.app.ActivityManager
import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.notification.CallNotificationManager.Companion.DEBOUNCE_TIME
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
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
    fun `given no incoming calls but when there is still active incoming call notification, then hide that notification`() =
        runTest(dispatcherProvider.main()) {
            // given
            val id = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val tag = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val userName = "user name"
            val (arrangement, callNotificationManager) = Arrangement()
                .withActiveNotifications(listOf(mockStatusBarNotification(id, tag)))
                .arrange()
            // when
            callNotificationManager.handleIncomingCalls(listOf(), TEST_USER_ID1, userName)
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(any(), any(), any()) }
            verify(exactly = 1) { arrangement.notificationManager.cancel(tag, id) }
        }

    @Test
    fun `given an incoming call for one user, then show notification for that call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification = mockk<Notification>()
            val userName = "user name"
            val callNotificationData = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName)
            val tag = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val id = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, callNotificationData)
                .arrange()
            arrangement.clearRecordedCalls() // clear first empty list recorded call
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName)
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(tag, id, notification) }
        }

    @Test
    fun `given an incoming call for one user, when call is updated, then update notification for that call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification = mockk<Notification>()
            val userName = "user name"
            val callNotificationData = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName)
            val updatedCall = TEST_CALL1.copy(conversationName = "new name")
            val updatedCallNotificationData = provideCallNotificationData(TEST_USER_ID1, updatedCall, userName)
            val tag = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val id = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, callNotificationData)
                .withIncomingNotificationForUserAndCall(notification, updatedCallNotificationData)
                .arrange()
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName)
            advanceUntilIdle()
            arrangement.clearRecordedCalls() // clear first empty list recorded call
            // when
            callNotificationManager.handleIncomingCalls(listOf(updatedCall), TEST_USER_ID1, userName) // updated call
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(tag, id, notification) } // should be updated
        }

    @Test
    fun `given an incoming call for one user, when call is not updated, then do not update notification for that call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification = mockk<Notification>()
            val userName = "user name"
            val callNotificationData = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName)
            val tag = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val id = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, callNotificationData)
                .arrange()
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName)
            advanceUntilIdle()
            arrangement.clearRecordedCalls() // clear first empty list recorded call
            // when
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName) // same call
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(tag, id, notification) } // should not be updated
        }

    @Test
    fun `given an incoming call for one same user, when another incoming call appears, then add notification only for this new call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val userName1 = "user name 1"
            val callNotificationData1 = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName1)
            val callNotificationData2 = provideCallNotificationData(TEST_USER_ID1, TEST_CALL2, userName1)
            val tag1 = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val tag2 = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val id1 = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val id2 = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL2.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, callNotificationData1)
                .withIncomingNotificationForUserAndCall(notification2, callNotificationData2)
                .arrange()
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName1)
            advanceUntilIdle()
            arrangement.clearRecordedCalls() // clear first empty list recorded call
            // when
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1, TEST_CALL2), TEST_USER_ID1, userName1)
            advanceUntilIdle()
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(tag1, id1, notification1) } // already shown previously
            verify(exactly = 1) { arrangement.notificationManager.notify(tag2, id2, notification2) } // should be added now
        }

    @Test
    fun `given incoming calls for two users, then show notification for the both calls`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val userName1 = "user name 1"
            val userName2 = "user name 2"
            val callNotificationData1 = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName1)
            val callNotificationData2 = provideCallNotificationData(TEST_USER_ID2, TEST_CALL2, userName2)
            val tag1 = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val tag2 = NotificationConstants.getIncomingCallTag(TEST_USER_ID2.toString())
            val id1 = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val id2 = NotificationConstants.getIncomingCallId(TEST_USER_ID2.toString(), TEST_CALL2.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, callNotificationData1)
                .withIncomingNotificationForUserAndCall(notification2, callNotificationData2)
                .arrange()
            arrangement.clearRecordedCalls() // clear first empty list recorded call
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName1)
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL2), TEST_USER_ID2, userName2)
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(tag1, id1, notification1) }
            verify(exactly = 1) { arrangement.notificationManager.notify(tag2, id2, notification2) }
            verify(exactly = 0) { arrangement.notificationManager.cancel(any(), any()) }
        }

    @Test
    fun `given two incoming calls for the same user, then show notification for the both calls`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val userName1 = "user name 1"
            val callNotificationData1 = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName1)
            val callNotificationData2 = provideCallNotificationData(TEST_USER_ID1, TEST_CALL2, userName1)
            val tag1 = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val tag2 = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val id1 = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val id2 = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL2.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, callNotificationData1)
                .withIncomingNotificationForUserAndCall(notification2, callNotificationData2)
                .arrange()
            arrangement.clearRecordedCalls() // clear first empty list recorded call
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1, TEST_CALL2), TEST_USER_ID1, userName1)
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(tag1, id1, notification1) }
            verify(exactly = 1) { arrangement.notificationManager.notify(tag2, id2, notification2) }
            verify(exactly = 0) { arrangement.notificationManager.cancel(any()) }
        }

    @Test
    fun `given incoming calls for two users, when one call ends, then do not cancel notification for another call`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val userName1 = "user name 1"
            val userName2 = "user name 2"
            val callNotificationData1 = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName1)
            val callNotificationData2 = provideCallNotificationData(TEST_USER_ID2, TEST_CALL2, userName2)
            val tag1 = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val tag2 = NotificationConstants.getIncomingCallTag(TEST_USER_ID2.toString())
            val id1 = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val id2 = NotificationConstants.getIncomingCallId(TEST_USER_ID2.toString(), TEST_CALL2.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, callNotificationData1)
                .withIncomingNotificationForUserAndCall(notification2, callNotificationData2)
                .arrange()
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName1)
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL2), TEST_USER_ID2, userName2)
            arrangement.withActiveNotifications(listOf(mockStatusBarNotification(id1, tag1), mockStatusBarNotification(id2, tag2)))
            advanceUntilIdle()
            arrangement.clearRecordedCalls() // clear calls recorded when initializing the state
            // when
            callNotificationManager.handleIncomingCalls(listOf(), TEST_USER_ID1, userName1) // first call is ended
            advanceUntilIdle()
            // then
            verify(exactly = 1) { arrangement.notificationManager.cancel(tag1, id1) }
            verify(exactly = 0) { arrangement.notificationManager.cancel(tag2, id2) }
        }

    @Test
    fun `given incoming calls for two users, when both call ends, then hide all notifications`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification1 = mockk<Notification>()
            val notification2 = mockk<Notification>()
            val userName1 = "user name 1"
            val userName2 = "user name 2"
            val callNotificationData1 = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName1)
            val callNotificationData2 = provideCallNotificationData(TEST_USER_ID2, TEST_CALL2, userName2)
            val tag1 = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val tag2 = NotificationConstants.getIncomingCallTag(TEST_USER_ID2.toString())
            val id1 = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val id2 = NotificationConstants.getIncomingCallId(TEST_USER_ID2.toString(), TEST_CALL2.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification1, callNotificationData1)
                .withIncomingNotificationForUserAndCall(notification2, callNotificationData2)
                .arrange()
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName1)
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL2), TEST_USER_ID2, userName2)
            advanceUntilIdle()
            arrangement.clearRecordedCalls() // clear calls recorded when initializing the state
            arrangement.withActiveNotifications(
                listOf(
                    mockStatusBarNotification(id1, tag1),
                    mockStatusBarNotification(id2, tag2)
                )
            )

            // when
            callNotificationManager.handleIncomingCalls(listOf(), TEST_USER_ID1, userName1)
            callNotificationManager.handleIncomingCalls(listOf(), TEST_USER_ID2, userName2)
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(tag1, id1, notification1) }
            verify(exactly = 0) { arrangement.notificationManager.notify(tag2, id2, notification2) }
            verify(exactly = 1) { arrangement.notificationManager.cancel(tag1, id1) }
            verify(exactly = 1) { arrangement.notificationManager.cancel(tag2, id2) }
        }

    @Test
    fun `given incoming call, when end call comes instantly after start, then do not even show notification`() =
        runTest(dispatcherProvider.main()) {
            // given
            val notification = mockk<Notification>()
            val userName = "user name"
            val callNotificationData = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName)
            val tag = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val id = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, callNotificationData)
                .arrange()
            // when
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName)
            advanceTimeBy((DEBOUNCE_TIME - 50).milliseconds)
            callNotificationManager.handleIncomingCalls(listOf(), TEST_USER_ID1, userName)
            // then
            verify(exactly = 0) { arrangement.notificationManager.notify(tag, id, notification) }
        }

    @Test
    fun `given incoming call, when end call comes some time after start, then first show notification and then hide`() =
        runTest(dispatcherProvider.main()) {
            // given
            val userName = "user name"
            val callNotificationData = provideCallNotificationData(TEST_USER_ID1, TEST_CALL1, userName)
            val tag = NotificationConstants.getIncomingCallTag(TEST_USER_ID1.toString())
            val id = NotificationConstants.getIncomingCallId(TEST_USER_ID1.toString(), TEST_CALL1.conversationId.toString())
            val notification = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withIncomingNotificationForUserAndCall(notification, callNotificationData)
                .arrange()
            arrangement.clearRecordedCalls() // clear first empty list recorded call
            // when
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName)
            advanceTimeBy((DEBOUNCE_TIME + 50).milliseconds)
            arrangement.withActiveNotifications(listOf(mockStatusBarNotification(id, tag)))
            callNotificationManager.handleIncomingCalls(listOf(), TEST_USER_ID1, userName)
            // then
            verify(exactly = 1) { arrangement.notificationManager.notify(tag, id, notification) }
            verify(exactly = 1) { arrangement.notificationManager.cancel(tag, id) }
        }

    @Test
    fun `given incoming call for current session, when handling incoming call, then show it as full screen intent`() =
        runTest(dispatcherProvider.main()) {
            // given
            val currentSession = AccountInfo.Valid(UserId("currentUserId", "domain"))
            val userName = "user name"
            val (arrangement, callNotificationManager) = Arrangement()
                .withCurrentSession(currentSession)
                .arrange()
            // when
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), currentSession.userId, userName)
            advanceUntilIdle()
            // then
            verify(exactly = 1) {
                arrangement.callNotificationBuilder.getIncomingCallNotification(data = any(), asFullScreenIntent = eq(true))
            }
        }

    @Test
    fun `given incoming call for another session, when handling incoming call, then do not show it as full screen intent`() =
        runTest(dispatcherProvider.main()) {
            // given
            val currentSession = AccountInfo.Valid(UserId("currentUserId", "domain"))
            val userName = "user name"
            val (arrangement, callNotificationManager) = Arrangement()
                .withCurrentSession(currentSession)
                .arrange()
            // when
            callNotificationManager.handleIncomingCalls(listOf(TEST_CALL1), TEST_USER_ID1, userName)
            advanceUntilIdle()
            // then
            verify(exactly = 1) {
                arrangement.callNotificationBuilder.getIncomingCallNotification(data = any(), asFullScreenIntent = eq(false))
            }
        }

    @Test
    fun `given incoming call exists, when bringing back notification, then show the notification again`() =
        runTest(dispatcherProvider.main()) {
            // given
            val call = provideCall(ConversationId("convId", "domain"), CallStatus.INCOMING)
            val data = provideCallNotificationData(TestUser.SELF_USER_ID, call, "user name")
            val tag = NotificationConstants.getIncomingCallTag(data.userId.toString())
            val id = NotificationConstants.getIncomingCallId(data.userId.toString(), data.conversationId.toString())
            val notification = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withCurrentSession(AccountInfo.Valid(TestUser.SELF_USER_ID))
                .withIncomingNotificationForUserAndCall(notification, data)
                .arrange()
            callNotificationManager.handleIncomingCalls(listOf(call), data.userId, data.userName)
            callNotificationManager.hideIncomingCallNotification(data.userId.toString(), data.conversationId.toString())
            advanceUntilIdle()
            arrangement.clearRecordedCalls()
            // when
            callNotificationManager.bringBackIncomingCallNotification(data.userId.toString(), data.conversationId.toString())
            advanceUntilIdle()
            // then
            verify(exactly = 1) {
                arrangement.callNotificationBuilder.getIncomingCallNotification(
                    data = match { it.userId == data.userId && it.conversationId == data.conversationId },
                    asFullScreenIntent = any(),
                )
            }
            verify(exactly = 1) {
                arrangement.notificationManager.notify(tag, id, notification)
            }
        }

    @Test
    fun `given incoming call does not exist, when bringing back notification, then do not show the notification again`() =
        runTest(dispatcherProvider.main()) {
            // given
            val call = provideCall(ConversationId("convId", "domain"), CallStatus.INCOMING)
            val data = provideCallNotificationData(TestUser.SELF_USER_ID, call, "user name")
            val tag = NotificationConstants.getIncomingCallTag(data.userId.toString())
            val id = NotificationConstants.getIncomingCallId(data.userId.toString(), data.conversationId.toString())
            val notification = mockk<Notification>()
            val (arrangement, callNotificationManager) = Arrangement()
                .withCurrentSession(AccountInfo.Valid(TestUser.SELF_USER_ID))
                .withIncomingNotificationForUserAndCall(notification, data)
                .arrange()
            callNotificationManager.handleIncomingCalls(listOf(call), data.userId, data.userName)
            callNotificationManager.hideIncomingCallNotification(data.userId.toString(), data.conversationId.toString())
            callNotificationManager.handleIncomingCalls(emptyList(), data.userId, data.userName) // simulate call not existing anymore
            advanceUntilIdle()
            arrangement.clearRecordedCalls()
            // when
            callNotificationManager.bringBackIncomingCallNotification(data.userId.toString(), data.conversationId.toString())
            advanceUntilIdle()
            // then
            verify(exactly = 0) {
                arrangement.callNotificationBuilder.getIncomingCallNotification(
                    data = match { it.userId == data.userId && it.conversationId == data.conversationId },
                    asFullScreenIntent = any(),
                )
            }
            verify(exactly = 0) {
                arrangement.notificationManager.notify(tag, id, notification)
            }
        }

    private inner class Arrangement {

        @MockK
        lateinit var context: Context

        @MockK
        lateinit var notificationManager: NotificationManagerCompat

        @MockK
        lateinit var activityManager: ActivityManager

        @MockK
        lateinit var callNotificationBuilder: CallNotificationBuilder

        @MockK
        lateinit var coreLogic: CoreLogic

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            mockkStatic(NotificationManagerCompat::from)
            every { NotificationManagerCompat.from(any()) } returns notificationManager
            every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
            withActiveNotifications(emptyList())
            every { callNotificationBuilder.getIncomingCallNotification(any(), any()) } returns mockk()
            withCurrentSession(AccountInfo.Valid(UserId("userId", "domain")))
        }

        fun clearRecordedCalls() {
            clearMocks(
                notificationManager,
                activityManager,
                callNotificationBuilder,
                answers = false,
                recordedCalls = true,
                childMocks = false,
                verificationMarks = false,
                exclusionRules = false
            )
        }

        fun withIncomingNotificationForUserAndCall(notification: Notification, forCallNotificationData: CallNotificationData) = apply {
            every { callNotificationBuilder.getIncomingCallNotification(eq(forCallNotificationData), any()) } returns notification
        }

        fun withActiveNotifications(list: List<StatusBarNotification>) = apply {
            every { notificationManager.activeNotifications } returns list
        }

        fun withCurrentSession(accountInfo: AccountInfo) = apply {
            coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Success(accountInfo)
        }

        fun arrange() = this to CallNotificationManager(
            context = context,
            dispatcherProvider = dispatcherProvider,
            builder = callNotificationBuilder,
            coreLogic = coreLogic,
            qualifiedIdMapper = QualifiedIdMapperImpl(TestUser.SELF_USER_ID),
        )
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
            callerId = UserId("caller", "domain"),
            participants = listOf(),
            isMuted = true,
            isCameraOn = false,
            isCbrEnabled = false,
            maxParticipants = 0,
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.OneOnOne,
            callerName = "otherUsername",
            callerTeamName = "team_1"
        )

        private fun provideCallNotificationData(userId: UserId, call: Call, userName: String) = CallNotificationData(
            userId = userId,
            userName = userName,
            conversationId = call.conversationId,
            conversationName = call.conversationName,
            conversationType = call.conversationType,
            callerName = call.callerName,
            callerTeamName = call.callerTeamName,
            callStatus = call.status
        )

        fun mockStatusBarNotification(id: Int, tag: String): StatusBarNotification {
            val statusBarNotification = mockk<StatusBarNotification>()
            every { statusBarNotification.id } returns id
            every { statusBarNotification.tag } returns tag
            return statusBarNotification
        }
    }
}
