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

package com.wire.android.util.lifecycle

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.migration.MigrationManager
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.session.SessionRepository
import com.wire.kalium.logic.data.sync.ConnectionPolicy
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.sync.SetConnectionPolicyUseCase
import com.wire.kalium.logic.sync.SyncManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ConnectionPolicyManagerTest {

    @Test
    fun givenCurrentlyActiveSessionAndInitialisedUI_whenHandlingPushNotification_thenShouldNotDowngradePolicy() = runTest {
        val user = USER_ID

        val (arrangement, connectionPolicyManager) = Arrangement()
            .withCurrentSession(user)
            .withAppInTheForeground()
            .arrange()

        connectionPolicyManager.handleConnectionOnPushNotification(user)

        coVerify(exactly = 0) { arrangement.setConnectionPolicyUseCase.invoke(ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS) }
    }

    @Test
    fun givenInitialisedUI_whenHandlingPushNotification_thenShouldUpgradePolicyThenWait() = runTest {
        val user = USER_ID

        val (arrangement, connectionPolicyManager) = Arrangement()
            .withAppInTheForeground()
            .arrange()

        connectionPolicyManager.handleConnectionOnPushNotification(user)

        coVerify(exactly = 1) {
            arrangement.setConnectionPolicyUseCase.invoke(ConnectionPolicy.KEEP_ALIVE)
            arrangement.syncManager.waitUntilLiveOrFailure()
        }
    }

    @Test
    fun givenNotInitialisedUI_whenHandlingPushNotification_thenShouldUpgradeThenWaitThenDowngrade() = runTest {
        val user = USER_ID

        val (arrangement, connectionPolicyManager) = Arrangement()
            .withAppInTheBackground()
            .arrange()

        connectionPolicyManager.handleConnectionOnPushNotification(user)

        coVerify(exactly = 1) {
            arrangement.setConnectionPolicyUseCase.invoke(ConnectionPolicy.KEEP_ALIVE)
            arrangement.syncManager.waitUntilLiveOrFailure()
            arrangement.setConnectionPolicyUseCase.invoke(ConnectionPolicy.DISCONNECT_AFTER_PENDING_EVENTS)
        }
    }

    private class Arrangement {

        @MockK
        lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var setConnectionPolicyUseCase: SetConnectionPolicyUseCase

        @MockK
        lateinit var syncManager: SyncManager

        @MockK
        lateinit var sessionRepository: SessionRepository

        @MockK
        lateinit var migrationManager: MigrationManager

        private val connectionPolicyManager by lazy {
            ConnectionPolicyManager(currentScreenManager, coreLogic, TestDispatcherProvider(), migrationManager)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            every { coreLogic.getGlobalScope().sessionRepository } returns sessionRepository
            every { coreLogic.getSessionScope(USER_ID) } returns userSessionScope
            every { userSessionScope.setConnectionPolicy } returns setConnectionPolicyUseCase
            every { userSessionScope.syncManager } returns syncManager
            coEvery { syncManager.waitUntilLiveOrFailure() } returns Either.Right(Unit)
            every { migrationManager.isMigrationCompletedFlow() } returns flowOf(true)
        }

        fun withAppInTheBackground() = apply {
            every { currentScreenManager.isAppVisibleFlow() } returns MutableStateFlow(false)
        }

        fun withAppInTheForeground() = apply {
            every { currentScreenManager.isAppVisibleFlow() } returns MutableStateFlow(true)
        }

        fun withCurrentSession(userId: UserId) = apply {
            val authSession: AccountInfo = mockk()
            every { authSession.userId } returns userId
            coEvery { sessionRepository.currentSession() } returns Either.Right(authSession)
        }

        fun arrange() = this to connectionPolicyManager
    }

    companion object {
        private val USER_ID = UserId("user", "domain")
        private val USER_ID_2 = UserId("user2", "domain2")
    }
}
