/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.ui.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeDestination.Archive
import com.wire.android.navigation.HomeDestination.Conversations
import com.wire.android.navigation.HomeDestination.Favorites
import com.wire.android.navigation.HomeDestination.Group
import com.wire.android.navigation.HomeDestination.OneOnOne
import com.wire.android.navigation.HomeDestination.Settings
import com.wire.android.navigation.HomeDestination.Support
import com.wire.android.navigation.HomeDestination.Vault
import com.wire.android.navigation.HomeDestination.WhatsNew
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.getBaseRoute
import com.wire.android.navigation.rememberTrackingAnimatedNavController
import com.wire.android.ui.common.topappbar.search.SearchBarState
import com.wire.android.ui.common.topappbar.search.rememberSearchbarState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class HomeStateHolder(
    val coroutineScope: CoroutineScope,
    val navController: NavHostController,
    val drawerState: DrawerState,
    val searchBarState: SearchBarState,
    val navigator: Navigator,
    private val currentNavigationItemState: State<HomeDestination>,
    private val lazyListStates: Map<HomeDestination, LazyListState>,
) {
    val currentNavigationItem
        get() = currentNavigationItemState.value

    fun lazyListStateFor(destination: HomeDestination): LazyListState {
        return lazyListStates[destination] ?: error("No LazyListState found for $destination")
    }

    fun nullAbleLazyListStateFor(destination: HomeDestination): LazyListState? {
        return lazyListStates[destination]
    }

    fun closeDrawer() {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    fun openDrawer() {
        coroutineScope.launch {
            drawerState.open()
        }
    }
}

@Composable
fun rememberHomeScreenState(
    navigator: Navigator,
    homeDestinations: List<HomeDestination> = listOf(
        Conversations,
        Favorites,
        OneOnOne,
        Group,
        Settings,
        Vault,
        Archive,
        Support,
        WhatsNew
    ),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberTrackingAnimatedNavController { route ->
        homeDestinations.find { it.direction.route.getBaseRoute() == route }?.itemName
    },
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed)
): HomeStateHolder {

    val searchBarState = rememberSearchbarState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentNavigationItemState = remember {
        derivedStateOf {
            HomeDestination.getArgumentsFromEntry(navBackStackEntry)?.let { args ->
                return@derivedStateOf HomeDestination.Folder(args.folderId, args.folderName)
            } ?: navBackStackEntry?.destination?.route?.let { HomeDestination.fromRoute(it) ?: Conversations } ?: Conversations
        }
    }
    val lazyListStates = homeDestinations.associateWith { rememberLazyListState() }

    return remember(homeDestinations) {
        HomeStateHolder(
            coroutineScope = coroutineScope,
            navController = navController,
            drawerState = drawerState,
            searchBarState = searchBarState,
            navigator = navigator,
            currentNavigationItemState = currentNavigationItemState,
            lazyListStates = lazyListStates
        )
    }
}
