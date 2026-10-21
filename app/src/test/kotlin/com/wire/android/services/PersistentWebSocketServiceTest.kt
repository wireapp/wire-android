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

import android.app.Application
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.fake.FakeSyncExecutor
import com.wire.android.notification.WireNotificationManager
import com.wire.android.session.FakeSessionAdmission
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.data.auth.PersistentWebSocketStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [36])
@OptIn(ExperimentalCoroutinesApi::class)
class PersistentWebSocketServiceTest {

    @Test
    fun `service keeps ready B syncing while A waits and drains each captured revision`() = runTest {
        with(Arrangement(StandardTestDispatcher(testScheduler))) {
            // Exercise onStartCommand directly: injection and foreground notification are separate framework concerns.
            PersistentWebSocketService.isServiceStarted = true
            try {
                service.onStartCommand(null, 0, 1)
                runCurrent()
                assertEquals(0, firstA.requestCount)
                assertEquals(1, readyB.activeRequestCount)
                admission.publish(USER_A, sessionA)
                runCurrent()
                assertEquals(1, firstA.activeRequestCount)
                admission.publish(USER_A, null)
                runCurrent()
                assertEquals(0, firstA.activeRequestCount)
                assertTrue(sessionA in admission.drained)
                assertEquals(1, readyB.activeRequestCount)
                admission.publish(USER_A, replacementA)
                runCurrent()
                assertEquals(1, secondA.activeRequestCount)
                assertEquals(1, readyB.requestCount)
                statuses.value = listOf(PersistentWebSocketStatus(USER_B, true))
                runCurrent()
                assertEquals(0, secondA.activeRequestCount)
                admission.publish(USER_A, sessionA)
                runCurrent()
                assertEquals(1, firstA.requestCount)
                assertEquals(1, readyB.activeRequestCount)
                verify(exactly = 0) { core.getSessionScope(any()) }
            } finally {
                service.onDestroy()
                runCurrent()
            }
            assertEquals(0, readyB.activeRequestCount)
            assertEquals(0, secondA.activeRequestCount)
        }
    }

    private class Arrangement(dispatcher: CoroutineDispatcher) {
        val core = mockk<CoreLogic>()
        private val global = mockk<GlobalKaliumScope>()
        private val notifications = mockk<WireNotificationManager>()
        val admission = FakeSessionAdmission(core)
        val statuses = MutableStateFlow(listOf(PersistentWebSocketStatus(USER_A, true), PersistentWebSocketStatus(USER_B, true)))
        val firstA = FakeSyncExecutor()
        val secondA = FakeSyncExecutor()
        val readyB = FakeSyncExecutor()
        val sessionA = session(firstA)
        val replacementA = session(secondA)
        val service = Robolectric.buildService(PersistentWebSocketService::class.java).get().apply {
            coreLogic = core
            notificationManager = notifications
            dispatcherProvider = TestDispatcherProvider(dispatcher)
        }

        init {
            every { core.getGlobalScope() } returns global
            coEvery { global.observePersistentWebSocketConnectionStatus() } returns
                ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(statuses)
            coEvery { notifications.observeNotificationsAndCallsPersistently(any(), any()) } returns Unit
            admission.publish(USER_B, session(readyB))
        }

        private fun session(executor: FakeSyncExecutor): UserSessionScope = mockk {
            every { syncExecutor } returns executor
        }
    }

    private companion object {
        val USER_A = UserId("A", "wire.com")
        val USER_B = UserId("B", "wire.com")
    }
}
