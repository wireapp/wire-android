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

import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MultiFactorAuthenticationStoreTest {
    @Test
    fun `given concurrent refreshes then only one load runs and all readers receive its result`() = runTest {
        val response = CompletableDeferred<MultiFactorAuthenticationOverview>()
        var loads = 0
        val store = MultiFactorAuthenticationStore {
            loads++
            response.await()
        }

        assertNull(store.state.value.overview)
        val first = launch { store.refresh() }
        runCurrent()
        assertTrue(store.state.value.isRefreshing)
        store.refresh()
        assertEquals(1, loads)

        response.complete(MultiFactorAuthenticationOverview.Disabled)
        first.join()
        assertEquals(MultiFactorAuthenticationOverview.Disabled, store.state.value.overview)
        assertFalse(store.state.value.isRefreshing)
    }

    @Test
    fun `given cached content when refresh fails then content remains and retry can recover`() = runTest {
        var fail = false
        val store = MultiFactorAuthenticationStore {
            if (fail) throw IOException("Offline")
            MultiFactorAuthenticationOverview.Disabled
        }
        store.refresh()
        fail = true

        store.refresh()

        assertEquals(MultiFactorAuthenticationOverview.Disabled, store.state.value.overview)
        assertTrue(store.state.value.failure is IOException)
        assertFalse(store.state.value.isRefreshing)

        fail = false
        store.refresh()
        assertNull(store.state.value.failure)
    }

    @Test
    fun `when a refresh is cancelled then no error is published and a later refresh can run`() = runTest {
        val response = CompletableDeferred<MultiFactorAuthenticationOverview>()
        val store = MultiFactorAuthenticationStore { response.await() }
        val first = launch { store.refresh() }
        runCurrent()

        first.cancel()
        first.join()

        assertTrue(first.isCancelled)
        assertFalse(store.state.value.isRefreshing)
        assertNull(store.state.value.failure)
        response.complete(MultiFactorAuthenticationOverview.Disabled)
        store.refresh()
        assertEquals(MultiFactorAuthenticationOverview.Disabled, store.state.value.overview)
    }
}
