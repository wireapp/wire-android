package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.home.newconversation.common.Screen
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
                    topBarTitle = stringResource(id = R.string.label_new_conversation),
                    searchPeopleViewModel = newConversationViewModel,
                    onNewGroupClicked = { newConversationNavController.navigate(Screen.NewGroupNameScreen.route) },
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
                    onCreateGroup = newConversationViewModel::createGroupConversation,
                    onGroupNameErrorAnimated = newConversationViewModel::onGroupNameErrorAnimated
                )
            }
        )
    }
}
