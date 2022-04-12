package com.wire.android.ui.home.newconversation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.home.newconversation.common.Screen
import com.wire.android.ui.home.newconversation.newGroup.NewGroupScreen

@Composable
fun NewConversationRouter(newConversationViewModel: NewConversationViewModel = hiltViewModel()) {
    val newConversationNavController = rememberNavController()
    val searchNavController = rememberNavController()

    NavHost(navController = newConversationNavController, startDestination = Screen.SearchListNavHostScreens.route) {
        composable(
            route = Screen.SearchListNavHostScreens.route,
            content = {
                SearchListNavigationHost(
                    newConversationNavController = newConversationNavController,
                    searchNavController = searchNavController,
                    newConversationViewModel = newConversationViewModel
                )
            })

        composable(
            route = Screen.NewGroupNameScreen.route,
            content = {
                NewGroupScreen(onBackPressed = { newConversationNavController.popBackStack() })
            })
    }
}
