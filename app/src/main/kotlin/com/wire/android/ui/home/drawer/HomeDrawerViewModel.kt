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

package com.wire.android.ui.home.drawer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.navigation.HomeDestination
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class HomeDrawerViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val observeArchivedUnreadConversationsCount: Lazy<ObserveArchivedUnreadConversationsCountUseCase>,
    private val observeSelfUser: ObserveSelfUserUseCase,
    private val getTeamUrl: GetTeamUrlUseCase,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
) : ViewModel() {

    var drawerState by mutableStateOf(HomeDrawerState())
        private set

    init {
        buildDrawerItems()
    }

    private suspend fun observeTeamManagementUrlForUser(): Flow<String> {
        return observeSelfUser().map {
            when (it.userType) {
                UserType.ADMIN,
                UserType.OWNER -> {
                    getTeamUrl()
                }

                UserType.INTERNAL,
                UserType.EXTERNAL,
                UserType.FEDERATED,
                UserType.GUEST,
                UserType.SERVICE,
                UserType.NONE -> {
                    String.EMPTY
                }
            }
        }
    }

    private fun buildDrawerItems() {
        viewModelScope.launch {
            combine(
                flowOf(isWireCellsEnabled()),
                observeArchivedUnreadConversationsCount.get().invoke(),
                observeTeamManagementUrlForUser()
            ) { wireCellsEnabled, unreadArchiveConversationsCount, teamManagementUrl ->
                buildList {
                    add(DrawerUiItem.RegularItem(destination = HomeDestination.Conversations))
                    if (wireCellsEnabled) {
                        add(DrawerUiItem.RegularItem(destination = HomeDestination.Cells))
                    }
                    if (BuildConfig.MEETINGS_ENABLED) {
                        add(DrawerUiItem.RegularItem(destination = HomeDestination.Meetings))
                    }
                    add(
                        DrawerUiItem.UnreadCounterItem(
                            destination = HomeDestination.Archive,
                            unreadCount = unreadArchiveConversationsCount
                        )
                    )
                } to buildList {
                    add(DrawerUiItem.RegularItem(destination = HomeDestination.WhatsNew))
                    add(DrawerUiItem.RegularItem(destination = HomeDestination.Settings))
                    if (teamManagementUrl.isNotBlank()) {
                        add(
                            DrawerUiItem.DynamicExternalNavigationItem(
                                destination = HomeDestination.TeamManagement,
                                url = teamManagementUrl
                            )
                        )
                    }
                    add(DrawerUiItem.RegularItem(destination = HomeDestination.Support))
                }
            }.collect {
                drawerState = drawerState.copy(items = it)
            }
        }
    }
}

/**
 * The type of the main navigation item.
 * Regular, with counter or with external navigation.
 */
sealed class DrawerUiItem(open val destination: HomeDestination) {
    data class RegularItem(override val destination: HomeDestination) : DrawerUiItem(destination)
    data class UnreadCounterItem(override val destination: HomeDestination, val unreadCount: Long) : DrawerUiItem(destination)
    data class DynamicExternalNavigationItem(override val destination: HomeDestination, val url: String) : DrawerUiItem(destination)
}
