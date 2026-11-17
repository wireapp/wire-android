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

import com.wire.android.ui.home.settings.mfa.MFAOverview
import com.wire.android.ui.home.settings.mfa.MFAStore
import com.wire.android.ui.home.settings.mfa.MFAStoreState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class ObserveMFAOverviewUseCase(
    private val store: MFAStore,
) {
    operator fun invoke(): Flow<MFAStoreState> = store.state
}

class RefreshMFAOverviewUseCase(
    private val store: MFAStore,
    private val loadOverview: suspend () -> MFAOverview,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke() = store.withRefresh {
        store.setRefreshStatus(isRefreshing = true, hasRefreshError = false)
        var hasRefreshError = false
        try {
            store.publishRefreshedOverview(loadOverview())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            hasRefreshError = true
        } finally {
            store.setRefreshStatus(isRefreshing = false, hasRefreshError = hasRefreshError)
        }
    }
}
