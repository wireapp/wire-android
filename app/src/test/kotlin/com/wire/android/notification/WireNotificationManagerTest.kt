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

import com.wire.android.common.runTestWithCancellation
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.media.PingRinger
import com.wire.android.services.ServicesManager
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.lifecycle.ConnectionPolicyManager
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.notification.LocalNotification
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.data.notification.LocalNotificationMessageAuthor
import com.wire.kalium.logic.data.notification.LocalNotificationUpdateMessageAction
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveOutgoingCallUseCase
import com.wire.kalium.logic.feature.connection.MarkConnectionRequestAsNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ConversationScope
import com.wire.kalium.logic.feature.message.GetNotificationsUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.message.MessageScope
import com.wire.kalium.logic.feature.message.Result
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.E2EIRequiredResult
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveE2EIRequiredUseCase
import com.wire.kalium.logic.feature.user.UserScope
import com.wire.kalium.logic.sync.SyncManager
import io.mockk.MockKAnnotations
import io.mockk.MockKMatcherScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class WireNotificationManagerTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun givenNotAuthenticatedUser_whenFetchAndShowNotificationsOnceCalled_thenNothingHappen() = runTest(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement()
            .withSession(GetAllSessionsResult.Failure.NoSessionFound)
            .arrange()

        manager.fetchAndShowNotificationsOnce("user_id")
        advanceUntilIdle()

        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        verify(exactly = 0) {
            arrangement.messageNotificationManager.handleNotification(
                newNotifications = any(),
                userId = any(),
                userName = TestUser.SELF_USER.handle!!
            )
        }
        verify(exactly = 0) { arrangement.callNotificationManager.handleIncomingCallNotifications(any(), any()) }
    }

    // todo: check later with boris!
    @Test
    fun givenAuthenticatedUser_whenFetchAndShowNotificationsOnceCalled_thenConnectionPolicyManagerIsCalled() =
        runTest(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement()
                .withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN)))
                .withMessageNotifications(listOf())
                .arrange()

            manager.fetchAndShowNotificationsOnce("user_id")
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(TEST_AUTH_TOKEN.userId, any()) }
        }

    @Test
    fun givenNotAuthenticatedUser_whenObserveCalled_thenNothingHappenAndCallNotificationHides() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement()
                .withCurrentScreen(CurrentScreen.SomeOther)
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(), this)
            advanceUntilIdle()

            verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
            verify(exactly = 1) { arrangement.callNotificationManager.hideAllNotifications() }
        }

    @Test
    fun givenSomeIncomingCalls_whenObserving_thenCallNotificationShowed() = runTestWithCancellation(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement()
            .withIncomingCalls(listOf())
            .withOutgoingCalls(listOf())
            .withMessageNotifications(listOf())
            .withCurrentScreen(CurrentScreen.SomeOther)
            .withCurrentUserSession(CurrentSessionResult.Success(AccountInfo.Valid(provideUserId())))
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this)
        runCurrent()

        verify(exactly = 0) { arrangement.callNotificationManager.hideIncomingCallNotification() }
        verify(exactly = 1) { arrangement.callNotificationManager.handleIncomingCallNotifications(any(), any()) }
    }

    @Test
    fun givenSomeIncomingCall_whenCurrentUserIsDifferentFromCallReceiver_thenCallNotificationIsShown() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val user1 = provideUserId("user1")
            val user2 = provideUserId("user2")
            val incomingCalls = listOf(provideCall())
            val (arrangement, manager) = Arrangement()
                .withSpecificUserSession(userId = user1, incomingCalls = listOf())
                .withSpecificUserSession(userId = user2, incomingCalls = incomingCalls)
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.SomeOther)
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(user1.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(user1, user2), this)
            runCurrent()

            verify(exactly = 1) { arrangement.callNotificationManager.handleIncomingCallNotifications(incomingCalls, user2) }
        }
    @Test
    fun givenOutgoingCall_whenCurrentUserIsDifferentFromCallReceiver_thenCallNotificationIsShown() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val user1 = provideUserId("user1")
            val user2 = provideUserId("user2")
            val outgoingCalls = listOf(provideCall().copy(status = CallStatus.STARTED))
            val (arrangement, manager) = Arrangement()
                .withSpecificUserSession(userId = user1)
                .withSpecificUserSession(userId = user2, outgoingCalls = outgoingCalls)
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.SomeOther)
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(user1.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(user1, user2), this)
            runCurrent()

            verify(exactly = 1) { arrangement.callNotificationManager.handleOutgoingCallNotifications(outgoingCalls, user2) }
        }

    @Test
    fun givenSomeNotifications_whenAppIsInForegroundAndNoUserLoggedIn_thenMessageNotificationNotShowed() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf(provideCall()))
                .withMessageNotifications(
                    listOf(
                        provideLocalNotificationConversation(
                            messages = listOf(provideLocalNotificationMessage())
                        )
                    )
                )
                .withCurrentScreen(CurrentScreen.SomeOther).arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(), this)
            runCurrent()

            verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
            verify(exactly = 0) {
                arrangement.messageNotificationManager.handleNotification(
                    newNotifications = any(), userId = any(), userName = TestUser.SELF_USER.handle!!
                )
            }
            verify(exactly = 1) { arrangement.callNotificationManager.hideAllNotifications() }
        }

    @Test
    fun givenSomeNotifications_whenAppIsInBackgroundAndNoUserLoggedIn_thenMessageNotificationNotShowed() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf(provideCall()))
                .withMessageNotifications(
                    listOf(
                        provideLocalNotificationConversation(
                            messages = listOf(provideLocalNotificationMessage())
                        )
                    )
                )
                .withCurrentScreen(CurrentScreen.InBackground)
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(), this)
            runCurrent()

            verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
            verify(exactly = 0) {
                arrangement.messageNotificationManager.handleNotification(
                    any(), any(), TestUser.SELF_USER.handle!!
                )
            }
            verify(exactly = 1) { arrangement.callNotificationManager.hideAllNotifications() }
        }

    @Test
    fun givenSomeNotifications_whenObserveCalled_thenCallNotificationShowed() = runTestWithCancellation(dispatcherProvider.main()) {
        val messageNotifications = listOf<LocalNotification>(
            provideLocalNotificationConversation(messages = listOf(provideLocalNotificationMessage())),
            provideLocalNotificationUpdateMessage()
        )
        val (arrangement, manager) = Arrangement()
            .withMessageNotifications(messageNotifications)
            .withIncomingCalls(listOf())
            .withOutgoingCalls(listOf())
            .withCurrentScreen(CurrentScreen.SomeOther)
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId(TestUser.SELF_USER.id.value)), this)
        runCurrent()

        verify(exactly = 1) {
            arrangement.messageNotificationManager.handleNotification(
                messageNotifications, TestUser.SELF_USER.id, TestUser.SELF_USER.handle!!
            )
        }
    }

    @Test
    fun givenSomeNotificationsAndCurrentScreenIsConversation_whenObserveCalled_thenNotificationIsNotShowed() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val conversationId = ConversationId("conversation_value", "conversation_domain")
            val (arrangement, manager) = Arrangement()
                .withMessageNotifications(
                    listOf(
                        provideLocalNotificationConversation(
                            id = conversationId,
                            messages = listOf(provideLocalNotificationMessage())
                        )
                    )
                )
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withCurrentScreen(CurrentScreen.Conversation(conversationId))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId(TestUser.SELF_USER.id.value)), this)
            runCurrent()

            verify(exactly = 1) {
                arrangement.messageNotificationManager.handleNotification(
                    listOf(), TestUser.SELF_USER.id, TestUser.SELF_USER.handle!!
                )
            }
            coVerify(atLeast = 1) {
                arrangement.markMessagesAsNotified(
                    MarkMessagesAsNotifiedUseCase.UpdateTarget.SingleConversation(
                        conversationId
                    )
                )
            }
        }

    @Test
    fun givenSomeNotifications_whenSelfUserChanged_thenNotificationIsNotDuplicated() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val conversationId = ConversationId("conversation_value", "conversation_domain")
            val selfUserFlow = MutableStateFlow(TestUser.SELF_USER)
            val (arrangement, manager) = Arrangement()
                .withMessageNotifications(
                    listOf(
                        provideLocalNotificationConversation(
                            id = conversationId,
                            messages = listOf(provideLocalNotificationMessage())
                        )
                    )
                )
                .withEstablishedCall(listOf())
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .withSelfUser(selfUserFlow)
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this)
            selfUserFlow.value = TestUser.SELF_USER.copy(availabilityStatus = UserAvailabilityStatus.NONE)
            runCurrent()

            verify(exactly = 1) {
                arrangement.messageNotificationManager.handleNotification(any(), any(), any())
            }
        }

    @Test
    fun givenCurrentScreenIsConversation_whenObserveCalled_thenNotificationForThatConversationIsHidden() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val conversationId = ConversationId("conversation_value", "conversation_domain")
            val (arrangement, manager) = Arrangement()
                .withMessageNotifications(listOf())
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withCurrentScreen(CurrentScreen.Conversation(conversationId))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this)
            runCurrent()

            coVerify(atLeast = 1) { arrangement.messageNotificationManager.hideNotification(conversationId, provideUserId()) }
        }

    @Test
    fun givenASingleUserId_whenCallingFetchAndShowOnceInParallel_thenPushNotificationIsHandledOnlyOnce() =
        runTest(dispatcherProvider.main()) {
            val userId = TEST_AUTH_TOKEN.userId
            val (arrangement, manager) = Arrangement()
                .withMessageNotifications(listOf())
                .withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN)))
                .withIncomingCalls(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .arrange()

            coEvery { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId, any()) } coAnswers {
                // Push handling is taking 10 minutes
                delay(10.minutes)
            }
            launch {
                // We call fetchAndShow now
                manager.fetchAndShowNotificationsOnce(userId.value)
            }
            launch {
                // After 5 minutes we call fetchAndShow again
                delay(5.minutes)
                manager.fetchAndShowNotificationsOnce(userId.value)
            }
            // After first call, should have handled push once
            advanceTimeBy(1.minutes.inWholeMilliseconds)
            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId, any()) }

            // After second call, should have handled push once
            advanceTimeBy(6.minutes.inWholeMilliseconds)
            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId, any()) }

            // After everything ends, should have handled push once
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId, any()) }
        }

    @Test
    fun givenASingleUserId_whenNotificationReceivedAndNoSuchUser_shouldSkipNotification() = runTest(dispatcherProvider.main()) {
        val otherAuthSession = provideAccountInfo("other_id")
        val userId = otherAuthSession.userId
        val (arrangement, manager) = Arrangement()
            .withMessageNotifications(listOf())
            .withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN)))
            .withIncomingCalls(listOf())
            .withCurrentScreen(CurrentScreen.InBackground)
            .arrange()

        manager.fetchAndShowNotificationsOnce(userId.value)
        advanceUntilIdle()

        coVerify(exactly = 0) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId, any()) }
    }

    @Test
    fun givenASingleUserId_whenNotificationReceivedAndNotCurrentUserButExistOnThatDevice_shouldCheckNotification() =
        runTest(dispatcherProvider.main()) {
            val otherAuthSession = provideAccountInfo("other_id")
            val userId = otherAuthSession.userId
            val (arrangement, manager) = Arrangement().withMessageNotifications(listOf())
                .withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN, otherAuthSession)))
                .withIncomingCalls(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .arrange()

            manager.fetchAndShowNotificationsOnce(userId.value)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId, any()) }
        }

    @Test
    fun givenPingNotification_whenObserveCalled_thenPingSoundIsPlayed() = runTestWithCancellation(dispatcherProvider.main()) {
        val conversationId = ConversationId("conversation_value", "conversation_domain")
        val (arrangement, manager) = Arrangement()
            .withMessageNotifications(
                listOf(
                    provideLocalNotificationConversation(
                        messages = listOf(provideLocalNotificationMessagePing())
                    )
                )
            )
            .withIncomingCalls(listOf())
            .withOutgoingCalls(listOf())
            .withCurrentScreen(CurrentScreen.Conversation(id = conversationId))
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this)
        runCurrent()

        verify(exactly = 1) {
            arrangement.pingRinger.ping(
                resource = any(),
                isReceivingPing = any()
            )
        }
    }

    @Test
    fun givenAppInBackground_withValidCurrentAccountAndOngoingCall_whenObserving_thenStartOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .withEstablishedCall(listOf(call))
                .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            advanceUntilIdle()

            verify(exactly = 1) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 0) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInBackground_withValidCurrentAccountAndNoOngoingCall_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .withEstablishedCall(listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            runCurrent()

            verify(exactly = 0) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInBackground_withInvalidCurrentAccountAndOngoingCall_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .withEstablishedCall(listOf(call))
                .withOutgoingCalls(listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(provideInvalidAccountInfo(userId.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            runCurrent()

            verify(exactly = 0) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInBackground_withInvalidCurrentAccountAndNoOngoingCall_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .withEstablishedCall(listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(provideInvalidAccountInfo(userId.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            runCurrent()

            verify(exactly = 0) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInForeground_withValidCurrentAccountAndOngoingCall_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.Home)
                .withEstablishedCall(listOf(call))
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            runCurrent()

            verify(exactly = 0) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInForeground_withValidCurrentAccountAndNoOngoingCall_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.Home)
                .withEstablishedCall(listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            runCurrent()

            verify(exactly = 0) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInForeground_withInvalidCurrentAccountAndOngoingCall_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.Home)
                .withEstablishedCall(listOf(call))
                .withCurrentUserSession(CurrentSessionResult.Success(provideInvalidAccountInfo(userId.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            runCurrent()

            verify(exactly = 0) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInForeground_withInvalidCurrentAccountAndNoOngoingCall_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.Home)
                .withEstablishedCall(listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(provideInvalidAccountInfo(userId.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            runCurrent()

            verify(exactly = 0) { arrangement.servicesManager.startOngoingCallService() }
            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInBackground_withTwoValidAccountsAndOngoingCallForNotCurrentOne_whenObserving_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId1 = UserId("value1", "domain")
            val userId2 = UserId("value2", "domain")
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withCurrentScreen(CurrentScreen.InBackground)
                .withSpecificUserSession(userId = userId1, establishedCalls = listOf())
                .withSpecificUserSession(userId = userId2, establishedCalls = listOf(call))
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId1.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId1, userId2), this)
            advanceUntilIdle()

            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInBackground_withTwoValidAccountsAndOngoingCallForNotCurrentOne_whenCurrentAccountChanges_thenStartOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId1 = UserId("value1", "domain")
            val userId2 = UserId("value2", "domain")
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withCurrentScreen(CurrentScreen.InBackground)
                .withSpecificUserSession(userId = userId1, establishedCalls = listOf())
                .withSpecificUserSession(userId = userId2, establishedCalls = listOf(call))
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId1.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId1, userId2), this)
            advanceUntilIdle()

            arrangement.withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId2.value)))
            advanceUntilIdle()

            verify(exactly = 1) { arrangement.servicesManager.startOngoingCallService() }
        }

    @Test
    fun givenAppInBackground_withTwoValidAccountsAndOngoingCallForCurrentOne_whenCurrentAccountChanges_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId1 = UserId("value1", "domain")
            val userId2 = UserId("value2", "domain")
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withCurrentScreen(CurrentScreen.InBackground)
                .withSpecificUserSession(userId = userId1, establishedCalls = listOf(call))
                .withSpecificUserSession(userId = userId2, establishedCalls = listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId1.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId1, userId2), this)
            advanceUntilIdle()

            arrangement.withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId2.value)))
            advanceUntilIdle()

            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenAppInBackground_withValidCurrentAccountAndOngoingCall_whenAccountBecomesInvalid_thenStopOngoingCallService() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val userId = provideUserId()
            val call = provideCall().copy(status = CallStatus.ESTABLISHED)
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .withEstablishedCall(listOf(call))
                .withCurrentUserSession(CurrentSessionResult.Success(provideAccountInfo(userId.value)))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(userId), this)
            advanceUntilIdle()

            arrangement.withCurrentUserSession(CurrentSessionResult.Success(provideInvalidAccountInfo(userId.value)))
            advanceUntilIdle()

            verify(exactly = 1) { arrangement.servicesManager.stopOngoingCallService() }
        }

    @Test
    fun givenSomeNotificationsAndUserBlockedByE2EIRequired_whenObserveCalled_thenNotificationIsNotShowed() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val conversationId = ConversationId("conversation_value", "conversation_domain")
            val (arrangement, manager) = Arrangement()
                .withMessageNotifications(
                    listOf(
                        provideLocalNotificationConversation(
                            id = conversationId,
                            messages = listOf(provideLocalNotificationMessage())
                        )
                    )
                )
                .withIncomingCalls(listOf())
                .withOutgoingCalls(listOf())
                .withCurrentScreen(CurrentScreen.SomeOther)
                .withObserveE2EIRequired(E2EIRequiredResult.NoGracePeriod.Create)
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId(TestUser.SELF_USER.id.value)), this)
            runCurrent()

            verify(exactly = 0) {
                arrangement.messageNotificationManager.handleNotification(
                    listOf(), TestUser.SELF_USER.id, TestUser.SELF_USER.handle!!
                )
            }
            coVerify(atLeast = 1) {
                arrangement.markMessagesAsNotified(MarkMessagesAsNotifiedUseCase.UpdateTarget.AllConversations)
            }
        }

    @Test
    fun givenSessionExistsForTheUserAndNoActiveJobs_whenGettingUsersToObserve_thenReturnThatUser() =
        runTest(dispatcherProvider.main()) {
            // given
            val userId = provideUserId()
            val (_, manager) = Arrangement()
                .withDoesValidSessionExistResult(userId, DoesValidSessionExistResult.Success(true))
                .arrange()
            val hasActiveJobs: (UserId) -> Boolean = { false }
            // when
            val result = manager.newUsersWithValidSessionAndWithoutActiveJobs(listOf(userId), hasActiveJobs)
            // then
            assertEquals(listOf(userId), result)
        }

    @Test
    fun givenSessionExistsForTheUserButWithActiveJobs_whenGettingUsersToObserve_thenDoNotReturnThatUser() =
        runTest(dispatcherProvider.main()) {
            // given
            val userId = provideUserId()
            val (_, manager) = Arrangement()
                .withDoesValidSessionExistResult(userId, DoesValidSessionExistResult.Success(true))
                .arrange()
            val hasActiveJobs: (UserId) -> Boolean = { true }
            // when
            val result = manager.newUsersWithValidSessionAndWithoutActiveJobs(listOf(userId), hasActiveJobs)
            // then
            assertEquals(listOf(), result)
        }

    @Test
    fun givenSessionDoesNotExistForTheUserAndNoActiveJobs_whenGettingUsersToObserve_thenDoNotReturnThatUser() =
        runTest(dispatcherProvider.main()) {
            // given
            val userId = provideUserId()
            val (_, manager) = Arrangement()
                .withDoesValidSessionExistResult(userId, DoesValidSessionExistResult.Success(false))
                .arrange()
            val hasActiveJobs: (UserId) -> Boolean = { false }
            // when
            val result = manager.newUsersWithValidSessionAndWithoutActiveJobs(listOf(userId), hasActiveJobs)
            // then
            assertEquals(listOf(), result)
        }

    private inner class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        lateinit var globalKaliumScope: GlobalKaliumScope

        @MockK
        lateinit var messageNotificationManager: MessageNotificationManager

        @MockK
        lateinit var callNotificationManager: CallNotificationManager

        @MockK
        lateinit var callsScope: CallsScope

        @MockK
        lateinit var conversationScope: ConversationScope

        @MockK
        lateinit var userScope: UserScope

        @MockK
        lateinit var syncManager: SyncManager

        @MockK
        lateinit var messageScope: MessageScope

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var getNotificationsUseCase: GetNotificationsUseCase

        @MockK
        lateinit var connectionPolicyManager: ConnectionPolicyManager

        @MockK
        lateinit var markMessagesAsNotified: MarkMessagesAsNotifiedUseCase

        @MockK
        lateinit var markConnectionRequestAsNotified: MarkConnectionRequestAsNotifiedUseCase

        @MockK
        lateinit var getIncomingCallsUseCase: GetIncomingCallsUseCase

        @MockK
        lateinit var observeOutgoingCall: ObserveOutgoingCallUseCase

        @MockK
        lateinit var establishedCall: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var getSessionsUseCase: GetSessionsUseCase

        @MockK
        lateinit var servicesManager: ServicesManager

        @MockK
        lateinit var getSelfUser: GetSelfUserUseCase

        @MockK
        lateinit var currentSessionFlowUseCase: CurrentSessionFlowUseCase

        @MockK
        lateinit var pingRinger: PingRinger

        @MockK
        lateinit var observeE2EIRequired: ObserveE2EIRequiredUseCase

        private val currentSessionChannel = Channel<CurrentSessionResult>(capacity = Channel.UNLIMITED)

        val wireNotificationManager by lazy {
            WireNotificationManager(
                coreLogic,
                currentScreenManager,
                messageNotificationManager,
                callNotificationManager,
                connectionPolicyManager,
                servicesManager,
                dispatcherProvider,
                pingRinger
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { observeE2EIRequired() } returns flowOf(E2EIRequiredResult.NotRequired)
            coEvery { userSessionScope.calls } returns callsScope
            coEvery { userSessionScope.messages } returns messageScope
            coEvery { userSessionScope.syncManager } returns syncManager
            coEvery { userSessionScope.conversations } returns conversationScope
            coEvery { userSessionScope.users } returns userScope
            coEvery { userSessionScope.observeE2EIRequired } returns observeE2EIRequired
            coEvery { conversationScope.markConnectionRequestAsNotified } returns markConnectionRequestAsNotified
            coEvery { userScope.getSelfUser } returns getSelfUser
            coEvery { markConnectionRequestAsNotified(any()) } returns Unit
            coEvery { syncManager.waitUntilLive() } returns Unit
            coEvery { globalKaliumScope.getSessions } returns getSessionsUseCase
            coEvery { coreLogic.getSessionScope(any()) } returns userSessionScope
            coEvery { coreLogic.getGlobalScope() } returns globalKaliumScope
            coEvery { messageNotificationManager.handleNotification(any(), any(), any()) } returns Unit
            coEvery { callsScope.getIncomingCalls } returns getIncomingCallsUseCase
            coEvery { callsScope.establishedCall } returns establishedCall
            coEvery { callsScope.observeOutgoingCall } returns observeOutgoingCall
            coEvery { callNotificationManager.handleIncomingCallNotifications(any(), any()) } returns Unit
            coEvery { callNotificationManager.hideIncomingCallNotification() } returns Unit
            coEvery { callNotificationManager.builder.getNotificationTitle(any()) } returns "Test title"
            coEvery { messageScope.getNotifications } returns getNotificationsUseCase
            coEvery { messageScope.markMessagesAsNotified } returns markMessagesAsNotified
            coEvery { markMessagesAsNotified(any<MarkMessagesAsNotifiedUseCase.UpdateTarget.SingleConversation>()) } returns Result.Success
            coEvery { globalKaliumScope.session.currentSessionFlow } returns currentSessionFlowUseCase
            coEvery { currentSessionFlowUseCase() } returns currentSessionChannel.consumeAsFlow()
            coEvery { getSelfUser.invoke() } returns flowOf(TestUser.SELF_USER)
            every { servicesManager.startOngoingCallService() } returns Unit
            every { servicesManager.stopOngoingCallService() } returns Unit
            every { pingRinger.ping(any(), any()) } returns Unit
            coEvery { globalKaliumScope.doesValidSessionExist.invoke(any()) } returns DoesValidSessionExistResult.Success(true)
        }

        private fun mockSpecificUserSession(
            incomingCalls: List<Call> = emptyList(),
            establishedCalls: List<Call> = emptyList(),
            outgoingCalls: List<Call> = emptyList(),
            notifications: List<LocalNotification> = emptyList(),
            selfUser: SelfUser = TestUser.SELF_USER,
            userId: MockKMatcherScope.() -> UserId,
        ) {
            coEvery { coreLogic.getSessionScope(userId()) } returns mockk {
                coEvery { syncManager } returns this@Arrangement.syncManager
                coEvery { conversations } returns mockk {
                    coEvery { markConnectionRequestAsNotified } returns this@Arrangement.markConnectionRequestAsNotified
                }
                coEvery { calls } returns mockk {
                    coEvery { establishedCall() } returns flowOf(establishedCalls)
                    coEvery { getIncomingCalls() } returns flowOf(incomingCalls)
                    coEvery { observeOutgoingCall() } returns flowOf(outgoingCalls)
                }
                coEvery { messages } returns mockk {
                    coEvery { getNotifications() } returns flowOf(notifications)
                    coEvery { markMessagesAsNotified } returns this@Arrangement.markMessagesAsNotified
                }
                coEvery { users } returns mockk {
                    coEvery { getSelfUser() } returns flowOf(selfUser)
                }
                coEvery { observeE2EIRequired } returns this@Arrangement.observeE2EIRequired
            }
        }

        fun withSession(session: GetAllSessionsResult): Arrangement {
            coEvery { getSessionsUseCase() } returns session
            return this
        }

        suspend fun withCurrentUserSession(session: CurrentSessionResult): Arrangement {
            currentSessionChannel.send(session)
            return this
        }

        fun withMessageNotifications(notifications: List<LocalNotification>): Arrangement {
            coEvery { getNotificationsUseCase() } returns flowOf(notifications)
            return this
        }

        fun withIncomingCalls(calls: List<Call>): Arrangement {
            coEvery { getIncomingCallsUseCase() } returns flowOf(calls)
            return this
        }
        fun withOutgoingCalls(calls: List<Call>): Arrangement {
            coEvery { observeOutgoingCall() } returns flowOf(calls)
            return this
        }

        fun withEstablishedCall(calls: List<Call>): Arrangement {
            coEvery { establishedCall() } returns flowOf(calls)
            return this
        }

        fun withSpecificUserSession(
            userId: UserId,
            incomingCalls: List<Call> = emptyList(),
            establishedCalls: List<Call> = emptyList(),
            outgoingCalls: List<Call> = emptyList(),
            notifications: List<LocalNotification> = emptyList(),
            selfUser: SelfUser = TestUser.SELF_USER,
        ): Arrangement = apply {
            mockSpecificUserSession(incomingCalls, establishedCalls, outgoingCalls, notifications, selfUser) { eq(userId) }
        }

        fun withCurrentScreen(screen: CurrentScreen): Arrangement {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(screen)
            return this
        }

        fun withSelfUser(selfUserFlow: Flow<SelfUser>) = apply {
            coEvery { getSelfUser.invoke() } returns selfUserFlow
        }

        fun withObserveE2EIRequired(result: E2EIRequiredResult) = apply {
            coEvery { observeE2EIRequired.invoke() } returns flowOf(result)
        }

        fun withDoesValidSessionExistResult(userId: UserId, result: DoesValidSessionExistResult) = apply {
            coEvery { globalKaliumScope.doesValidSessionExist.invoke(userId) } returns result
        }

        fun arrange() = this to wireNotificationManager
    }

    companion object {
        private val TEST_SERVER_CONFIG: ServerConfig = newServerConfig(1)
        private val TEST_AUTH_TOKEN = provideAccountInfo()

        private fun provideAccountInfo(userId: String = "user_id"): AccountInfo = AccountInfo.Valid(
            userId = provideUserId(userId)
        )

        private fun provideInvalidAccountInfo(userId: String = "user_id"): AccountInfo = AccountInfo.Invalid(
            userId = provideUserId(userId),
            logoutReason = LogoutReason.SESSION_EXPIRED
        )

        private fun provideCall(id: ConversationId = ConversationId("conversation_value", "conversation_domain")) = Call(
            conversationId = id,
            status = CallStatus.INCOMING,
            callerId = "caller_id",
            participants = listOf(),
            isMuted = false,
            isCameraOn = false,
            maxParticipants = 0,
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.ONE_ON_ONE,
            callerName = "otherUsername",
            callerTeamName = "team_1",
            isCbrEnabled = false
        )

        private fun provideLocalNotificationConversation(
            id: ConversationId = ConversationId("conversation_value", "conversation_domain"),
            messages: List<LocalNotificationMessage> = listOf()
        ) = LocalNotification.Conversation(
            id, "name_${id.value}", messages, true
        )

        private fun provideLocalNotificationUpdateMessage(
            id: ConversationId = ConversationId("conversation_value", "conversation_domain"),
            action: LocalNotificationUpdateMessageAction = LocalNotificationUpdateMessageAction.Edit("new text", "new_name_${id.value}")
        ): LocalNotification.UpdateMessage {
            return LocalNotification.UpdateMessage(id, "name_${id.value}", action)
        }

        private fun provideLocalNotificationMessage(): LocalNotificationMessage = LocalNotificationMessage.Text(
            "message_id", LocalNotificationMessageAuthor("author", null), Instant.DISTANT_FUTURE, "testing text"
        )

        private fun provideLocalNotificationMessagePing(): LocalNotificationMessage = LocalNotificationMessage.Knock(
            messageId = "message_id",
            author = LocalNotificationMessageAuthor("author", null),
            time = Instant.DISTANT_FUTURE
        )

        private fun provideUserId(value: String = "user_id") = UserId(value, "domain")
    }
}
