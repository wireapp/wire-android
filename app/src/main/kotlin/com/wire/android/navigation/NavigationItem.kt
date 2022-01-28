package com.wire.android.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.support.SupportScreen
import com.wire.android.ui.home.userprofile.UserProfileScreen

@ExperimentalMaterial3Api
open class NavigationItem(
    open val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    open val content: @Composable (NavBackStackEntry) -> Unit
    //TODO add animations here
) {

//    object Splash  //TODO
//    object Login  //TODO

    object Home : NavigationItem(
        route = "home/{$HOME_START_TAB_ARGUMENT}",
        content = { HomeScreen(it.arguments?.getString(HOME_START_TAB_ARGUMENT), hiltViewModel()) },
        arguments = listOf(
            navArgument(HOME_START_TAB_ARGUMENT) { type = NavType.StringType }
        )
    ) {
        fun navigationRoute(startTabRoute: String): String = "home/$startTabRoute"
    }

    object Settings : NavigationItem(
        route = "settings",
        content = { SettingsScreen() },
    )

    object Support : NavigationItem(
        route = "support",
        content = { SupportScreen() },
    )

    object UserProfile : NavigationItem(
        route = "user_profile",
        content = { UserProfileScreen() },
    )

    companion object {

        const val HOME_START_TAB_ARGUMENT: String = "start_tab_index"

        val globalNavigationItems = listOf(
            Settings,
            Support,
            UserProfile,
            Home
        )
        val map: Map<String, NavigationItem> = globalNavigationItems.associateBy { it.route }

        fun fromRoute(route: String?): NavigationItem? = map[route]
    }

}
