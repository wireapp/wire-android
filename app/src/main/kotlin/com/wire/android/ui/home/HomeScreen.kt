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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import com.wire.android.navigation.HomeDestination
import com.wire.android.ui.home.drawer.HomeDrawerState

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeContent(
    homeState: HomeState,
    homeDrawerState: HomeDrawerState,
    homeStateHolder: HomeShellState,
    onNewConversationClick: () -> Unit,
    onSelfUserClick: () -> Unit,
    onNavigateToHomeItem: (HomeDestination) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val fabFocusRequester = remember { FocusRequester() }
    val drawerFocusRequester = remember { FocusRequester() }
    val firstDrawerItemFocusRequester = remember { FocusRequester() }
    val lastDrawerItemFocusRequester = remember { FocusRequester() }
    var isDrawerSheetFocused by remember { mutableStateOf(false) }

    LaunchedEffect(homeStateHolder.drawerState.isOpen) {
        if (homeStateHolder.drawerState.isOpen) {
            withFrameNanos { }
            if (!firstDrawerItemFocusRequester.requestFocus()) {
                drawerFocusRequester.requestFocus()
            }
        }
    }

    with(homeStateHolder) {
        fun closeHomeDrawer() {
            focusManager.clearFocus(force = true)
            closeDrawer()
        }

        fun requestDrawerItemFocus(isShiftPressed: Boolean) {
            if (isShiftPressed) {
                lastDrawerItemFocusRequester.requestFocus()
            } else {
                firstDrawerItemFocusRequester.requestFocus()
            }
        }

        val drawerSheetFocusTrapState = HomeDrawerSheetFocusTrapState(
            enabled = drawerState.isOpen,
            focusRequester = drawerFocusRequester,
            isSheetFocused = isDrawerSheetFocused
        )
        val drawerSheetFocusTrapActions = HomeDrawerSheetFocusTrapActions(
            onSheetFocusChanged = { isDrawerSheetFocused = it },
            onItemFocusRequested = ::requestDrawerItemFocus,
            onClose = ::closeHomeDrawer
        )

        ModalNavigationDrawer(
            modifier = modifier.homeDrawerKeyboardNavigation(
                isDrawerOpen = drawerState.isOpen,
                isDrawerSheetFocused = isDrawerSheetFocused,
                onDrawerItemFocusRequested = ::requestDrawerItemFocus,
                onCloseDrawer = ::closeHomeDrawer
            ),
            drawerState = drawerState,
            drawerContent = {
                HomeDrawerSheet(
                    currentDestination = currentNavigationItem,
                    homeDrawerState = homeDrawerState,
                    focusTrapState = drawerSheetFocusTrapState,
                    focusTrapActions = drawerSheetFocusTrapActions,
                    firstItemFocusRequester = firstDrawerItemFocusRequester,
                    lastItemFocusRequester = lastDrawerItemFocusRequester,
                    onNavigateToHomeItem = onNavigateToHomeItem,
                    onCloseDrawer = ::closeHomeDrawer
                )
            },
            gesturesEnabled = drawerState.isOpen,
            content = {
                HomeScaffold(
                    homeState = homeState,
                    state = remember(homeStateHolder) { HomeScaffoldState(homeStateHolder) },
                    drawerState = drawerState,
                    focusRequesters = HomeScaffoldFocusRequesters(
                        search = searchFocusRequester,
                        fab = fabFocusRequester
                    ),
                    actions = HomeScaffoldActions(
                        onDrawerItemFocusRequested = ::requestDrawerItemFocus,
                        onNewConversationClick = onNewConversationClick,
                        onSelfUserClick = onSelfUserClick,
                        onHamburgerMenuClick = ::openDrawer
                    ),
                    content = content,
                )
            }
        )
    }
}
