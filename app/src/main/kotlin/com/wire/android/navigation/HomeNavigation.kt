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
import com.wire.android.ui.home.HomeState
import com.wire.android.ui.home.archive.ArchiveScreen
import com.wire.android.ui.home.conversationslist.ConversationRouterHomeBridge
import com.wire.android.ui.home.vault.VaultScreen

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterialApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun HomeNavigationGraph(homeState: HomeState, navController: NavHostController, startDestination: String?) {
    NavHost(
        navController = navController,
        startDestination = startDestination ?: HomeNavigationItem.Conversations.route
    ) {
        HomeNavigationItem.all
            .forEach { item ->
                composable(
                    route = item.route,
                    content = item.content(homeState)
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
    val content: (HomeState) -> (@Composable (NavBackStackEntry) -> Unit)
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
                    onBottomSheetVisibilityChanged = homeState::toggleBottomSheetVisibility,
                    onScrollPositionProviderChanged = homeState::updateScrollPositionProvider
                )
            }
        }
    ),

    Vault(
        route = HomeDestinationsRoutes.vault,
        title = R.string.vault_screen_title,
        content = { { VaultScreen() } }
    ),

    Archive(
        route = HomeDestinationsRoutes.archive,
        title = R.string.archive_screen_title,
        content = { { ArchiveScreen() } }
    );

    companion object {
        val all = listOf(Conversations, Archive, Vault)
    }
}

private object HomeDestinationsRoutes {
    const val conversations = "home_conversations"
    const val vault = "home_vault"
    const val archive = "home_archive"
}
