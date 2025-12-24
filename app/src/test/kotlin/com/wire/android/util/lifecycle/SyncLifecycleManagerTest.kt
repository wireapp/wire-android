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

import com.wire.android.framework.TestUser
import com.wire.android.framework.fake.FakeSyncExecutor
import com.wire.android.util.CurrentScreenManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncLifecycleManagerTest {

    @Test
    fun givenCurrentlyActiveSessionAndInitialisedUI_whenHandlingPushNotification_thenShouldIncreaseSyncRequestAndWaitUntilLive() = runTest {
        val (arrangement, connectionPolicyManager) = Arrangement()
            .withAppInTheForeground()
            .arrange()

        connectionPolicyManager.syncTemporarily(TestUser.SELF_USER_ID)

        assertEquals(1, arrangement.syncExecutor.requestCount)
        assertEquals(1, arrangement.syncExecutor.waitUntilLiveCount)
    }

    @Test
    fun givenUINotInitialised_whenObservingLifecycle_thenShouldNotIncreaseSyncRequest() = runTest {
        val (arrangement, syncLifecycleManager) = Arrangement()
            .withAppInTheBackground()
            .arrange()

        val observingLifecycleJob = launch {
            syncLifecycleManager.observeAppLifecycle()
        }
        advanceUntilIdle()
        observingLifecycleJob.cancel()

        assertEquals(0, arrangement.syncExecutor.requestCount)
    }

    @Test
    fun givenUIInitialised_whenObservingLifecycle_thenShouldIncreaseSyncRequest() = runTest {
        val (arrangement, syncLifecycleManager) = Arrangement()
            .withAppInTheForeground()
            .arrange()

        val observingLifecycleJob = launch {
            syncLifecycleManager.observeAppLifecycle()
        }
        advanceUntilIdle()
        observingLifecycleJob.cancel()
        assertEquals(1, arrangement.syncExecutor.requestCount)
    }

    @Test
    fun givenUIInitialised_whenObservingLifecycleAndRequestingTemporarySync_thenShouldIncreaseSyncRequestTwiceAndWaitOnce() = runTest {
        val (arrangement, syncLifecycleManager) = Arrangement()
            .withAppInTheForeground()
            .arrange()

        val observingLifecycleJob = launch {
            syncLifecycleManager.observeAppLifecycle()
        }
        syncLifecycleManager.syncTemporarily(TestUser.SELF_USER_ID)
        advanceUntilIdle()
        observingLifecycleJob.cancel()

        assertEquals(2, arrangement.syncExecutor.requestCount)
        assertEquals(1, arrangement.syncExecutor.waitUntilLiveCount)
    }

    private class Arrangement {

        @MockK
        lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var observeValidAccountsUseCase: ObserveValidAccountsUseCase

        var syncExecutor = FakeSyncExecutor()

        private val syncLifecycleManager by lazy {
            SyncLifecycleManager(currentScreenManager, coreLogic)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getGlobalScope().observeValidAccounts } returns observeValidAccountsUseCase
            every { coreLogic.getSessionScope(TestUser.SELF_USER_ID) } returns userSessionScope
            coEvery { observeValidAccountsUseCase.invoke() } returns flowOf(
                listOf(
                    TestUser.SELF_USER to null
                )
            )
        }

        fun withAppInTheBackground() = apply {
            every { currentScreenManager.isAppVisibleFlow() } returns MutableStateFlow(false)
        }

        fun withAppInTheForeground() = apply {
            every { currentScreenManager.isAppVisibleFlow() } returns MutableStateFlow(true)
        }

        fun arrange() = this to syncLifecycleManager.also {
            every { userSessionScope.syncExecutor } returns syncExecutor
        }
    }
}
