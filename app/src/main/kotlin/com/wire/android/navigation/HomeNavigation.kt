package com.wire.android.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import com.wire.android.R
import com.wire.android.ui.common.WireBottomNavigationItemData
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.ui.home.archive.ArchiveScreen
import com.wire.android.ui.home.conversationslist.ConversationItemType
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.settings.SettingsScreen
import com.wire.android.ui.home.vault.VaultScreen

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalMaterial3Api
internal fun navigateToItemInHome(
    navController: NavController,
    item: HomeNavigationItem
) {
    navController.navigate(item.route) {
        navController.graph.startDestinationRoute?.let { route ->
            popUpTo(route) {
                saveState = true
            }
        }
        launchSingleTop = true
        restoreState = true
    }
}

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@ExperimentalAnimationApi
@Suppress("LongParameterList")
enum class HomeNavigationItem(
    val route: String,
    @StringRes val title: Int,
    @StringRes val tabName: Int = title,
    @DrawableRes val icon: Int,
    val isSearchable: Boolean = false,
    val withNewConversationFab: Boolean = false,
    val content: (HomeStateHolder) -> (@Composable (NavBackStackEntry) -> Unit)
) {
    Conversations(
        route = HomeDestinationsRoutes.conversations,
        title = R.string.conversations_screen_title,
        tabName = R.string.conversations_all_tab_title,
        icon = R.drawable.ic_conversation,
        isSearchable = true,
        withNewConversationFab = true,
        content = { homeState ->
            {
                ConversationRouterHomeBridge(
                    conversationItemType = ConversationItemType.ALL_CONVERSATIONS,
                    onHomeBottomSheetContentChanged = homeState::changeBottomSheetContent,
                    onOpenBottomSheet = homeState::openBottomSheet,
                    onCloseBottomSheet = homeState::closeBottomSheet,
                    onSnackBarStateChanged = homeState::setSnackBarState,
                    searchBarState = homeState.searchBarState
                )
            }
        }
    ),

    Calls(
        route = HomeDestinationsRoutes.calls,
        title = R.string.conversations_calls_tab_title,
        icon = R.drawable.ic_call,
        isSearchable = true,
        withNewConversationFab = true,
        content = { homeState ->
            {
                ConversationRouterHomeBridge(
                    conversationItemType = ConversationItemType.CALLS,
                    onHomeBottomSheetContentChanged = homeState::changeBottomSheetContent,
                    onOpenBottomSheet = homeState::openBottomSheet,
                    onCloseBottomSheet = homeState::closeBottomSheet,
                    onSnackBarStateChanged = homeState::setSnackBarState,
                    searchBarState = homeState.searchBarState
                )
            }
        }
    ),

    Mentions(
        route = HomeDestinationsRoutes.mentions,
        title = R.string.conversations_mentions_tab_title,
        icon = R.drawable.ic_mention,
        isSearchable = true,
        withNewConversationFab = true,
        content = { homeState ->
            {
                ConversationRouterHomeBridge(
                    conversationItemType = ConversationItemType.MENTIONS,
                    onHomeBottomSheetContentChanged = homeState::changeBottomSheetContent,
                    onOpenBottomSheet = homeState::openBottomSheet,
                    onCloseBottomSheet = homeState::closeBottomSheet,
                    onSnackBarStateChanged = homeState::setSnackBarState,
                    searchBarState = homeState.searchBarState
                )
            }
        }
    ),
    Settings(
        route = HomeDestinationsRoutes.settings,
        title = R.string.settings_screen_title,
        icon = R.drawable.ic_settings,
        content =
        { { SettingsScreen() } }
    ),

    Vault(
        route = HomeDestinationsRoutes.vault,
        title = R.string.vault_screen_title,
        icon = R.drawable.ic_vault,
        content =
        { { VaultScreen() } }
    ),

    Archive(
        route = HomeDestinationsRoutes.archive,
        title = R.string.archive_screen_title,
        icon = R.drawable.ic_archive,
        content =
        { { ArchiveScreen() } }
    );

    val withBottomTabs: Boolean get() = bottomTabItems.contains(this)

    fun toBottomNavigationItemData(notificationAmount: Long): WireBottomNavigationItemData =
        WireBottomNavigationItemData(icon, tabName, notificationAmount, route)

    companion object {
        // TODO uncomment when CallsScreen and MentionScreen will be implemented
//         val bottomTabItems = listOf(Conversations, Calls, Mentions)
        val bottomTabItems = listOf<HomeNavigationItem>()
    }
}

private object HomeDestinationsRoutes {
    const val conversations = "home_all_conversations"
    const val calls = "home_calls"
    const val mentions = "home_mentions"
    const val vault = "home_vault"
    const val archive = "home_archive"
    const val settings = "home_settings"
}
