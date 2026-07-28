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

package com.wire.android.ui

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.startup.KaliumStartup
import com.wire.kalium.logic.startup.StartupHandle
import com.wire.kalium.logic.startup.StartupResult
import com.wire.kalium.logic.startup.StartupState
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Duration.Companion.milliseconds

@ExtendWith(CoroutineTestExtension::class)
class SessionStartupViewModelTest {

    @Test
    fun givenSessionStartupFinishesBeforeRevealDelay_whenPreparing_thenBlockingMigrationIsNeverShown() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val handle = ControllableStartupHandle()
        val viewModel = createViewModel(handle, dispatcher)
        runCurrent()

        advanceTimeBy(50)
        handle.complete()
        runCurrent()

        assertEquals(SessionStartupUiState.Ready(TestUser.USER_ID), viewModel.state.value)
    }

    @Test
    fun givenSessionStartupRunsPastRevealDelay_whenPreparing_thenBlockingMigrationIsShown() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val handle = ControllableStartupHandle()
        val viewModel = createViewModel(handle, dispatcher)
        runCurrent()

        advanceTimeBy(749)
        runCurrent()
        assertFalse((viewModel.state.value as SessionStartupUiState.Working).showBlockingMigration)

        advanceTimeBy(1)
        runCurrent()
        assertTrue((viewModel.state.value as SessionStartupUiState.Working).showBlockingMigration)
    }

    @Test
    fun givenBlockingMigrationIsShown_whenStartupCompletes_thenMinimumVisibleDurationIsKept() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val handle = ControllableStartupHandle()
        val viewModel = createViewModel(handle, dispatcher)
        runCurrent()

        advanceTimeBy(750)
        runCurrent()
        handle.complete()
        runCurrent()

        advanceTimeBy(499)
        runCurrent()
        assertTrue(viewModel.state.value is SessionStartupUiState.Working)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(SessionStartupUiState.Ready(TestUser.USER_ID), viewModel.state.value)
    }

    private fun kotlinx.coroutines.test.TestScope.createViewModel(
        handle: StartupHandle<UserSessionScope>,
        dispatcher: TestDispatcher,
    ): SessionStartupViewModel {
        val startup = object : KaliumStartup {
            override fun session(userId: UserId): StartupHandle<UserSessionScope> = handle
        }
        return SessionStartupViewModel(
            startup = startup,
            resolveCurrentSession = {
                CurrentSessionResult.Success(AccountInfo.Valid(TestUser.USER_ID))
            },
            dispatchers = TestDispatcherProvider(dispatcher),
            presentationPolicy = BlockingWorkPresentationPolicy(
                revealDelay = 750.milliseconds,
                minimumVisibleDuration = 500.milliseconds,
            ),
            elapsedRealtime = { testScheduler.currentTime },
        )
    }

    private class ControllableStartupHandle : StartupHandle<UserSessionScope> {
        private val completion = CompletableDeferred<Unit>()
        private val stateMutable = MutableStateFlow<StartupState>(StartupState.NotStarted)
        private val sessionScope = mockk<UserSessionScope>()

        override val state: StateFlow<StartupState> = stateMutable

        override suspend fun open(): StartupResult<UserSessionScope> {
            stateMutable.value = StartupState.Opening
            completion.await()
            stateMutable.value = StartupState.Ready
            return StartupResult.Success(sessionScope)
        }

        override suspend fun retry(): StartupResult<UserSessionScope> = open()

        override fun readyOrNull(): UserSessionScope? =
            sessionScope.takeIf { stateMutable.value is StartupState.Ready }

        fun complete() {
            completion.complete(Unit)
        }
    }
}
