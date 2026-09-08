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
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.lifecycle.SyncLifecycleManager
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.sync.SyncState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getSessionScope(USER_ID) } returns userSessionScope
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
