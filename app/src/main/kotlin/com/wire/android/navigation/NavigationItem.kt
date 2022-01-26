package com.wire.android.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import com.wire.android.R
import com.wire.android.ui.main.archive.ArchiveScreen
import com.wire.android.ui.main.conversations.ConversationsScreen
import com.wire.android.ui.main.settings.SettingsScreen
import com.wire.android.ui.main.support.SupportScreen
import com.wire.android.ui.main.userprofile.UserProfileScreen
import com.wire.android.ui.main.vault.VaultScreen

sealed class NavigationItem(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    val addingToBackStack: Boolean = false,
    val content: @Composable (NavBackStackEntry) -> Unit,
    val navigationElements: NavigationElements = NavigationElements.None,
) {

//    object Splash  //TODO
//    object Login  //TODO

    object Conversations : NavigationItem(
        route = "conversations",
        content = { ConversationsScreen(hiltViewModel()) },
        navigationElements = NavigationElements.TopBar.WithDrawer(
            title = R.string.conversations_screen_title,
            isSearchable = true,
            hasUserAvatar = true
        ) as NavigationElements
    )

    object Vault : NavigationItem(
        route = "vault",
        content = { VaultScreen() },
        navigationElements = NavigationElements.TopBar.WithDrawer(
            title = R.string.vault_screen_title,
            hasUserAvatar = true
        ) as NavigationElements
    )

    object Archive : NavigationItem(
        route = "archive",
        content = { ArchiveScreen() },
        navigationElements = NavigationElements.TopBar.WithDrawer(
            title = R.string.archive_screen_title,
            hasUserAvatar = true
        ) as NavigationElements
    )

    object Settings : NavigationItem(
        route = "settings",
        content = { SettingsScreen() },
        addingToBackStack = true,
        navigationElements = NavigationElements.TopBar(
            title = R.string.settings_screen_title,
            btnType = TopBarBtn.BACK
        ) as NavigationElements
    )

    object Support : NavigationItem(
        route = "support",
        content = { SupportScreen() },
        addingToBackStack = true,
        navigationElements = NavigationElements.TopBar(
            title = R.string.support_screen_title,
            btnType = TopBarBtn.BACK
        ) as NavigationElements
    )

    object UserProfile : NavigationItem(
        route = "user_profile",
        content = { UserProfileScreen() },
        addingToBackStack = true,
        navigationElements = NavigationElements.None //TODO
    )

    companion object {

        val values = listOf(
            Archive,
            Conversations,
            Settings,
            Support,
            UserProfile,
            Vault,
        )
        val map: Map<String, NavigationItem> = values.associateBy { it.route }

        fun fromRoute(route: String?): NavigationItem? = map[route]
    }

}

sealed class NavigationElements {

    object None : NavigationElements()

    open class TopBar(
        @StringRes open val title: Int,
        open val btnType: TopBarBtn,
        open val isSearchable: Boolean = false,
        open val hasUserAvatar: Boolean = false
    ) : NavigationElements() {

        data class WithDrawer(
            @StringRes override val title: Int,
            override val isSearchable: Boolean = false,
            override val hasUserAvatar: Boolean = false
        ) : TopBar(title, TopBarBtn.MENU, isSearchable, hasUserAvatar)
    }

}

enum class TopBarBtn {
    CLOSE, BACK, MENU
}
