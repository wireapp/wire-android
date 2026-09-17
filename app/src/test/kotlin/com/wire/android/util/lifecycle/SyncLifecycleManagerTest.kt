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
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
import io.mockk.MockKAnnotations
import com.wire.kalium.logic.sync.ForegroundActionsUseCase
import com.wire.kalium.logic.sync.SyncRequest
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.awaitCancellation
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

    @Test
    fun `visibility transitions check registration and release foreground sync`() = runTest {
        val (arrangement, manager) = Arrangement().arrange()
        val job = launch { manager.observeAppLifecycle() }
        advanceUntilIdle()
        coVerify(exactly = 0) { arrangement.foregroundActions.registerMLSClientIfNeeded() }

        arrangement.visibility.value = true
        advanceUntilIdle()
        assertEquals(1, arrangement.syncExecutor.activeRequests)
        coVerify(exactly = 1) { arrangement.foregroundActions.registerMLSClientIfNeeded() }
        arrangement.visibility.value = true
        advanceUntilIdle()
        coVerify(exactly = 1) { arrangement.foregroundActions.registerMLSClientIfNeeded() }

        arrangement.visibility.value = false
        advanceUntilIdle()
        assertEquals(0, arrangement.syncExecutor.activeRequests)
        arrangement.visibility.value = true
        advanceUntilIdle()
        coVerify(exactly = 2) { arrangement.foregroundActions.registerMLSClientIfNeeded() }
        job.cancel()
        advanceUntilIdle()
        assertEquals(0, arrangement.syncExecutor.activeRequests)
    }

    @Test
    fun `registration failure keeps foreground sync active and retries on next foreground`() = runTest {
        val (arrangement, manager) = Arrangement().withAppInTheForeground().arrange()
        coEvery {
            arrangement.foregroundActions.registerMLSClientIfNeeded()
        } throws IllegalStateException("test failure")
        val job = launch { manager.observeAppLifecycle() }
        advanceUntilIdle()
        assertEquals(1, arrangement.syncExecutor.activeRequests)
        arrangement.visibility.value = false
        advanceUntilIdle()
        assertEquals(0, arrangement.syncExecutor.activeRequests)
        coEvery { arrangement.foregroundActions.registerMLSClientIfNeeded() } returns Unit
        arrangement.visibility.value = true
        advanceUntilIdle()
        coVerify(exactly = 2) { arrangement.foregroundActions.registerMLSClientIfNeeded() }
        assertEquals(1, arrangement.syncExecutor.activeRequests)
        job.cancel()
    }

    @Test
    fun `background cancels pending registration check`() = runTest {
        val (arrangement, manager) = Arrangement().withAppInTheForeground().arrange()
        var cancelled = false
        coEvery { arrangement.foregroundActions.registerMLSClientIfNeeded() } coAnswers {
            try {
                awaitCancellation()
            } finally {
                cancelled = true
            }
        }
        val job = launch { manager.observeAppLifecycle() }
        advanceUntilIdle()
        arrangement.visibility.value = false
        advanceUntilIdle()
        assertEquals(true, cancelled)
        assertEquals(0, arrangement.syncExecutor.activeRequests)
        job.cancel()
    }

    @Test
    fun `new valid session while visible checks each account`() = runTest {
        val (arrangement, manager) = Arrangement().withAppInTheForeground().arrange()
        val secondUser = TestUser.SELF_USER_ID.copy(value = "second-user")
        every { arrangement.coreLogic.getSessionScope(secondUser) } returns arrangement.userSessionScope
        val job = launch { manager.observeAppLifecycle() }
        advanceUntilIdle()
        arrangement.sessions.value = GetAllSessionsResult.Success(
            listOf(AccountInfo.Valid(TestUser.SELF_USER_ID), AccountInfo.Valid(secondUser))
        )
        advanceUntilIdle()
        coVerify(exactly = 3) { arrangement.foregroundActions.registerMLSClientIfNeeded() }
        assertEquals(2, arrangement.syncExecutor.activeRequests)
        job.cancel()
    }

    private class TrackingSyncExecutor : FakeSyncExecutor() {
        var activeRequests = 0

        override suspend fun <T> request(executorAction: suspend SyncRequest.() -> T): T {
            activeRequests++
            return try {
                super.request(executorAction)
            } finally {
                activeRequests--
            }
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
        lateinit var observeValidAccountsUseCase: ObserveSessionsUseCase

        @MockK
        lateinit var foregroundActions: ForegroundActionsUseCase

        val visibility = MutableStateFlow(false)
        val sessions = MutableStateFlow<GetAllSessionsResult>(
            GetAllSessionsResult.Success(listOf(AccountInfo.Valid(TestUser.SELF_USER_ID)))
        )
        val syncExecutor = TrackingSyncExecutor()

        private val syncLifecycleManager by lazy {
            SyncLifecycleManager(currentScreenManager, coreLogic)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getGlobalScope().observeAllValidSessionsFlow } returns observeValidAccountsUseCase
            every { coreLogic.getSessionScope(TestUser.SELF_USER_ID) } returns userSessionScope
            coEvery { observeValidAccountsUseCase.invoke() } returns sessions
            every { userSessionScope.users.foregroundActions } returns foregroundActions
            every { currentScreenManager.isAppVisibleFlow() } returns visibility
        }

        fun withAppInTheBackground() = apply {
            visibility.value = false
        }

        fun withAppInTheForeground() = apply {
            visibility.value = true
        }

        fun arrange() = this to syncLifecycleManager.also {
            every { userSessionScope.syncExecutor } returns syncExecutor
        }
    }
}
