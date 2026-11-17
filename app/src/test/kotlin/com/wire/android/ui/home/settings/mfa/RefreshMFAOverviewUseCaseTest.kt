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

package com.wire.android.ui.home.settings.mfa

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshMFAOverviewUseCaseTest {
    @Test
    fun `given concurrent refreshes then only one load runs and all readers receive its result`() = runTest {
        val response = CompletableDeferred<MFAOverview>()
        var loads = 0
        val store = MFAStore()
        val loadOverview: suspend () -> MFAOverview = {
            loads++
            response.await()
        }
        val refresh = RefreshMFAOverviewUseCase(store, loadOverview)
        val otherRefresh = RefreshMFAOverviewUseCase(store, loadOverview)

        assertNull(store.state.value.overview)
        val first = launch { refresh() }
        runCurrent()
        assertTrue(store.state.value.isRefreshing)
        otherRefresh()
        assertEquals(1, loads)

        response.complete(MFAOverview.Disabled)
        first.join()
        assertEquals(MFAOverview.Disabled, store.state.value.overview)
        assertFalse(store.state.value.isRefreshing)
    }

    @Test
    fun `given cached content when refresh fails then content remains and retry can recover`() = runTest {
        var fail = false
        val store = MFAStore()
        val refresh = RefreshMFAOverviewUseCase(store) {
            if (fail) throw IOException("Offline")
            MFAOverview.Disabled
        }
        refresh()
        fail = true

        refresh()

        assertEquals(MFAOverview.Disabled, store.state.value.overview)
        assertTrue(store.state.value.hasRefreshError)
        assertFalse(store.state.value.isRefreshing)

        fail = false
        refresh()
        assertFalse(store.state.value.hasRefreshError)
    }

    @Test
    fun `when a refresh is cancelled then no error is published and a later refresh can run`() = runTest {
        val response = CompletableDeferred<MFAOverview>()
        val store = MFAStore()
        val refresh = RefreshMFAOverviewUseCase(store) { response.await() }
        val first = launch { refresh() }
        runCurrent()

        first.cancel()
        first.join()

        assertTrue(first.isCancelled)
        assertFalse(store.state.value.isRefreshing)
        assertFalse(store.state.value.hasRefreshError)
        response.complete(MFAOverview.Disabled)
        refresh()
        assertEquals(MFAOverview.Disabled, store.state.value.overview)
    }

    @Test
    fun `a mutation waits for a pending refresh and is not lost to a later dummy refresh`() = runTest {
        val original = MFAOverview.Enabled(
            authenticatorApps = listOf(AuthenticatorApp("primary", "Primary", LocalDate(2026, 10, 14))),
            recoveryCodesStatus = RecoveryCodesStatus(3, 10),
        )
        val response = CompletableDeferred<MFAOverview>()
        val store = MFAStore()
        val refresh = RefreshMFAOverviewUseCase(store) { response.await() }
        val addition = CompleteAuthenticatorAppAdditionUseCase(store)
        val pendingRefresh = launch { refresh() }
        runCurrent()

        val pendingAddition = async { addition(AuthenticatorAppAddedResult("backup", "Backup")) }
        runCurrent()
        assertFalse(pendingAddition.isCompleted)
        assertNull(store.state.value.overview)

        response.complete(original)
        pendingRefresh.join()
        assertEquals(AuthenticatorAppMutationResult.SUCCESS, pendingAddition.await())
        val updated = store.state.value.overview as MFAOverview.Enabled
        assertEquals(listOf("primary", "backup"), updated.authenticatorApps.map { it.id })
        assertEquals(original.recoveryCodesStatus, updated.recoveryCodesStatus)

        refresh()
        assertEquals(updated, store.state.value.overview)
    }

    @Test
    fun `retry clears the refresh error immediately while retaining cached content`() = runTest {
        val store = MFAStore()
        var response = CompletableDeferred<MFAOverview>().also { it.complete(MFAOverview.Disabled) }
        val refresh = RefreshMFAOverviewUseCase(store) { response.await() }
        refresh()
        response = CompletableDeferred<MFAOverview>().also { it.completeExceptionally(IOException("Offline")) }
        refresh()
        assertTrue(store.state.value.hasRefreshError)

        response = CompletableDeferred()
        val retry = launch { refresh() }
        runCurrent()

        assertEquals(MFAStoreState(overview = MFAOverview.Disabled, isRefreshing = true), store.state.value)
        response.complete(MFAOverview.Disabled)
        retry.join()
        assertEquals(MFAStoreState(overview = MFAOverview.Disabled), store.state.value)
    }
}
