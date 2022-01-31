package com.wire.android.ui.home

import androidx.annotation.StringRes
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.R
import com.wire.android.ui.home.archive.ArchiveScreen
import com.wire.android.ui.home.conversationlist.ConversationRoute
import com.wire.android.ui.home.conversations.ConversationScreenPreview
import com.wire.android.ui.home.vault.VaultScreen

@ExperimentalMaterial3Api
@Composable
fun HomeNavigationGraph(navController: NavHostController, startDestination: String?) {
    NavHost(navController, startDestination = startDestination ?: HomeNavigationItem.Conversations.route) {
        HomeNavigationItem.all
            .forEach { item ->
                composable(route = item.route, content = item.content)
            }
    }
}

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

@ExperimentalMaterial3Api
sealed class HomeNavigationItem(
    val route: String,
    @StringRes val title: Int,
    val isSearchable: Boolean = false,
    val content: @Composable (NavBackStackEntry) -> Unit
) {

    object Conversations : HomeNavigationItem(
        route = "home_conversations",
        title = R.string.conversations_screen_title,
        isSearchable = true,
        content = { ConversationScreenPreview() },
    )

    object Vault : HomeNavigationItem(
        route = "home_vault",
        title = R.string.vault_screen_title,
        content = { VaultScreen() },
    )

    object Archive : HomeNavigationItem(
        route = "home_archive",
        title = R.string.archive_screen_title,
        content = { ArchiveScreen() },
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

