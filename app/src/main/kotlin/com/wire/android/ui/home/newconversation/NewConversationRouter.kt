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

package com.wire.android.ui.home.newconversation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.navigation.rememberTrackingAnimatedNavController
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.search.SearchPeopleRouter
import com.wire.android.ui.home.newconversation.common.NewConversationNavigationItem
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionScreen
import com.wire.android.ui.home.newconversation.newgroup.NewGroupScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationRouter() {
    val newConversationViewModel: NewConversationViewModel = hiltViewModel()
    val newConversationNavController = rememberTrackingAnimatedNavController() { NewConversationNavigationItem.fromRoute(it)?.itemName }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        }) { internalPadding ->
        NavHost(
            navController = newConversationNavController,
            startDestination = NewConversationNavigationItem.SearchListNavHostScreens.route,
            modifier = Modifier.padding(internalPadding)
        ) {
            composable(
                route = NewConversationNavigationItem.SearchListNavHostScreens.route,
                content = {
                    SearchPeopleRouter(
                        searchAllPeopleViewModel = newConversationViewModel,
                        onGroupSelectionSubmitAction = {
                            newConversationNavController.navigate(NewConversationNavigationItem.NewGroupNameScreen.route)
                        }
                    )
                }
            )
            composable(
                route = NewConversationNavigationItem.NewGroupNameScreen.route,
                content = {
                    NewGroupScreen(
                        onBackPressed = newConversationNavController::popBackStack,
                        newGroupState = newConversationViewModel.newGroupState,
                        onGroupNameChange = newConversationViewModel::onGroupNameChange,
                        onContinuePressed = {
                            if (newConversationViewModel.newGroupState.isSelfTeamMember) {
                                newConversationNavController.navigate(NewConversationNavigationItem.GroupOptionsScreen.route)
                            } else {
                                newConversationViewModel.createGroup()
                            }
                        },
                        onGroupNameErrorAnimated = newConversationViewModel::onGroupNameErrorAnimated
                    )
                }
            )
            composable(
                route = NewConversationNavigationItem.GroupOptionsScreen.route,
                content = {
                    GroupOptionScreen(
                        onBackPressed = newConversationNavController::popBackStack,
                        onCreateGroup = newConversationViewModel::createGroup,
                        groupOptionState = newConversationViewModel.groupOptionsState,
                        onAllowGuestChanged = newConversationViewModel::onAllowGuestStatusChanged,
                        onAllowServicesChanged = newConversationViewModel::onAllowServicesStatusChanged,
                        onReadReceiptChanged = newConversationViewModel::onReadReceiptStatusChanged,
                        onAllowGuestsDialogDismissed = newConversationViewModel::onAllowGuestsDialogDismissed,
                        onAllowGuestsClicked = newConversationViewModel::onAllowGuestsClicked,
                        onNotAllowGuestsClicked = newConversationViewModel::onNotAllowGuestClicked,
                        onErrorDismissed = newConversationViewModel::onGroupOptionsErrorDismiss
                    )
                }
            )
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        newConversationViewModel.infoMessage.collect {
            snackbarHostState.showSnackbar(it.asString(context.resources))
        }
    }
}
