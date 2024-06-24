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

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.Navigator
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
    val currentNavigationItem: HomeDestination,
    val searchBarState: SearchBarState,
    val navigator: Navigator
) {

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
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    navController: NavHostController = rememberTrackingAnimatedNavController {
        HomeDestination.fromRoute(it)?.itemName
    },
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
): HomeStateHolder {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentNavigationItem = currentRoute?.let { HomeDestination.fromRoute(it) } ?: HomeDestination.Conversations

    val searchBarState = rememberSearchbarState()

    val homeState = remember(
        currentNavigationItem
    ) {
        HomeStateHolder(
            coroutineScope,
            navController,
            drawerState,
            currentNavigationItem,
            searchBarState,
            navigator
        )
    }

    return homeState
}
