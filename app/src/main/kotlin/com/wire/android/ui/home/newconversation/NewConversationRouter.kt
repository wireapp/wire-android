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
    val navController = rememberNavController()
    val listNavController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.SearchListNavHostScreens.route) {
        composable(
            route = Screen.SearchListNavHostScreens.route,
            content = {
                SearchListNavigationHost(
                    navController = navController,
                    listNavController = listNavController,
                    newConversationViewModel = newConversationViewModel
                )
            })

        composable(
            route = Screen.NewGroupNameScreen.route,
            content = {
                NewGroupScreen(onBackPressed = { navController.popBackStack() })
            })
    }
}
