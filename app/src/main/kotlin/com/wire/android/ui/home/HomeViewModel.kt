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
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.authentication.PostLoginAuthenticationRequirement
import com.wire.android.ui.authentication.PostLoginAuthenticationRequirementResolver
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class HomeViewModel @Inject constructor(
    userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount private val currentAccount: UserId,
    private val observeSelf: ObserveSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase,
    private val observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
) : ActionsViewModel<HomeRequirement>() {

    private val dataStore = userDataStoreProvider.getOrCreate(currentAccount)

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
            // On a fresh login the session graph can be created before the initial sync persists the self user.
            // Reading the migration eligibility before that would cache a missing team ID for the whole session.
            selfUserFlow.first()

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
            // Keep reads awaited and resolve once: the feature policy is deterministic and side-effect free.
            when (
                PostLoginAuthenticationRequirementResolver.resolve(
                    needsDeviceRegistration = needsToRegisterClient(),
                    initialSyncCompleted = dataStore.initialSyncCompleted.first(),
                    hasUsername = !selfUser.handle.isNullOrEmpty(),
                )
            ) {
                PostLoginAuthenticationRequirement.RegisterDevice ->
                    sendAction(HomeRequirement.RegisterDevice(currentAccount))
                PostLoginAuthenticationRequirement.InitialSync -> sendAction(HomeRequirement.InitialSync)
                PostLoginAuthenticationRequirement.CreateUsername -> sendAction(HomeRequirement.CreateAccountUsername)
                null -> Unit
            }
        }
    }
}
