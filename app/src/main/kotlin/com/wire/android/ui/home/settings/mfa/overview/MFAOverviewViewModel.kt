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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.feature.mfa.MFAOverview
import com.wire.kalium.logic.feature.mfa.ObserveMFAOverviewUseCase
import com.wire.kalium.logic.feature.mfa.RefreshMFAOverviewUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MFAOverviewState {
    data object Loading : MFAOverviewState
    data object Error : MFAOverviewState

    data class Loaded(
        val overview: MFAOverview,
        val isRefreshing: Boolean = false,
        val hasRefreshError: Boolean = false,
    ) : MFAOverviewState
}

class MFAOverviewViewModel @Inject constructor(
    observeOverview: ObserveMFAOverviewUseCase,
    private val refreshOverview: RefreshMFAOverviewUseCase,
) : ViewModel() {
    private data class RefreshStatus(val isRefreshing: Boolean = false, val hasError: Boolean = false)
    private val refreshStatus = MutableStateFlow(RefreshStatus())

    val state = combine(observeOverview(), refreshStatus) { overview, status ->
        when {
            overview != null -> MFAOverviewState.Loaded(
                overview = overview,
                isRefreshing = status.isRefreshing,
                hasRefreshError = status.hasError,
            )
            status.hasError -> MFAOverviewState.Error
            else -> MFAOverviewState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MFAOverviewState.Loading)

    init {
        refresh()
    }

    @Suppress("TooGenericExceptionCaught")
    fun refresh() {
        if (refreshStatus.value.isRefreshing) return
        refreshStatus.value = RefreshStatus(isRefreshing = true)
        viewModelScope.launch {
            var hasError = false
            try {
                hasError = refreshOverview() is Either.Left
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                hasError = true
            } finally {
                refreshStatus.value = RefreshStatus(hasError = hasError)
            }
        }
    }
}
