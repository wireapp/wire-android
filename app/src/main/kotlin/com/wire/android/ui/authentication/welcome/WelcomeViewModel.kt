/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.authentication.welcome

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.destinations.CreatePersonalAccountOverviewScreenDestination
import com.wire.android.ui.destinations.CreateTeamAccountOverviewScreenDestination
import com.wire.android.ui.destinations.LoginScreenDestination
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val authServerConfigProvider: AuthServerConfigProvider,
    private val getSessions: GetSessionsUseCase
) : ViewModel() {

    var state by mutableStateOf(authServerConfigProvider.authServer.value)
        private set

    var isThereActiveSession by mutableStateOf(false)

    init {
        observerAuthServer()
        checkNumberOfSessions()
    }

    private fun observerAuthServer() {
        viewModelScope.launch {
            authServerConfigProvider.authServer.collect {
                state = it
            }
        }
    }

    private fun checkNumberOfSessions() {
        viewModelScope.launch {
            getSessions().let {
                when (it) {
                    is GetAllSessionsResult.Success -> {
                        isThereActiveSession =
                            it.sessions.filterIsInstance<AccountInfo.Valid>().isNullOrEmpty().not()
                    }
                    is GetAllSessionsResult.Failure.Generic -> {}
                    GetAllSessionsResult.Failure.NoSessionFound -> {
                        isThereActiveSession = false
                    }
                }
            }
        }
    }

    fun navigateBack() {
        viewModelScope.launch {
            navigationManager.navigateBack()
        }
    }

    fun isProxyEnabled() = state.apiProxy != null

    fun goToLogin() {
        navigate(NavigationCommand(LoginScreenDestination()))
    }

    fun goToCreateEnterpriseAccount() {
        navigate(NavigationCommand(CreateTeamAccountOverviewScreenDestination))
    }

    fun goToCreatePrivateAccount() {
        navigate(NavigationCommand(CreatePersonalAccountOverviewScreenDestination()))
    }

    private fun navigate(navigationCommand: NavigationCommand) {
        viewModelScope.launch {
            navigationManager.navigate(navigationCommand)
        }
    }
}
