package com.wire.android.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wire.android.R
import com.wire.android.ui.home.HomeUIState
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.settings.SettingsScreen

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeNavigationGraph(homeUIState: HomeUIState, navController: NavHostController, startDestination: HomeNavigationItem) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route
    ) {
        HomeNavigationItem.values()
            .forEach { item ->
                composable(
                    route = item.route,
                    content = item.content(homeUIState)
                )
            }
    }
}

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
enum class HomeNavigationItem(
    val route: String,
    @StringRes val title: Int,
    val isSearchable: Boolean = false,
    val isSwipeable: Boolean = true,
    val content: (HomeUIState) -> (@Composable (NavBackStackEntry) -> Unit)
) {
    Conversations(
        route = HomeDestinationsRoutes.conversations,
        title = R.string.conversations_screen_title,
        isSearchable = true,
        isSwipeable = false,
        content = { homeState ->
            {
                ConversationRouterHomeBridge(
                    onHomeBottomSheetContentChanged = homeState::changeBottomSheetContent,
                    onOpenBottomSheet = homeState::openBottomSheet,
                    onSnackBarStateChanged = homeState::setSnackBarState,
                    onScrollPositionProviderChanged = homeState::updateScrollPositionProvider
                )
            }
        }
    ),

    Settings(
        route = HomeDestinationsRoutes.SETTINGS,
        title = R.string.settings_screen_title,
        content = { homeUIState -> { SettingsScreen(homeUIState.lazyListState) } }
    );

// TODO: Re-enable once we have vault
//    Vault(
//        route = HomeDestinationsRoutes.vault,
//        title = R.string.vault_screen_title,
//        content = { { VaultScreen() } }
//    ),

// TODO: Re-enable once we have Archive
//    Archive(
//        route = HomeDestinationsRoutes.archive,
//        title = R.string.archive_screen_title,
//        content = { { ArchiveScreen() } }
//    );
}

private object HomeDestinationsRoutes {
    const val conversations = "home_conversations"
    const val vault = "home_vault"
    const val archive = "home_archive"
    const val SETTINGS = "home_settings"
}
