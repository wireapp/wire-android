/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.ui.home.settings.account

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.navigation.SavedStateViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.team.GetUpdatedSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.IsPasswordRequiredUseCase
import com.wire.kalium.logic.feature.user.IsReadOnlyAccountUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.functional.getOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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
    private val getSelfTeam: GetUpdatedSelfTeamUseCase,
    private val serverConfig: SelfServerConfigUseCase,
    private val isPasswordRequired: IsPasswordRequiredUseCase,
    private val isReadOnlyAccount: IsReadOnlyAccountUseCase,
    private val dispatchers: DispatcherProvider,
    private val isE2EIEnabledUseCase: IsE2EIEnabledUseCase
) : SavedStateViewModel(savedStateHandle) {

    var myAccountState by mutableStateOf(MyAccountState())
        private set

    @VisibleForTesting
    var hasSAMLCred by Delegates.notNull<Boolean>()

    @VisibleForTesting
    var managedByWire by Delegates.notNull<Boolean>()

    @VisibleForTesting
    var isE2EIEnabled by Delegates.notNull<Boolean>()

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
            isE2EIEnabled = isE2EIEnabledUseCase()
        }
        myAccountState = myAccountState.copy(
            isEditEmailAllowed = isChangeEmailEnabledByBuild() && !hasSAMLCred && managedByWire,
            isEditNameAllowed = managedByWire && !isE2EIEnabled,
            isEditHandleAllowed = managedByWire && !isE2EIEnabled
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
            val selfTeam = getSelfTeam().getOrNull()
            val self = getSelf().flowOn(dispatchers.io()).shareIn(this, SharingStarted.WhileSubscribed(1))

            self.collect { user ->
                myAccountState = myAccountState.copy(
                    fullName = user.name.orEmpty(),
                    userName = user.handle.orEmpty(),
                    email = user.email.orEmpty(),
                    teamName = selfTeam?.name.orEmpty(),
                    domain = user.id.domain
                )
            }
        }
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
