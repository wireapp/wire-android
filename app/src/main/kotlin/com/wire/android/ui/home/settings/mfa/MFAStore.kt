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

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class MFAStoreState(
    val overview: MFAOverview? = null,
    val isRefreshing: Boolean = false,
    val hasRefreshError: Boolean = false,
)

/** Owned by the MFA flow ViewModel, not the account's dependency graph. */
class MFAStore @Inject constructor() {
    private val _state = MutableStateFlow(MFAStoreState())
    val state = _state.asStateFlow()
    private val updateMutex = Mutex()
    private var dummyOverviewOverride: MFAOverview? = null

    /** Refreshes and mutations share one lock; overlapping refresh requests are skipped. */
    internal suspend fun withRefresh(block: suspend () -> Unit) {
        if (!updateMutex.tryLock()) return
        try {
            block()
        } finally {
            updateMutex.unlock()
        }
    }

    /** Called inside [withRefresh] so loading cannot overwrite a concurrent mutation. */
    internal fun publishRefreshedOverview(overview: MFAOverview) {
        _state.update { it.copy(overview = dummyOverviewOverride ?: overview) }
    }

    /** Refresh status is published by the refresh use case while it holds [withRefresh]. */
    internal fun setRefreshStatus(isRefreshing: Boolean, hasRefreshError: Boolean) {
        _state.update { it.copy(isRefreshing = isRefreshing, hasRefreshError = hasRefreshError) }
    }

    /** Evaluate rules against the latest overview and publish their outcome under the same lock. */
    internal suspend fun <T> updateOverview(transform: (MFAOverview?) -> MFAOverviewUpdate<T>): T = updateMutex.withLock {
        val update = transform(_state.value.overview)
        update.overview?.let { overview ->
            // Keep local prototype changes authoritative until the MFA area is closed.
            dummyOverviewOverride = overview
            _state.value = MFAStoreState(overview = overview)
        }
        update.result
    }
}

/** A null overview leaves the cache unchanged, for example when an operation is rejected. */
internal data class MFAOverviewUpdate<T>(val overview: MFAOverview? = null, val result: T)
