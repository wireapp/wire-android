package com.wire.android.navigation

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import com.wire.android.BuildConfig
import com.wire.android.ui.authentication.AuthScreen
import com.wire.android.ui.home.HomeScreen
import com.wire.android.ui.home.conversations.ConversationScreen
import com.wire.android.ui.settings.SettingsScreen
import com.wire.android.ui.userprofile.UserProfileScreen

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterialApi::class
)
enum class NavigationItem(
    private val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    open val content: @Composable (NavBackStackEntry) -> Unit
    // TODO add animations here
) {

    Authentication(
        route = "auth",
        content = { AuthScreen() }
    ),

    Home(
        route = "home",
        content = { HomeScreen(it.arguments?.getString(EXTRA_HOME_TAB_ITEM), hiltViewModel()) },
    ),

    Settings(
        route = "settings",
        content = { SettingsScreen() },
    ),

    Support(
        route = BuildConfig.SUPPORT_URL,
        content = { },
    ),

    UserProfile(
        route = "user_profile",
        content = { UserProfileScreen(it.arguments?.getString(EXTRA_USER_ID), hiltViewModel()) },
    ),

    Conversation(
        route = "conversation",
        content = {
            ConversationScreen(hiltViewModel())
        }
    );

    fun getRoute(extraRouteId: String = ""): String = "$route/$extraRouteId}"

    companion object {
        const val EXTRA_HOME_TAB_ITEM = "extra_home_tab_item"
        const val EXTRA_USER_ID = "extra_user_id"

        @OptIn(ExperimentalMaterialApi::class)
        private val map: Map<String, NavigationItem> = values().associateBy { it.route }

        fun fromRoute(route: String?): NavigationItem? = map[route]
    }
}

fun NavigationItem.isExternalRoute() = this.getRoute().startsWith("http")
