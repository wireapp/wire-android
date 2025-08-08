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

package com.wire.android.ui.home

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
<<<<<<< HEAD
import com.wire.android.ui.common.ActionsViewModel
=======
import com.wire.android.navigation.SavedStateViewModel
import com.wire.kalium.logic.data.auth.AccountInfo
>>>>>>> 8cdb1f721 (fix: crash on logout [WPB-18706] (#4147))
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class HomeViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val dataStore: UserDataStore,
    private val observeSelf: ObserveSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase,
    private val observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
<<<<<<< HEAD
) : ActionsViewModel<HomeRequirement>() {
=======
    private val shouldTriggerMigrationForUser: ShouldTriggerMigrationForUserUserCase,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
) : SavedStateViewModel(savedStateHandle) {
>>>>>>> 8cdb1f721 (fix: crash on logout [WPB-18706] (#4147))

    @VisibleForTesting
    var homeState by mutableStateOf(HomeState())
        private set

    private val selfUserFlow = MutableSharedFlow<SelfUser?>(replay = 1)

    init {
        observeSelfUser()
        observeLegalHoldStatus()
        observeCreateTeamIndicator()
    }

    private fun observeSelfUser() {
        viewModelScope.launch {
            observeSelf().collectLatest { selfUser ->
                selfUserFlow.emit(selfUser)
                homeState = homeState.copy(
                    userAvatarData = UserAvatarData(
                        asset = selfUser.previewPicture?.let {
                            UserAvatarAsset(it)
                        },
                        availabilityStatus = selfUser.availabilityStatus,
                        nameBasedAvatar = NameBasedAvatar(selfUser.name, selfUser.accentId)
                    )
                )
            }
        }
    }

    private fun observeLegalHoldStatus() {
        viewModelScope.launch {
            observeLegalHoldStatusForSelfUser()
                .collectLatest {
                    homeState =
                        homeState.copy(shouldDisplayLegalHoldIndicator = it != LegalHoldStateForSelfUser.Disabled)
                }
        }
    }

    private fun observeCreateTeamIndicator() {
        viewModelScope.launch {
            if (!canMigrateFromPersonalToTeam()) {
                homeState = homeState.copy(
                    shouldShowCreateTeamUnreadIndicator = false
                )
                return@launch
            }

            dataStore.isCreateTeamNoticeRead().collect { isRead ->
                homeState = homeState.copy(
                    shouldShowCreateTeamUnreadIndicator = !isRead
                )
            }
        }
    }

    fun checkRequirements() {
        viewModelScope.launch {
            val selfUser = selfUserFlow.firstOrNull() ?: return@launch
            if (isLoggedOut()) return@launch
            when {
                needsToRegisterClient() -> // check if the client needs to be registered
                    sendAction(HomeRequirement.RegisterDevice)

                !dataStore.initialSyncCompleted.first() -> // check if the initial sync needs to be completed
                    sendAction(HomeRequirement.InitialSync)

                selfUser.handle.isNullOrEmpty() -> // check if the user handle needs to be set
                    sendAction(HomeRequirement.CreateAccountUsername)
            }
        }
    }
<<<<<<< HEAD
=======

    private suspend fun shouldDisplayWelcomeToARScreen() =
        globalDataStore.isMigrationCompleted() && !globalDataStore.isWelcomeScreenPresented()

    fun dismissWelcomeMessage() {
        viewModelScope.launch {
            globalDataStore.setWelcomeScreenPresented()
            homeState = homeState.copy(shouldDisplayWelcomeMessage = false)
        }
    }

    private suspend fun isLoggedOut(): Boolean {
        val accountInfo = (currentSessionFlow.get().invoke().firstOrNull() as? CurrentSessionResult.Success)?.accountInfo
        return accountInfo !is AccountInfo.Valid
    }
>>>>>>> 8cdb1f721 (fix: crash on logout [WPB-18706] (#4147))
}
