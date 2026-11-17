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

package com.wire.android.ui.home.settings.mfa.overview

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.settings.mfa.AuthenticatorApp
import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.MFAStore
import com.wire.android.ui.home.settings.mfa.RecoveryCodesStatus
import com.wire.android.ui.home.settings.mfa.authenticatorapps.edit.AuthenticatorAppRenameResult
import com.wire.android.ui.home.settings.mfa.recoverycodes.MFARecoveryCodesResult
import com.wire.android.ui.home.settings.mfa.setup.MFASetupResult
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
class MFAOverviewViewModelTest {
    private val owner = ViewModelStore()
    private val enabled = MFAOverview.Enabled(
        authenticatorApps = listOf(AuthenticatorApp("phone", "Android phone", LocalDate(2026, 10, 14))),
        recoveryCodesStatus = RecoveryCodesStatus(remaining = 10, total = 10),
    )

    @AfterEach
    fun tearDown() {
        owner.clear()
    }

    @Test
    fun `given no cached data then loading is shown until the overview arrives`() = runTest {
        val response = CompletableDeferred<MFAOverview>()
        val viewModel = createViewModel { response.await() }
        runCurrent()
        assertEquals(MFAOverviewState.Loading, viewModel.state.value)

        response.complete(MFAOverview.Disabled)
        runCurrent()

        assertEquals(
            MFAOverviewState.Loaded(MFAOverview.Disabled),
            viewModel.state.value,
        )
    }

    @Test
    fun `given initial load failure when retry succeeds then error is replaced with content`() = runTest {
        var fail = true
        val viewModel = createViewModel {
            if (fail) throw IOException("Offline")
            enabled
        }
        runCurrent()
        assertEquals(MFAOverviewState.Error, viewModel.state.value)

        fail = false
        viewModel.refresh()
        runCurrent()

        assertEquals(MFAOverviewState.Loaded(enabled), viewModel.state.value)
    }

    @Test
    fun `given a loaded overview when the store changes then the screen reacts without being recreated`() = runTest {
        var response = enabled
        val store = MFAStore()
        val loadOverview: suspend () -> MFAOverview = { response }
        val viewModel = createViewModel(store, loadOverview)
        runCurrent()

        response = enabled.copy(authenticatorApps = enabled.authenticatorApps.map { it.copy(name = "New name") })
        RefreshMFAOverviewUseCase(store, loadOverview)()
        runCurrent()

        assertEquals(MFAOverviewState.Loaded(response), viewModel.state.value)
    }

    @Test
    fun `given cached data when refresh is pending or fails then content stays visible`() = runTest {
        var response = CompletableDeferred<MFAOverview>().also { it.complete(enabled) }
        val viewModel = createViewModel { response.await() }
        runCurrent()

        response = CompletableDeferred()
        viewModel.refresh()
        runCurrent()
        assertEquals(MFAOverviewState.Loaded(enabled, isRefreshing = true), viewModel.state.value)

        response.completeExceptionally(IOException("Offline"))
        runCurrent()

        assertEquals(MFAOverviewState.Loaded(enabled, hasRefreshError = true), viewModel.state.value)
    }

    @Test
    fun `when the flow owner is cleared then the pending load is cancelled without publishing an error`() = runTest {
        var cancelled = false
        val response = CompletableDeferred<MFAOverview>()
        val store = MFAStore()
        createViewModel(store) {
            try {
                response.await()
            } finally {
                cancelled = true
            }
        }
        runCurrent()

        owner.clear()
        runCurrent()

        assertTrue(cancelled)
        assertFalse(store.state.value.isRefreshing)
        assertFalse(store.state.value.hasRefreshError)
        assertNull(store.state.value.overview)
    }

    @Test
    fun `completed setup updates the observed overview and a dummy refresh keeps it enabled`() = runTest {
        val viewModel = createViewModel { MFAOverview.Disabled }
        viewModel.onSetupCompleted(MFASetupResult("new-app", "Android phone", 10))
        runCurrent()

        val completed = viewModel.state.value as MFAOverviewState.Loaded
        val overview = completed.overview as MFAOverview.Enabled
        assertEquals("new-app", overview.authenticatorApps.single().id)
        assertEquals(RecoveryCodesStatus(10, 10), overview.recoveryCodesStatus)

        viewModel.refresh()
        runCurrent()
        assertEquals(completed, viewModel.state.value)
    }

