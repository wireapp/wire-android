package com.wire.android.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import com.wire.android.R
import com.wire.android.ui.main.archive.ArchiveScreen
import com.wire.android.ui.main.convesations.ConversationsScreen
import com.wire.android.ui.main.settings.SettingsScreen
import com.wire.android.ui.main.support.SupportScreen
import com.wire.android.ui.main.userprofile.UserProfileScreen
import com.wire.android.ui.main.vault.VaultScreen

sealed class NavigationItem(
    val route: String,
    val arguments: List<NamedNavArgument> = emptyList(),
    val addingToBackStack: Boolean = false,
    val content: @Composable (NavBackStackEntry) -> Unit,
    val type: NavigationType = NavigationType.None,
) {

//    object Splash  //TODO
//    object Login  //TODO

    object Conversations : NavigationItem(
        route = "conversations",
        content = { ConversationsScreen(hiltViewModel()) },
        type = NavigationType.WithTopBar.WithDrawer(
            title = R.string.conversations_screen_title,
            isSearchable = true,
            hasUserAvatar = true
        ) as NavigationType
    )

    object Vault : NavigationItem(
        route = "vault",
        content = { VaultScreen() },
        type = NavigationType.WithTopBar.WithDrawer(
            title = R.string.vault_screen_title,
            hasUserAvatar = true
        ) as NavigationType
    )

    object Archive : NavigationItem(
        route = "archive",
        content = { ArchiveScreen() },
        type = NavigationType.WithTopBar.WithDrawer(
            title = R.string.archive_screen_title,
            hasUserAvatar = true
        ) as NavigationType
    )

    object Settings : NavigationItem(
        route = "settings",
        content = { SettingsScreen() },
        addingToBackStack = true,
        type = NavigationType.WithTopBar(
            title = R.string.settings_screen_title,
            btnType = TopBarBtn.BACK
        ) as NavigationType
    )

    object Support : NavigationItem(
        route = "support",
        content = { SupportScreen() },
        addingToBackStack = true,
        type = NavigationType.WithTopBar(
            title = R.string.support_screen_title,
            btnType = TopBarBtn.BACK
        ) as NavigationType
    )

    object UserProfile : NavigationItem(
        route = "user_profile",
        content = { UserProfileScreen() },
        addingToBackStack = true,
        type = NavigationType.None //TODO
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

sealed class NavigationType {

    object None : NavigationType()

    open class WithTopBar(
        @StringRes open val title: Int,
        open val btnType: TopBarBtn,
        open val isSearchable: Boolean = false,
        open val hasUserAvatar: Boolean = false
    ) : NavigationType() {

        data class WithDrawer(
            @StringRes override val title: Int,
            override val isSearchable: Boolean = false,
            override val hasUserAvatar: Boolean = false
        ) : WithTopBar(title, TopBarBtn.MENU, isSearchable, hasUserAvatar)
    }

}

enum class TopBarBtn {
    CLOSE, BACK, MENU
}
