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
import kotlinx.coroutines.async
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

class MultiFactorAuthenticationViewModel @Inject constructor(
    store: MultiFactorAuthenticationStore,
) : ViewModel() {
    // Construct the use cases from the same unscoped store so the flow owns exactly one cache.
    private val observeOverview = ObserveMultiFactorAuthenticationOverviewUseCase(store)
    private val refreshOverview = RefreshMultiFactorAuthenticationOverviewUseCase(store)
    private val completeSetup = CompleteMultiFactorAuthenticationSetupUseCase(store)
    private val updateRecoveryCodes = UpdateMultiFactorAuthenticationRecoveryCodesStatusUseCase(store)
    private val completeTurnOff = CompleteMultiFactorAuthenticationTurnOffUseCase(store)
    private val completeAuthenticatorRename = CompleteAuthenticatorAppRenameUseCase(store)
    private val completeAuthenticatorAddition = CompleteAuthenticatorAppAdditionUseCase(store)
    private val completeAuthenticatorRemoval = CompleteAuthenticatorAppRemovalUseCase(store)

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

    fun onRecoveryCodesGenerated(result: MultiFactorAuthenticationRecoveryCodesResult) {
        viewModelScope.launch { updateRecoveryCodes(result) }
    }

    fun onTurnOffCompleted() {
        viewModelScope.launch { completeTurnOff() }
    }

    fun onAuthenticatorAppRenamed(result: AuthenticatorAppRenameResult) {
        viewModelScope.launch { completeAuthenticatorRename(result) }
    }

    fun onAuthenticatorAppAdded(result: AuthenticatorAppAddedResult) = viewModelScope.async {
        completeAuthenticatorAddition(result)
    }

    fun removeAuthenticatorApp(appId: String) = viewModelScope.async {
        completeAuthenticatorRemoval(appId)
    }
}
