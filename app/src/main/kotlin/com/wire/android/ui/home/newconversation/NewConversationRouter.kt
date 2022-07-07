package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.home.newconversation.common.Screen
import com.wire.android.ui.home.newconversation.newgroup.NewGroupScreen

@Composable
fun NewConversationRouter() {
    val newConversationViewModel: CreateNewConversationViewModel = hiltViewModel()
    val newConversationNavController = rememberNavController()

    NavHost(
        navController = newConversationNavController,
        startDestination = Screen.SearchListNavHostScreens.route
    ) {
        composable(
            route = Screen.SearchListNavHostScreens.route,
            content = {
                SearchPeopleRouter(
                    searchPeopleViewModel = newConversationViewModel,
                    openNewGroup = { newConversationNavController.navigate(Screen.NewGroupNameScreen.route) },
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
                    onCreateGroup = newConversationViewModel::createGroup,
                    onGroupNameErrorAnimated = newConversationViewModel::onGroupNameErrorAnimated
                )
            }
        )
    }
}
