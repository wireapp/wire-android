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
package com.wire.android

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.framework.TestUser
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.auth.PersistentWebSocketStatus
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.LogoutCallbackManager
import com.wire.kalium.logic.feature.message.MessageScope
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GlobalObserversManagerTest {

    @Test
    fun `given few valid accounts, when starting observing accounts, then create user-specific notification channels`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), false)
        )
        val accounts = listOf(
            TestUser.SELF_USER,
            TestUser.SELF_USER.copy(id = TestUser.USER_ID.copy(value = "something else"))
        )
        val (arrangement, manager) = Arrangement()
            .withValidAccounts(accounts.map { it to null })
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.notificationChannelsManager.createUserNotificationChannels(accounts) }
        coVerify(exactly = 1) { arrangement.notificationManager.observeNotificationsAndCallsWhileRunning(any(), any()) }
    }

    @Test
    fun `given valid accounts, at least one with persistent socket enabled, and socket service running, then do not start service again`() {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), true)
        )
        val accounts = listOf(
            TestUser.SELF_USER,
            TestUser.SELF_USER.copy(id = TestUser.USER_ID.copy(value = "something else"))
        )
        val (arrangement, manager) = Arrangement()
            .withValidAccounts(accounts.map { it to null })
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()

        manager.observe()

        coVerify(exactly = 1) {
            arrangement.notificationManager.observeNotificationsAndCallsWhileRunning(
                listOf(TestUser.SELF_USER.id),
                any()
            )
        }
    }

    @Test
    fun `given app visible and valid session, when handling ephemeral messages, then call deleteEphemeralMessageEndDate`() {
        val (arrangement, manager) = Arrangement()
            .withCurrentSessionFlow(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
            .withAppVisibleFlow(true)
            .arrange()
        manager.observe()
        coVerify(exactly = 1) { arrangement.messageScope.deleteEphemeralMessageEndDate() }
    }

    @Test
    fun `given app not visible and valid session, when handling ephemeral messages, then do not call deleteEphemeralMessageEndDate`() {
        val (arrangement, manager) = Arrangement()
            .withCurrentSessionFlow(CurrentSessionResult.Success(AccountInfo.Valid(TestUser.SELF_USER.id)))
            .withAppVisibleFlow(false)
            .arrange()
        manager.observe()
        coVerify(exactly = 0) { arrangement.messageScope.deleteEphemeralMessageEndDate() }
    }

    @Test
    fun `given app visible and invalid session, when handling ephemeral messages, then do not call deleteEphemeralMessageEndDate`() {
        val (arrangement, manager) = Arrangement()
            .withCurrentSessionFlow(CurrentSessionResult.Success(AccountInfo.Invalid(TestUser.SELF_USER.id, LogoutReason.DELETED_ACCOUNT)))
            .withAppVisibleFlow(true)
            .arrange()
        manager.observe()
        coVerify(exactly = 0) { arrangement.messageScope.deleteEphemeralMessageEndDate() }
    }

    @Test
    fun `given app visible and no session, when handling ephemeral messages, then do not call deleteEphemeralMessageEndDate`() {
        val (arrangement, manager) = Arrangement()
            .withCurrentSessionFlow(CurrentSessionResult.Failure.SessionNotFound)
            .withAppVisibleFlow(true)
            .arrange()
        manager.observe()
        coVerify(exactly = 0) { arrangement.messageScope.deleteEphemeralMessageEndDate() }
    }

    @Test
    fun `given app visible and session failure, when handling ephemeral messages, then do not call deleteEphemeralMessageEndDate`() {
        val (arrangement, manager) = Arrangement()
            .withCurrentSessionFlow(CurrentSessionResult.Failure.Generic(CoreFailure.Unknown(RuntimeException("error"))))
            .withAppVisibleFlow(true)
            .arrange()
        manager.observe()
        coVerify(exactly = 0) { arrangement.messageScope.deleteEphemeralMessageEndDate() }
    }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var notificationChannelsManager: NotificationChannelsManager

        @MockK
        lateinit var notificationManager: WireNotificationManager

        @MockK
        lateinit var userDataStoreProvider: UserDataStoreProvider

        @MockK
        lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        lateinit var logoutCallbackManager: LogoutCallbackManager

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var messageScope: MessageScope

        private val manager by lazy {
            GlobalObserversManager(
                dispatcherProvider = TestDispatcherProvider(),
                coreLogic = coreLogic,
                notificationChannelsManager = notificationChannelsManager,
                notificationManager = notificationManager,
                userDataStoreProvider = userDataStoreProvider,
                currentScreenManager = currentScreenManager,
            )
        }

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            mockUri()
            every { notificationChannelsManager.createUserNotificationChannels(any()) } returns Unit
            every { coreLogic.getGlobalScope().logoutCallbackManager } returns logoutCallbackManager
            every { coreLogic.getSessionScope(any()) } returns userSessionScope
            every { userSessionScope.messages } returns messageScope
            coEvery { messageScope.deleteEphemeralMessageEndDate() } returns Unit
            withPersistentWebSocketConnectionStatuses(emptyList())
            withValidAccounts(emptyList())
            withCurrentSessionFlow(CurrentSessionResult.Failure.SessionNotFound)
            withAppVisibleFlow(true)
        }

        fun withValidAccounts(list: List<Pair<SelfUser, Team?>>): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().observeValidAccounts() } returns flowOf(list)
        }

        fun withPersistentWebSocketConnectionStatuses(list: List<PersistentWebSocketStatus>): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(flowOf(list))
        }

        fun withCurrentSessionFlow(result: CurrentSessionResult): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().session.currentSessionFlow() } returns flowOf(result)
        }

        fun withAppVisibleFlow(isVisible: Boolean) = apply {
            coEvery { currentScreenManager.isAppVisibleFlow() } returns MutableStateFlow(isVisible)
        }

        fun arrange() = this to manager
    }
}