    @Test
    fun `regenerating codes resets the count without changing authenticators and survives a refresh`() = runTest {
        val original = enabled.copy(recoveryCodesStatus = RecoveryCodesStatus(3, 10))
        val viewModel = createViewModel { original }
        runCurrent()

        viewModel.onRecoveryCodesGenerated(MFARecoveryCodesResult(10))
        runCurrent()

        val updated = viewModel.state.value as MFAOverviewState.Loaded
        assertEquals(original.copy(recoveryCodesStatus = RecoveryCodesStatus(10, 10)), updated.overview)
        viewModel.refresh()
        runCurrent()
        assertEquals(updated, viewModel.state.value)
    }

    @Test
    fun `turning off clears the enabled overview and an enabled remote snapshot cannot restore it`() = runTest {
        val viewModel = createViewModel { enabled }
        runCurrent()
        viewModel.onTurnOffCompleted()
        runCurrent()

        val disabled = MFAOverviewState.Loaded(MFAOverview.Disabled)
        assertEquals(disabled, viewModel.state.value)
        viewModel.refresh()
        runCurrent()
        assertEquals(disabled, viewModel.state.value)
    }

    @Test
    fun `setup after turning off creates a fresh enabled overview without the old authenticator`() = runTest {
        val viewModel = createViewModel { enabled }
        runCurrent()
        viewModel.onTurnOffCompleted()
        runCurrent()
        viewModel.onSetupCompleted(MFASetupResult("replacement", "New phone", 10))
        runCurrent()

        val updated = (viewModel.state.value as MFAOverviewState.Loaded)
            .overview as MFAOverview.Enabled
        assertEquals("replacement", updated.authenticatorApps.single().id)
        assertEquals("New phone", updated.authenticatorApps.single().name)
        assertEquals(RecoveryCodesStatus(10, 10), updated.recoveryCodesStatus)
        viewModel.refresh()
        runCurrent()
        assertEquals(updated, (viewModel.state.value as MFAOverviewState.Loaded).overview)
    }

    @Test
    fun `renaming updates the shared snapshot by id and preserves other account data across refreshes`() = runTest {
        val other = enabled.authenticatorApps.single().copy(id = "backup", name = "Backup")
        val original = enabled.copy(authenticatorApps = enabled.authenticatorApps + other)
        val viewModel = createViewModel { original }
        runCurrent()
        val app = enabled.authenticatorApps.single()

        viewModel.onAuthenticatorAppRenamed(AuthenticatorAppRenameResult(app.id, "New name"))
        runCurrent()

        val expected = original.copy(authenticatorApps = listOf(app.copy(name = "New name"), other))
        assertEquals(expected, (viewModel.state.value as MFAOverviewState.Loaded).overview)
        viewModel.refresh()
        runCurrent()
        assertEquals(expected, (viewModel.state.value as MFAOverviewState.Loaded).overview)
    }

    @Test
    fun `stale rename results neither add missing apps nor restore MFA after turning off`() = runTest {
        val viewModel = createViewModel { enabled }
        runCurrent()
        viewModel.onAuthenticatorAppRenamed(AuthenticatorAppRenameResult("missing", "New name"))
        runCurrent()
        assertEquals(enabled, (viewModel.state.value as MFAOverviewState.Loaded).overview)

        viewModel.onTurnOffCompleted()
        runCurrent()
        viewModel.onAuthenticatorAppRenamed(AuthenticatorAppRenameResult(enabled.authenticatorApps.single().id, "New name"))
        runCurrent()
        assertEquals(
            MFAOverview.Disabled,
            (viewModel.state.value as MFAOverviewState.Loaded).overview,
        )
    }

    private fun createViewModel(
        store: MFAStore = MFAStore(),
        loadOverview: suspend () -> MFAOverview,
    ): MFAOverviewViewModel =
        ViewModelProvider.create(
            owner,
            viewModelFactory { initializer { MFAOverviewViewModel(store, loadOverview) } },
        )[MFAOverviewViewModel::class]
}
