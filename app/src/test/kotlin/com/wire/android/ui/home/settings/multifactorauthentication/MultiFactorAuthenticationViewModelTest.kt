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

package com.wire.android.ui.home.settings.multifactorauthentication

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.config.CoroutineTestExtension
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MultiFactorAuthenticationViewModelTest {
    private val owner = ViewModelStore()
    private val enabled = MultiFactorAuthenticationOverview.Enabled(
        authenticatorApps = listOf(AuthenticatorApp("phone", "Android phone", LocalDate(2026, 10, 14))),
        recoveryCodesStatus = RecoveryCodesStatus(remaining = 10, total = 10),
    )

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `given no cached data then loading is shown until the overview arrives`() = runTest {
        val response = CompletableDeferred<MultiFactorAuthenticationOverview>()
        val viewModel = createViewModel(MultiFactorAuthenticationStore { response.await() })
        runCurrent()
        assertEquals(MultiFactorAuthenticationOverviewState.Loading, viewModel.state.value)

        response.complete(MultiFactorAuthenticationOverview.Disabled)
        runCurrent()

        assertEquals(
            MultiFactorAuthenticationOverviewState.Loaded(MultiFactorAuthenticationOverview.Disabled),
            viewModel.state.value,
        )
    }

    @Test
    fun `given initial load failure when retry succeeds then error is replaced with content`() = runTest {
        var fail = true
        val viewModel = createViewModel(
            MultiFactorAuthenticationStore {
                if (fail) throw IOException("Offline")
                enabled
            }
        )
        runCurrent()
        assertEquals(MultiFactorAuthenticationOverviewState.Error, viewModel.state.value)

        fail = false
        viewModel.refresh()
        runCurrent()

        assertEquals(MultiFactorAuthenticationOverviewState.Loaded(enabled), viewModel.state.value)
    }

    @Test
    fun `given a loaded overview when the store changes then the screen reacts without being recreated`() = runTest {
        var response = enabled
        val store = MultiFactorAuthenticationStore { response }
        val viewModel = createViewModel(store)
        runCurrent()

        response = enabled.copy(authenticatorApps = enabled.authenticatorApps.map { it.copy(name = "New name") })
        store.refresh()
        runCurrent()

        assertEquals(MultiFactorAuthenticationOverviewState.Loaded(response), viewModel.state.value)
    }

    @Test
    fun `given cached data when refresh is pending or fails then content stays visible`() = runTest {
        var response = CompletableDeferred<MultiFactorAuthenticationOverview>().also { it.complete(enabled) }
        val viewModel = createViewModel(MultiFactorAuthenticationStore { response.await() })
        runCurrent()

        response = CompletableDeferred()
        viewModel.refresh()
        runCurrent()
        assertEquals(MultiFactorAuthenticationOverviewState.Loaded(enabled, isRefreshing = true), viewModel.state.value)

        response.completeExceptionally(IOException("Offline"))
        runCurrent()

        assertEquals(MultiFactorAuthenticationOverviewState.Loaded(enabled, hasRefreshError = true), viewModel.state.value)
    }

    @Test
    fun `when the flow owner is cleared then the pending load is cancelled without publishing an error`() = runTest {
        var cancelled = false
        val response = CompletableDeferred<MultiFactorAuthenticationOverview>()
        val store = MultiFactorAuthenticationStore {
            try {
                response.await()
            } finally {
                cancelled = true
            }
        }
        createViewModel(store)
        runCurrent()

        owner.clear()
        runCurrent()

        assertTrue(cancelled)
        assertFalse(store.state.value.isRefreshing)
        assertNull(store.state.value.failure)
        assertNull(store.state.value.overview)
    }

    @Test
    fun `completed setup updates the observed overview and a dummy refresh keeps it enabled`() = runTest {
        val store = MultiFactorAuthenticationStore { MultiFactorAuthenticationOverview.Disabled }
        val viewModel = createViewModel(store)
        viewModel.onSetupCompleted(MultiFactorAuthenticationSetupResult("new-app", "Android phone", 10))
        runCurrent()

        val completed = viewModel.state.value as MultiFactorAuthenticationOverviewState.Loaded
        val overview = completed.overview as MultiFactorAuthenticationOverview.Enabled
        assertEquals("new-app", overview.authenticatorApps.single().id)
        assertEquals(RecoveryCodesStatus(10, 10), overview.recoveryCodesStatus)

        viewModel.refresh()
        runCurrent()
        assertEquals(completed, viewModel.state.value)
    }

    @Test
    fun `regenerating codes resets the count without changing authenticators and survives a refresh`() = runTest {
        val original = enabled.copy(recoveryCodesStatus = RecoveryCodesStatus(3, 10))
        val viewModel = createViewModel(MultiFactorAuthenticationStore { original })
        runCurrent()

        viewModel.onRecoveryCodesGenerated(MultiFactorAuthenticationRecoveryCodesResult(10))
        runCurrent()

        val updated = viewModel.state.value as MultiFactorAuthenticationOverviewState.Loaded
        assertEquals(original.copy(recoveryCodesStatus = RecoveryCodesStatus(10, 10)), updated.overview)
        viewModel.refresh()
        runCurrent()
        assertEquals(updated, viewModel.state.value)
    }

    private fun createViewModel(store: MultiFactorAuthenticationStore): MultiFactorAuthenticationViewModel =
        ViewModelProvider.create(
            owner,
            viewModelFactory { initializer { MultiFactorAuthenticationViewModel(store) } },
        )[MultiFactorAuthenticationViewModel::class]
}
