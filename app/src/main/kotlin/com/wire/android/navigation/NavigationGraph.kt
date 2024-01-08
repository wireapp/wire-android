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

package com.wire.android.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.spec.Route
import com.wire.android.navigation.style.DefaultNestedNavGraphAnimations
import com.wire.android.navigation.style.DefaultRootNavGraphAnimations
import com.wire.android.ui.NavGraphs
import com.wire.android.ui.home.newconversation.NewConversationViewModel

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalAnimationApi::class)
@Composable
fun NavigationGraph(
    navigator: Navigator,
    startDestination: Route,
) {
    val navHostEngine = rememberAnimatedNavHostEngine(
        rootDefaultAnimations = DefaultRootNavGraphAnimations,
        defaultAnimationsForNestedNavGraph = mapOf(
            NavGraphs.createPersonalAccount to DefaultNestedNavGraphAnimations,
            NavGraphs.createTeamAccount to DefaultNestedNavGraphAnimations,
            NavGraphs.newConversation to DefaultNestedNavGraphAnimations,
        )
    )

    DestinationsNavHost(
        navGraph = NavGraphs.root,
        engine = navHostEngine,
        startRoute = startDestination,
        navController = navigator.navController,
        dependenciesContainerBuilder = {
            // 👇 To make Navigator available to all destinations as a non-navigation parameter
            dependency(navigator)

            // 👇 To tie NewConversationViewModel to nested NewConversationNavGraph, making it shared between all screens that belong to it
            dependency(NavGraphs.newConversation) {
                val parentEntry = remember(navBackStackEntry) { navController.getBackStackEntry(NavGraphs.newConversation.route) }
                hiltViewModel<NewConversationViewModel>(parentEntry)
            }
        }
    )
}
