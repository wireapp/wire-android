package com.wire.android.ui.main

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.ui.main.archive.ArchiveScreen
import com.wire.android.ui.main.conversation.ConversationRoute
import com.wire.android.ui.main.conversation.ConversationViewModel
import com.wire.android.ui.main.navigation.MainDrawer
import com.wire.android.ui.main.navigation.MainNavigationScreenItem
import com.wire.android.ui.main.navigation.MainTopBar
import com.wire.android.ui.main.navigation.getCurrentNavigationItem
import com.wire.android.ui.main.settings.SettingsScreen
import com.wire.android.ui.main.support.SupportScreen
import com.wire.android.ui.main.userprofile.UserProfileScreen
import com.wire.android.ui.main.vault.VaultScreen
import kotlinx.coroutines.CoroutineScope

@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainScreen() {
    val wireAppState = rememberWireAppState()

    Scaffold(
        scaffoldState = wireAppState.scaffoldState,
        topBar = {
            MainTopBar(
                wireAppState,
                onTextChanged
            )
        },
        drawerContent = {
            MainDrawer(wireAppState)
        },
    ) {
        NavHost(wireAppState.navController, startDestination = MainNavigationScreenItem.Conversations.route) {
            composable(route = MainNavigationScreenItem.Conversations.route, content = {
                val conversationViewModel = ConversationViewModel()

                ConversationRoute(
                    viewModel = conversationViewModel,
                    onSearchInputChanged = { conversationViewModel.search(it) }
                )
            })
            composable(route = MainNavigationScreenItem.Vault.route, content = { VaultScreen() })
            composable(route = MainNavigationScreenItem.Archive.route, content = { ArchiveScreen() })
            composable(route = MainNavigationScreenItem.UserProfile.route, content = { UserProfileScreen() })
            composable(route = MainNavigationScreenItem.Settings.route, content = { SettingsScreen() })
            composable(route = MainNavigationScreenItem.Support.route, content = { SupportScreen() })
        }
    }
}


@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}

@Composable
fun rememberWireAppState(
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
) = remember(scaffoldState, navController, coroutineScope) {
    WireAppState(scaffoldState, navController, coroutineScope)
}

class WireAppState(
    val scaffoldState: ScaffoldState,
    val navController: NavHostController,
    val coroutineScope: CoroutineScope,
) {

    val drawerState
        get() = scaffoldState.drawerState

    val currentNavigationItem
        @Composable get() = navController.getCurrentNavigationItem()

    val shouldShowSearchBar: Boolean
        @Composable get() = currentNavigationItem?.hasSearchableTopBar ?: false
}
