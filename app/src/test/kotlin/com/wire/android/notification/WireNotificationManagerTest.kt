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
 *
 *
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
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.notification.LocalNotificationConversation
import com.wire.kalium.logic.data.notification.LocalNotificationMessage
import com.wire.kalium.logic.data.notification.LocalNotificationMessageAuthor
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.call.Call
import com.wire.kalium.logic.feature.call.CallStatus
import com.wire.kalium.logic.feature.call.CallsScope
import com.wire.kalium.logic.feature.call.usecase.GetIncomingCallsUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.connection.MarkConnectionRequestAsNotifiedUseCase
import com.wire.kalium.logic.feature.conversation.ConversationScope
import com.wire.kalium.logic.feature.message.GetNotificationsUseCase
import com.wire.kalium.logic.feature.message.MarkMessagesAsNotifiedUseCase
import com.wire.kalium.logic.feature.message.MessageScope
import com.wire.kalium.logic.feature.message.Result
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.UserScope
import com.wire.kalium.logic.sync.SyncManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class WireNotificationManagerTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun givenNotAuthenticatedUser_whenFetchAndShowNotificationsOnceCalled_thenNothingHappen() = runTest(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement().withSession(GetAllSessionsResult.Failure.NoSessionFound)
            .withCurrentUserSession(provideCurrentInvalidUserSession()).arrange()

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
            val (arrangement, manager) = Arrangement().withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN)))
                .withCurrentUserSession(provideCurrentValidUserSession()).withMessageNotifications(listOf()).withIncomingCalls(listOf())
                .arrange()

            manager.fetchAndShowNotificationsOnce("user_id")
            advanceUntilIdle()

            verify(atLeast = 1) { arrangement.coreLogic.getSessionScope(any()) }
            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(TEST_AUTH_TOKEN.userId) }
            verify(exactly = 1) { arrangement.callNotificationManager.handleIncomingCallNotifications(listOf(), any()) }
        }

    @Test
    fun givenNotAuthenticatedUser_whenObserveCalled_thenNothingHappenAndCallNotificationHides() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement().withCurrentScreen(CurrentScreen.SomeOther).arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(), this) {}
            advanceUntilIdle()

            verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
            verify(exactly = 1) { arrangement.callNotificationManager.hideAllNotifications() }
        }

    @Test
    fun givenNoIncomingCalls_whenObserveCalled_thenCallNotificationHides() = runTestWithCancellation(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement()
            .withIncomingCalls(listOf())
            .withMessageNotifications(listOf())
            .withCurrentScreen(CurrentScreen.SomeOther)
            .withCurrentUserSession(CurrentSessionResult.Success(AccountInfo.Valid(provideUserId())))
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
        runCurrent()

        verify(exactly = 1) { arrangement.callNotificationManager.hideIncomingCallNotification() }
    }

    @Test
    fun givenSomeIncomingCall_whenCurrentUserIsDifferentFromCallReceiver_thenCallNotificationIsShown() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement()
                .withIncomingCalls(listOf())
                .withMessageNotifications(listOf())
                .withCurrentScreen(CurrentScreen.SomeOther)
                .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
            runCurrent()

            verify(exactly = 1) { arrangement.callNotificationManager.handleIncomingCallNotifications(any(), any()) }
        }

    @Test
    fun givenSomeIncomingCalls_whenAppIsNotVisible_thenCallNotificationHidden() = runTestWithCancellation(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement().withIncomingCalls(listOf(provideCall()))
            .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
            .withMessageNotifications(listOf())
            .withCurrentScreen(CurrentScreen.InBackground)
            .withEstablishedCall(listOf())
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
        runCurrent()

        verify(exactly = 0) { arrangement.callNotificationManager.hideIncomingCallNotification() }
        verify(exactly = 1) { arrangement.callNotificationManager.handleIncomingCallNotifications(any(), any()) }
    }

    @Test
    fun givenSomeIncomingCalls_whenAppIsVisible_thenCallNotificationShowed() = runTestWithCancellation(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement().withIncomingCalls(listOf(provideCall()))
            .withCurrentUserSession(CurrentSessionResult.Success(AccountInfo.Valid(provideUserId())))
            .withCurrentScreen(CurrentScreen.SomeOther)
            .withMessageNotifications(listOf())
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
        runCurrent()

        verify(exactly = 1) { arrangement.callNotificationManager.hideIncomingCallNotification() }
        verify(exactly = 0) { arrangement.callNotificationManager.handleIncomingCallNotifications(any(), any()) }
    }

    @Test
    fun givenSomeNotifications_whenAppIsInForegroundAndNoUserLoggedIn_thenMessageNotificationNotShowed() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement().withIncomingCalls(listOf(provideCall())).withMessageNotifications(
                listOf(
                    provideLocalNotificationConversation(
                        messages = listOf(provideLocalNotificationMessage())
                    )
                )
            ).withCurrentScreen(CurrentScreen.SomeOther).arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(), this) {}
            runCurrent()

            verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
            verify(exactly = 0) {
                arrangement.messageNotificationManager.handleNotification(
                    newNotifications = listOf(), userId = any(), userName = TestUser.SELF_USER.handle!!
                )
            }
            verify(exactly = 1) { arrangement.callNotificationManager.hideAllNotifications() }
        }

    @Test
    fun givenSomeNotifications_whenAppIsInBackgroundAndNoUserLoggedIn_thenMessageNotificationNotShowed() =
        runTestWithCancellation(dispatcherProvider.main()) {
            val (arrangement, manager) = Arrangement().withIncomingCalls(listOf(provideCall())).withMessageNotifications(
                listOf(provideLocalNotificationConversation(messages = listOf(provideLocalNotificationMessage())))
            ).withCurrentScreen(CurrentScreen.InBackground).arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(), this) {}
            runCurrent()

            verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
            verify(exactly = 0) {
                arrangement.messageNotificationManager.handleNotification(
                    listOf(), any(), TestUser.SELF_USER.handle!!
                )
            }
            verify(exactly = 1) { arrangement.callNotificationManager.hideAllNotifications() }
        }

    @Test
    fun givenSomeNotifications_whenObserveCalled_thenCallNotificationShowed() = runTestWithCancellation(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement()
            .withMessageNotifications(
                listOf(
                    provideLocalNotificationConversation(
                        messages = listOf(provideLocalNotificationMessage())
                    )
                )
            )
            .withIncomingCalls(listOf())
            .withCurrentScreen(CurrentScreen.SomeOther)
            .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
        runCurrent()

        verify(exactly = 1) {
            arrangement.messageNotificationManager.handleNotification(
                any(), any(), TestUser.SELF_USER.handle!!
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
                        provideLocalNotificationConversation(id = conversationId, messages = listOf(provideLocalNotificationMessage()))
                    )
                ).withIncomingCalls(listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
                .withCurrentScreen(CurrentScreen.Conversation(conversationId))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
            runCurrent()

            verify(exactly = 1) {
                arrangement.messageNotificationManager.handleNotification(
                    listOf(), any(), TestUser.SELF_USER.handle!!
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
                        provideLocalNotificationConversation(id = conversationId, messages = listOf(provideLocalNotificationMessage()))
                    )
                )
                .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
                .withEstablishedCall(listOf())
                .withIncomingCalls(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .withSelfUser(selfUserFlow)
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
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
            val (arrangement, manager) = Arrangement().withMessageNotifications(listOf())
                .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
                .withIncomingCalls(listOf())
                .withCurrentScreen(CurrentScreen.Conversation(conversationId))
                .arrange()

            manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
            runCurrent()

            coVerify(atLeast = 1) { arrangement.messageNotificationManager.hideNotification(conversationId, provideUserId()) }
        }

    @Test
    fun givenASingleUserId_whenCallingFetchAndShowOnceInParallel_thenPushNotificationIsHandledOnlyOnce() =
        runTest(dispatcherProvider.main()) {
            val userId = TEST_AUTH_TOKEN.userId
            val (arrangement, manager) = Arrangement().withMessageNotifications(listOf())
                .withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN))).withCurrentUserSession(provideCurrentValidUserSession())
                .withIncomingCalls(listOf()).withCurrentScreen(CurrentScreen.InBackground).arrange()

            coEvery { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId) } coAnswers {
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
            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId) }

            // After second call, should have handled push once
            advanceTimeBy(6.minutes.inWholeMilliseconds)
            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId) }

            // After everything ends, should have handled push once
            advanceUntilIdle()
            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId) }
        }

    @Test
    fun givenASingleUserId_whenNotificationReceivedAndNoSuchUser_shouldSkipNotification() = runTest(dispatcherProvider.main()) {
        val otherAuthSession = provideAccountInfo("other_id")
        val userId = otherAuthSession.userId
        val (arrangement, manager) = Arrangement().withMessageNotifications(listOf())
            .withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN)))
            .withCurrentUserSession(provideCurrentValidUserSession(TEST_AUTH_TOKEN))
            .withIncomingCalls(listOf())
            .withCurrentScreen(CurrentScreen.InBackground)
            .arrange()

        manager.fetchAndShowNotificationsOnce(userId.value)
        advanceUntilIdle()

        coVerify(exactly = 0) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId) }
    }

    @Test
    fun givenASingleUserId_whenNotificationReceivedAndNotCurrentUserButExistOnThatDevice_shouldCheckNotification() =
        runTest(dispatcherProvider.main()) {
            val otherAuthSession = provideAccountInfo("other_id")
            val userId = otherAuthSession.userId
            val (arrangement, manager) = Arrangement().withMessageNotifications(listOf())
                .withSession(GetAllSessionsResult.Success(listOf(TEST_AUTH_TOKEN, otherAuthSession)))
                .withCurrentUserSession(provideCurrentValidUserSession(TEST_AUTH_TOKEN))
                .withIncomingCalls(listOf())
                .withCurrentScreen(CurrentScreen.InBackground)
                .arrange()

            manager.fetchAndShowNotificationsOnce(userId.value)
            advanceUntilIdle()

            coVerify(exactly = 1) { arrangement.connectionPolicyManager.handleConnectionOnPushNotification(userId) }
        }

    @Test
    fun givenSomeEstablishedCalls_whenAppIsNotVisible_thenOngoingCallServiceRun() = runTestWithCancellation(dispatcherProvider.main()) {
        val (arrangement, manager) = Arrangement()
            .withIncomingCalls(listOf())
            .withMessageNotifications(listOf())
            .withCurrentScreen(CurrentScreen.InBackground).withEstablishedCall(
                listOf(provideCall().copy(status = CallStatus.ESTABLISHED))
            )
            .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
        runCurrent()

        verify(exactly = 1) { arrangement.servicesManager.startOngoingCallService(any(), any(), any()) }
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
            .withCurrentUserSession(CurrentSessionResult.Success(TEST_AUTH_TOKEN))
            .withIncomingCalls(listOf())
            .withCurrentScreen(CurrentScreen.Conversation(id = conversationId))
            .arrange()

        manager.observeNotificationsAndCallsWhileRunning(listOf(provideUserId()), this) {}
        runCurrent()

        verify(exactly = 1) {
            arrangement.pingRinger.ping(
                resource = any(),
                isReceivingPing = any()
            )
        }
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
        lateinit var establishedCall: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var getSessionsUseCase: GetSessionsUseCase

        @MockK
        lateinit var servicesManager: ServicesManager

        @MockK
        lateinit var currentSessionUseCase: CurrentSessionUseCase

        @MockK
        lateinit var getSelfUser: GetSelfUserUseCase

        @MockK
        lateinit var pingRinger: PingRinger

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

            coEvery { userSessionScope.calls } returns callsScope
            coEvery { userSessionScope.messages } returns messageScope
            coEvery { userSessionScope.syncManager } returns syncManager
            coEvery { userSessionScope.conversations } returns conversationScope
            coEvery { userSessionScope.users } returns userScope
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
            coEvery { callNotificationManager.handleIncomingCallNotifications(any(), any()) } returns Unit
            coEvery { callNotificationManager.hideIncomingCallNotification() } returns Unit
            coEvery { callNotificationManager.getNotificationTitle(any()) } returns "Test title"
            coEvery { messageScope.getNotifications } returns getNotificationsUseCase
            coEvery { messageScope.markMessagesAsNotified } returns markMessagesAsNotified
            coEvery { markMessagesAsNotified(any<MarkMessagesAsNotifiedUseCase.UpdateTarget.SingleConversation>()) } returns Result.Success
            coEvery { globalKaliumScope.session.currentSession } returns currentSessionUseCase
            coEvery { getSelfUser.invoke() } returns flowOf(TestUser.SELF_USER)
            every { servicesManager.startOngoingCallService(any(), any(), any()) } returns Unit
            every { servicesManager.stopOngoingCallService() } returns Unit
            every { pingRinger.ping(any(), any()) } returns Unit
        }

        fun withSession(session: GetAllSessionsResult): Arrangement {
            coEvery { getSessionsUseCase() } returns session
            return this
        }

        fun withCurrentUserSession(session: CurrentSessionResult): Arrangement {
            coEvery { currentSessionUseCase() } returns session
            return this
        }

        fun withMessageNotifications(notifications: List<LocalNotificationConversation>): Arrangement {
            coEvery { getNotificationsUseCase() } returns flowOf(notifications)
            return this
        }

        fun withIncomingCalls(calls: List<Call>): Arrangement {
            coEvery { getIncomingCallsUseCase() } returns flowOf(calls)
            return this
        }

        fun withEstablishedCall(calls: List<Call>): Arrangement {
            coEvery { establishedCall() } returns flowOf(calls)
            return this
        }

        fun withCurrentScreen(screen: CurrentScreen): Arrangement {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(screen)
            return this
        }

        fun withSelfUser(selfUserFlow: Flow<SelfUser>) = apply {
            coEvery { getSelfUser.invoke() } returns selfUserFlow
        }

        fun arrange() = this to wireNotificationManager
    }

    companion object {
        private val TEST_SERVER_CONFIG: ServerConfig = newServerConfig(1)
        private val TEST_AUTH_TOKEN = provideAccountInfo()

        private fun provideAccountInfo(userId: String = "user_id"): AccountInfo {
            return AccountInfo.Valid(
                userId = UserId(userId, "domain.de")
            )
        }

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
            callerTeamName = "team_1"
        )

        private fun provideLocalNotificationConversation(
            id: ConversationId = ConversationId("conversation_value", "conversation_domain"),
            messages: List<LocalNotificationMessage> = listOf()
        ) = LocalNotificationConversation(
            id, "name_${id.value}", messages, true
        )

        private fun provideLocalNotificationMessage(): LocalNotificationMessage = LocalNotificationMessage.Text(
            LocalNotificationMessageAuthor("author", null), Instant.DISTANT_FUTURE, "testing text"
        )

        private fun provideLocalNotificationMessagePing(): LocalNotificationMessage = LocalNotificationMessage.Knock(
            author = LocalNotificationMessageAuthor("author", null),
            time = Instant.DISTANT_FUTURE
        )

        private fun provideUserId() = UserId("value", "domain")

        private fun appVisibleFlow() = MutableStateFlow(true)
        private fun appInvisibleFlow() = MutableStateFlow(false)

        private fun provideCurrentValidUserSession(authSession: AccountInfo = TEST_AUTH_TOKEN) = CurrentSessionResult.Success(authSession)

        private fun provideCurrentInvalidUserSession() = CurrentSessionResult.Failure.SessionNotFound
    }
}
