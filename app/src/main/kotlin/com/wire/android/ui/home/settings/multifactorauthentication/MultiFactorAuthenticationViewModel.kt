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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MultiFactorAuthenticationOverviewState {
    data object Loading : MultiFactorAuthenticationOverviewState
    data object Error : MultiFactorAuthenticationOverviewState

    data class Loaded(
        val overview: MultiFactorAuthenticationOverview,
        val isRefreshing: Boolean = false,
        val hasRefreshError: Boolean = false,
    ) : MultiFactorAuthenticationOverviewState
}

class MultiFactorAuthenticationViewModel(
    observeOverview: ObserveMultiFactorAuthenticationOverviewUseCase,
    private val refreshOverview: RefreshMultiFactorAuthenticationOverviewUseCase,
    private val completeSetup: CompleteMultiFactorAuthenticationSetupUseCase,
) : ViewModel() {
    // Construct both use cases from the same unscoped store so the flow owns exactly one cache.
    @Inject
    constructor(store: MultiFactorAuthenticationStore) : this(
        ObserveMultiFactorAuthenticationOverviewUseCase(store),
        RefreshMultiFactorAuthenticationOverviewUseCase(store),
        CompleteMultiFactorAuthenticationSetupUseCase(store),
    )

    val state = observeOverview().map { storeState ->
        val overview = storeState.overview
        when {
            overview != null -> MultiFactorAuthenticationOverviewState.Loaded(
                overview = overview,
                isRefreshing = storeState.isRefreshing,
                hasRefreshError = storeState.failure != null,
            )
            storeState.failure != null -> MultiFactorAuthenticationOverviewState.Error
            else -> MultiFactorAuthenticationOverviewState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MultiFactorAuthenticationOverviewState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { refreshOverview() }
    }

    fun onSetupCompleted(result: MultiFactorAuthenticationSetupResult) {
        viewModelScope.launch { completeSetup(result) }
    }
}
