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

package com.wire.android.ui.home.settings.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.util.VisibleForTesting
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_SETTINGS_DISPLAY_NAME_CHANGED
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.navigation.getBackNavArg
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@HiltViewModel
class MyAccountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSelf: GetSelfUserUseCase,
    private val getSelfTeam: GetSelfTeamUseCase,
    private val serverConfig: SelfServerConfigUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val isReadOnlyAccount: IsReadOnlyAccountUseCase,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider
) : SavedStateViewModel(savedStateHandle) {

    var myAccountState by mutableStateOf(MyAccountState())
        private set

    @VisibleForTesting
    var hasSAMLCred by Delegates.notNull<Boolean>()

    @VisibleForTesting
    var managedByWire by Delegates.notNull<Boolean>()

    init {
        runBlocking {
            hasSAMLCred = when (val result = isPasswordRequired()) {
                is IsPasswordRequiredUseCase.Result.Failure -> false
                is IsPasswordRequiredUseCase.Result.Success -> {
                    !result.value
                }
            }

            // is the account is read only it means it is not maneged by wire
            managedByWire = !isReadOnlyAccount()
        }
        myAccountState = myAccountState.copy(
            isReadOnlyAccount = !managedByWire,
            isEditEmailAllowed = isChangeEmailEnabledByBuild() && !hasSAMLCred && managedByWire,
            isEditHandleAllowed = managedByWire
        )
        viewModelScope.launch {
            fetchSelfUser()
        }

        viewModelScope.launch {
            if (!hasSAMLCred) {
                loadChangePasswordUrl()
            }
        }
    }

    private suspend fun loadChangePasswordUrl() {
        when (val result = withContext(dispatchers.io()) { serverConfig() }) {
            is SelfServerConfigUseCase.Result.Failure -> appLogger.e(
                "Error when fetching the accounts url for change password"
            )

            is SelfServerConfigUseCase.Result.Success ->
                myAccountState = myAccountState.copy(changePasswordUrl = result.serverLinks.links.forgotPassword)
        }
    }

    private suspend fun fetchSelfUser() {
        viewModelScope.launch {
            val self = getSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))
            val selfTeam = getSelfTeam().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))

            combine(self, selfTeam) { selfUser: SelfUser, team: Team? -> selfUser to team }
                .collect { (user, team) ->
                    myAccountState = myAccountState.copy(
                        fullName = user.name.orEmpty(),
                        userName = user.handle.orEmpty(),
                        email = user.email.orEmpty(),
                        teamName = team?.name.orEmpty(),
                        domain = user.id.domain
                    )
                }
        }
    }

    fun navigateToChangeDisplayName() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(
                    destination = NavigationItem.EditDisplayName.getRouteWithArgs(),
                    backStackMode = BackStackMode.NONE
                )
            )
        }
    }

    fun navigateToChangeEmail() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(
                    destination = NavigationItem.EditEmailAddress.getRouteWithArgs(),
                    backStackMode = BackStackMode.NONE
                )
            )
        }
    }

    fun navigateToChangeHandle() {
        viewModelScope.launch {
            navigationManager.navigate(
                NavigationCommand(
                    destination = NavigationItem.EditHandle.getRouteWithArgs(),
                    backStackMode = BackStackMode.NONE
                )
            )
        }
    }

    fun checkForPendingMessages(): SettingsOperationResult {
        return with(savedStateHandle) {
            when (getBackNavArg<Boolean>(EXTRA_SETTINGS_DISPLAY_NAME_CHANGED)) {
                true -> SettingsOperationResult.Result(UIText.StringResource(R.string.settings_myaccount_display_name_updated))
                false -> SettingsOperationResult.Result(UIText.StringResource(R.string.error_unknown_message))
                null -> SettingsOperationResult.None
            }
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    sealed interface SettingsOperationResult {
        object None : SettingsOperationResult
        class Result(val message: UIText) : SettingsOperationResult
    }

    companion object {
        /**
         * This is a build time flag that allows to enable/disable the change email feature.
         * NOTE: This is using this approach to being able to test correctly and not depend on custom build behavior.
         */
        @JvmStatic
        fun isChangeEmailEnabledByBuild(): Boolean = BuildConfig.ALLOW_CHANGE_OF_EMAIL
    }
}
