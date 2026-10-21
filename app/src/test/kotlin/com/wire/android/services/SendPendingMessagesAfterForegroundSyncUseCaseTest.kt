/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.android.framework.fake.FakeSyncExecutor
import com.wire.android.session.FakeSessionAdmission
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.lifecycle.SyncLifecycleManager
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.sync.SendPendingMessagesUseCase
import com.wire.kalium.logic.sync.SyncRequestResult
import com.wire.kalium.logic.sync.SyncStateObserver
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Suppress("DEPRECATION")
class SendPendingMessagesAfterForegroundSyncUseCaseTest {

    @Test
    fun `given next sync reaches live when sending pending messages then sends inside a single temporary sync request`() = runTest {
        val (arrangement, useCase) = Arrangement()
            .arrange()

        useCase(USER_ID)

        assertEquals(1, arrangement.syncExecutor.requestCount)
        assertEquals(0, arrangement.syncExecutor.waitUntilLiveCount)
        assertEquals(1, arrangement.syncExecutor.waitUntilNextLiveCount)
        coVerify(exactly = 1) { arrangement.sendPendingMessages() }
    }

    @Test
    fun `given next sync fails when sending pending messages then does not send`() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withNextSyncRequestResult(SyncRequestResult.Failure(CoreFailure.Unknown(RuntimeException("sync failed"))))
            .arrange()

        useCase(USER_ID)

        assertEquals(1, arrangement.syncExecutor.requestCount)
        assertEquals(0, arrangement.syncExecutor.waitUntilLiveCount)
        assertEquals(1, arrangement.syncExecutor.waitUntilNextLiveCount)
        coVerify(exactly = 0) { arrangement.sendPendingMessages() }
    }

    @Test
    fun `early metadata waits and revocation drains captured send without replay`() = runTest {
        val entered = CompletableDeferred<Unit>()
        val drained = CompletableDeferred<Unit>()
        val (arrangement, useCase) = Arrangement().arrange()
        arrangement.admission.publish(USER_ID, null)
        coEvery { arrangement.sendPendingMessages() } coAnswers {
            entered.complete(Unit)
            try { awaitCancellation() } finally { drained.complete(Unit) }
        }
        val request = async {
            useCase(USER_ID)
        }
        runCurrent()
        assertEquals(0, arrangement.syncExecutor.requestCount)
        arrangement.admission.publish(USER_ID, arrangement.userSessionScope)
        entered.await()
        arrangement.admission.publish(USER_ID, null)
        runCurrent()
        assertTrue(drained.isCompleted)
        arrangement.admission.publish(USER_ID, arrangement.userSessionScope)
        request.await()
        assertEquals(1, arrangement.syncExecutor.requestCount)
        coVerify(exactly = 1) { arrangement.sendPendingMessages() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `raw invalidation cancels waiting send and does not acquire on later publication`() = runTest {
        val (arrangement, useCase) = Arrangement().arrange()
        arrangement.admission.publish(USER_ID, null)
        val request = async {
            useCase(USER_ID)
        }
        runCurrent()
        arrangement.rawAccounts.value = GetAllSessionsResult.Success(emptyList())
        request.await()
        arrangement.admission.publish(USER_ID, arrangement.userSessionScope)
        runCurrent()
        assertEquals(0, arrangement.syncExecutor.requestCount)
        coVerify(exactly = 0) { arrangement.sendPendingMessages() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `captured send failure propagates unchanged`() = runTest {
        val (arrangement, useCase) = Arrangement().arrange()
        val failure = IllegalStateException("send failed")
        coEvery { arrangement.sendPendingMessages() } throws failure
        val observed = try {
            useCase(USER_ID)
            error("expected failure")
        } catch (actual: IllegalStateException) {
            actual
        }
        assertSame(failure, generateSequence<Throwable>(observed) { it.cause }.last())
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `caller cancellation while waiting is not converted to successful send`() = runTest {
        val (arrangement, useCase) = Arrangement().arrange()
        arrangement.admission.publish(USER_ID, null)
        val request = async {
            useCase(USER_ID)
        }
        runCurrent()
        request.cancel()
        request.join()
        assertTrue(request.isCancelled)
        assertEquals(0, arrangement.syncExecutor.requestCount)
        coVerify(exactly = 0) { arrangement.sendPendingMessages() }
    }

    private class Arrangement {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var syncStateObserver: SyncStateObserver

        @MockK
        lateinit var sendPendingMessages: SendPendingMessagesUseCase

        var syncExecutor = FakeSyncExecutor()
        val rawAccounts = MutableStateFlow<GetAllSessionsResult>(GetAllSessionsResult.Success(listOf(AccountInfo.Valid(USER_ID))))
        val admission by lazy { FakeSessionAdmission(coreLogic) { userSessionScope } }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            admission
            coEvery { coreLogic.getGlobalScope().observeAllValidSessionsFlow() } returns rawAccounts
            every { userSessionScope.syncManager } returns syncStateObserver
            every { syncStateObserver.syncState } returns MutableStateFlow(SyncState.Live)
            every { userSessionScope.sendPendingMessages } returns sendPendingMessages
            coEvery { sendPendingMessages() } returns SendPendingMessagesUseCase.Result.Success
        }

        fun withNextSyncRequestResult(syncRequestResult: SyncRequestResult) = apply {
            syncExecutor = object : FakeSyncExecutor() {
                override fun onWaitUntilNextLiveOrFailure(): SyncRequestResult =
                    syncRequestResult.also { waitUntilNextLiveCount++ }
            }
        }

        fun arrange(): Pair<Arrangement, SendPendingMessagesAfterForegroundSyncUseCase> {
            every { userSessionScope.syncExecutor } returns syncExecutor
            val syncLifecycleManager = SyncLifecycleManager(currentScreenManager, coreLogic)
            return this to SendPendingMessagesAfterForegroundSyncUseCase(coreLogic, syncLifecycleManager)
        }
    }

    private companion object {
        private val USER_ID = UserId("currentUser", "wire.com")
    }
}
