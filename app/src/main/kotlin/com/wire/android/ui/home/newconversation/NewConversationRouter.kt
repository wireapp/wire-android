package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.home.newconversation.common.Screen
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionScreen
import com.wire.android.ui.home.newconversation.newgroup.NewGroupScreen
import com.wire.android.ui.home.conversations.search.SearchPeopleRouter

@Composable
fun NewConversationRouter() {
    val newConversationViewModel: NewConversationViewModel = hiltViewModel()
    val newConversationNavController = rememberNavController()

    NavHost(
        navController = newConversationNavController,
        startDestination = Screen.SearchListNavHostScreens.route
    ) {
        composable(
            route = Screen.SearchListNavHostScreens.route,
            content = {
                SearchPeopleRouter(
                    searchBarTitle = stringResource(id = R.string.label_new_conversation),
                    searchPeopleViewModel = newConversationViewModel,
                    onPeoplePicked = { newConversationNavController.navigate(Screen.NewGroupNameScreen.route) },
                )
            }
        )
        composable(
            route = Screen.NewGroupNameScreen.route,
            content = {
                NewGroupScreen(
                    onBackPressed = newConversationNavController::popBackStack,
                    newGroupState = newConversationViewModel.groupNameState,
                    onGroupNameChange = newConversationViewModel::onGroupNameChange,
                    onContinuePressed = { newConversationNavController.navigate(Screen.GroupOptionsScreen.route) },
                    onGroupNameErrorAnimated = newConversationViewModel::onGroupNameErrorAnimated
                )
            }
        )

        composable(
            route = Screen.GroupOptionsScreen.route,
            content = {
                GroupOptionScreen(
                    onBackPressed = newConversationNavController::popBackStack,
                    onCreateGroup = newConversationViewModel::createGroupConversation,
                    groupOptionState = newConversationViewModel.groupOptionsState,
                    onAllowGuestChanged = newConversationViewModel::onAllowGuestStatusChanged,
                    onAllowServicesChanged = newConversationViewModel::onAllowServicesStatusChanged,
                    onReadReceiptChanged = newConversationViewModel::onReadReceiptStatusChanged,
                    onAllowGuestsDialogDismissed = newConversationViewModel::onAllowGuestsDialogDismissed,
                    onAllowGuestsClicked = newConversationViewModel::onAllowGuestsClicked,
                    onNotAllowGuestsClicked = newConversationViewModel::onNotAllowGuestClicked
                )
            }
        )
    }
}
