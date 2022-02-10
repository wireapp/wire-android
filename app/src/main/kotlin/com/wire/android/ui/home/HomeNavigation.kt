package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.archive.ArchiveScreen
import com.wire.android.ui.home.conversationslist.ConversationRouter
import com.wire.android.ui.home.vault.VaultScreen

@ExperimentalMaterialApi
@ExperimentalMaterial3Api
@Composable
fun HomeNavigationGraph(
    navController: NavHostController,
    startDestination: String?,
    updateScrollPosition: (Int) -> Unit = {}
) {
    NavHost(
        modifier = Modifier.padding(top = dimensions().smallTopBarHeight),
        navController = navController,
        startDestination = startDestination ?: HomeNavigationItem.Conversations.route
    ) {
        HomeNavigationItem.Conversations.run {
            composable(route = route, content = { ConversationRouter(updateScrollPosition) })
        }
        HomeNavigationItem.Archive.run {
            composable(route = route, content = { ArchiveScreen() })
        }
        HomeNavigationItem.Vault.run {
            composable(route = route, content = { VaultScreen() })
        }
    }
}

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
sealed class HomeNavigationItem(
    val route: String,
    @StringRes val title: Int,
    val isSearchable: Boolean = false,
    val isSwipeable: Boolean = true
) {

    object Conversations : HomeNavigationItem(
        route = HomeDestinations.conversations,
        title = R.string.conversations_screen_title,
        isSearchable = true,
        isSwipeable = false
    )

    object Vault : HomeNavigationItem(
        route = HomeDestinations.vault,
        title = R.string.vault_screen_title
    )

    object Archive : HomeNavigationItem(
        route = HomeDestinations.archive,
        title = R.string.archive_screen_title
    )

    companion object {

        val all = listOf(Conversations, Archive, Vault)

        @Composable
        fun getCurrentNavigationItem(controller: NavController): HomeNavigationItem {
            val navBackStackEntry by controller.currentBackStackEntryAsState()

            return when (navBackStackEntry?.destination?.route) {
                Archive.route -> Archive
                Vault.route -> Vault
                else -> Conversations
            }
        }
    }
}

object HomeDestinations {
    const val conversations = "home_conversations"
    const val vault = "home_vault"
    const val archive = "home_archive"
}
