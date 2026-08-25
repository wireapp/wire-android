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
package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var snapshotWriteObserver: ObserverHandle

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        snapshotWriteObserver = Snapshot.registerGlobalWriteObserver {
            Snapshot.sendApplyNotifications()
        }
    }

    @AfterEach
    fun tearDown() {
        snapshotWriteObserver.dispose()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state preserves generic links and default session flags`() = runTest(dispatcher) {
        val links = TestLinks("custom")
        val viewModel = WelcomeViewModel(links, FakeGateway(WelcomeSessionResult.Unavailable))

        assertEquals(WelcomeScreenState(links), viewModel.state)
    }

    @Test
    fun `nomad result blocks login without changing other defaults`() = runTest(dispatcher) {
        val viewModel = arrange(WelcomeSessionResult.NomadAccountBlocksLogin)

        advanceUntilIdle()

        assertEquals(
            WelcomeScreenState(TestLinks(), nomadAccountBlocksLogin = true),
            viewModel.state,
        )
    }

    @Test
    fun `sessions result maps active and maximum account flags`() = runTest(dispatcher) {
        listOf(
            WelcomeSessionResult.Sessions(validSessionCount = 0, maxAccounts = 4) to
                WelcomeScreenState(TestLinks()),
            WelcomeSessionResult.Sessions(validSessionCount = 2, maxAccounts = 4) to
                WelcomeScreenState(TestLinks(), isThereActiveSession = true),
            WelcomeSessionResult.Sessions(validSessionCount = 4, maxAccounts = 4) to
                WelcomeScreenState(TestLinks(), isThereActiveSession = true, maxAccountsReached = true),
        ).forEach { (result, expected) ->
            val viewModel = arrange(result)
            advanceUntilIdle()
            assertEquals(expected, viewModel.state)
        }
    }

    @Test
    fun `no session result keeps active session false`() = runTest(dispatcher) {
        val viewModel = arrange(WelcomeSessionResult.NoSessionFound)

        advanceUntilIdle()

        assertEquals(WelcomeScreenState(TestLinks()), viewModel.state)
    }

    @Test
    fun `unavailable result silently preserves the default state`() = runTest(dispatcher) {
        val viewModel = arrange(WelcomeSessionResult.Unavailable)

        advanceUntilIdle()

        assertEquals(WelcomeScreenState(TestLinks()), viewModel.state)
    }

    private fun arrange(result: WelcomeSessionResult): WelcomeViewModel<TestLinks> =
        WelcomeViewModel(TestLinks(), FakeGateway(result))

    private data class TestLinks(val name: String = "default")

    private class FakeGateway(private val result: WelcomeSessionResult) : WelcomeSessionGateway {
        override suspend fun loadSessions(): WelcomeSessionResult = result
    }
}
