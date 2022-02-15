package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.wire.android.BuildConfig
import com.wire.android.ui.authentication.AuthScreen
import com.wire.android.ui.home.HomeDestinations
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.userprofile.UserProfileRoute

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
sealed class NavigationItem(
    open val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    open val content: @Composable (NavBackStackEntry) -> Unit
    // TODO add animations here
) {

//    object Splash  //TODO

    object Authentication : NavigationItem(
        route = "auth",
        content = { AuthScreen() }
    )

    @ExperimentalMaterialApi
    object Home : NavigationItem(
        route = "home/{$HOME_START_TAB_ARGUMENT}",
        content = { HomeScreen(it.arguments?.getString(HOME_START_TAB_ARGUMENT), hiltViewModel()) },
        arguments = listOf(
            navArgument(HOME_START_TAB_ARGUMENT) { type = NavType.StringType }
        )
    ) {
        fun navigationRoute(startTabRoute: String = HomeDestinations.conversations): String = "home/$startTabRoute"
    }

    object Settings : NavigationItem(
        route = "settings",
        content = { SettingsScreen() },
    )

    object Support : NavigationItem(
        route = BuildConfig.SUPPORT_URL,
        content = { },
    )

    object UserProfile : NavigationItem(
        route = "user_profile",
        content = { UserProfileRoute(hiltViewModel()) },
    )

    object Conversation : NavigationItem(
        route = "conversation/{$CONVERSATION_ID_ARGUMENT}",
        content = {
            ConversationScreen(hiltViewModel())
        },
        arguments = listOf(
            navArgument(CONVERSATION_ID_ARGUMENT) { type = NavType.StringType }
        )
    ) {
        fun createRoute(conversationId: String) = "conversation/$conversationId"
    }

    companion object {
        const val HOME_START_TAB_ARGUMENT: String = "start_tab_index"
        const val CONVERSATION_ID_ARGUMENT: String = "conversation_id"

        @ExperimentalMaterialApi
        val globalNavigationItems = listOf(
            Authentication,
            Settings,
            Support,
            UserProfile,
            Home,
            Conversation
        )

        @OptIn(ExperimentalMaterialApi::class)
        private val map: Map<String, NavigationItem> = globalNavigationItems.associateBy { it.route }

        fun fromRoute(route: String?): NavigationItem? = map[route]
    }
}

fun NavigationItem.isExternalRoute() = this.route.startsWith("http")
