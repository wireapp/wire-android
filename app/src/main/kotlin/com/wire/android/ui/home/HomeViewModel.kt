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
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.feature.analytics.AnonymousAnalyticsManager
import com.wire.android.feature.analytics.model.AnalyticsEvent
import com.wire.android.migration.userDatabase.ShouldTriggerMigrationForUserUserCase
import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.navigation.SavedStateViewModel
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class HomeViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val globalDataStore: GlobalDataStore,
    private val dataStore: UserDataStore,
    private val getSelfUser: GetSelfUserUseCase,
    private val observeSelf: ObserveSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase,
    private val observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val shouldTriggerMigrationForUser: ShouldTriggerMigrationForUserUserCase,
    private val analyticsManager: AnonymousAnalyticsManager,
) : SavedStateViewModel(savedStateHandle) {

    @VisibleForTesting
    var homeState by mutableStateOf(HomeState())
        private set

    init {
        loadUserAvatar()
        observeLegalHoldStatus()
        observeCreateTeamIndicator()
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

    fun checkRequirements(onRequirement: (HomeRequirement) -> Unit) {
        viewModelScope.launch {
            val selfUser = getSelfUser() ?: return@launch
            when {
                shouldTriggerMigrationForUser(selfUser.id) ->
                    onRequirement(HomeRequirement.Migration(selfUser.id))

                needsToRegisterClient() -> // check if the client has been registered and open the proper screen if not
                    onRequirement(HomeRequirement.RegisterDevice)

                selfUser.handle.isNullOrEmpty() -> // check if the user handle has been set and open the proper screen if not
                    onRequirement(HomeRequirement.CreateAccountUsername)

                shouldDisplayWelcomeToARScreen() -> {
                    homeState = homeState.copy(shouldDisplayWelcomeMessage = true)
                }
            }
        }
    }

    private suspend fun shouldDisplayWelcomeToARScreen() =
        globalDataStore.isMigrationCompleted() && !globalDataStore.isWelcomeScreenPresented()

    private fun loadUserAvatar() {
        viewModelScope.launch {
            observeSelf().collect { selfUser ->
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

    fun dismissWelcomeMessage() {
        viewModelScope.launch {
            globalDataStore.setWelcomeScreenPresented()
            homeState = homeState.copy(shouldDisplayWelcomeMessage = false)
        }
    }

    fun sendOpenProfileEvent() {
        analyticsManager.sendEvent(
            AnalyticsEvent.UserProfileOpened(
                isMigrationDotActive = homeState.shouldShowCreateTeamUnreadIndicator
            )
        )
    }
}
