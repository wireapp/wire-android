/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.Navigator
import androidx.navigation.compose.navigation
import com.ramcosta.composedestinations.animations.NavHostAnimatedDestinationStyle
import com.ramcosta.composedestinations.manualcomposablecalls.ManualComposableCalls
import com.ramcosta.composedestinations.manualcomposablecalls.allDeepLinks
import com.ramcosta.composedestinations.navigation.DependenciesContainerBuilder
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.spec.NavGraphSpec
import com.ramcosta.composedestinations.spec.NavHostEngine
import com.ramcosta.composedestinations.spec.TypedDestinationSpec
import com.wire.android.navigation.wrapper.setTabletDialogRouteMatcher

@Composable
internal fun rememberWireNavHostEngine(
    navHostContentAlignment: Alignment = Alignment.Center,
): NavHostEngine = remember(navHostContentAlignment) {
    // Keep wrapper clipping aligned with the same tablet-dialog route policy used by style resolution.
    setTabletDialogRouteMatcher { route ->
        TabletDialogRoutePolicy.shouldShowAsDialog(route.getBaseRoute())
    }
    WireNavHostEngine(
        navHostContentAlignment = navHostContentAlignment,
    )
}

/**
 * a custom NavHostEngine implementation for Wire that:
 * 1. Sets a default content alignment for NavHost composable.
 * 2. Implements custom logic for determining when to apply animated transitions to navigation graphs and destinations
 * 3. Implements custom logic for determining when to apply dialog-style presentation to destinations, based on a tablet parity policy.
 *   This is done to ensure that all routes that should be presented as dialogs on tablets are registered as such in the NavGraph, even if they are not registered as dialogs on phones.
 *
 *   Note: MUST BE KEPT IN SYNC WITH UPSTREAM WHEN UPDATING COMPOSE DESTINATIONS DEPENDENCY
 */
// Required because compose-destinations 2.3.0 exposes ManualComposableCalls
// (@RestrictTo(LIBRARY_GROUP)) in the public NavHostEngine interface; implementing
// that interface outside the library otherwise triggers RestrictedApi lint.
@SuppressLint("RestrictedApi")
internal class WireNavHostEngine(
    private val navHostContentAlignment: Alignment,
) : NavHostEngine {

    override val type = NavHostEngine.Type.DEFAULT

    @Composable
    override fun rememberNavController(
        vararg navigators: Navigator<out NavDestination>
    ) = androidx.navigation.compose.rememberNavController(*navigators)

    @Composable
    override fun NavHost(
        modifier: Modifier,
        route: String,
        start: Direction,
        defaultTransitions: NavHostAnimatedDestinationStyle,
        navController: NavHostController,
        builder: NavGraphBuilder.() -> Unit,
    ) = with(defaultTransitions) {
        androidx.navigation.compose.NavHost(
            navController = navController,
            startDestination = start.route,
            modifier = modifier,
            route = route,
            contentAlignment = navHostContentAlignment,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition,
            sizeTransform = sizeTransform,
            builder = builder
        )
    }

    override fun NavGraphBuilder.navigation(
        navGraph: NavGraphSpec,
        manualComposableCalls: ManualComposableCalls,
        builder: NavGraphBuilder.() -> Unit
    ) {
        val transitions = manualComposableCalls.manualAnimation(navGraph.route)
            ?: navGraph.defaultTransitions

        if (transitions != null) {
            with(transitions) {
                navigation(
                    startDestination = navGraph.startRoute.route,
                    route = navGraph.route,
                    arguments = navGraph.arguments,
                    deepLinks = navGraph.allDeepLinks(manualComposableCalls),
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    popEnterTransition = popEnterTransition,
                    popExitTransition = popExitTransition,
                    sizeTransform = sizeTransform,
                    builder = builder,
                )
            }
        } else {
            navigation(
                startDestination = navGraph.startRoute.route,
                route = navGraph.route,
                arguments = navGraph.arguments,
                deepLinks = navGraph.allDeepLinks(manualComposableCalls),
                builder = builder
            )
        }
    }

    override fun <T> NavGraphBuilder.composable(
        destination: TypedDestinationSpec<T>,
        navController: NavHostController,
        dependenciesContainerBuilder: @Composable DependenciesContainerBuilder<*>.() -> Unit,
        manualComposableCalls: ManualComposableCalls,
    ) {
        // Force true dialog registration for tablet parity routes; keep default/manual behavior otherwise.
        val resolvedStyle = resolveTabletDialogParityStyle(
            destinationRoute = destination.route,
            originalStyle = destination.style,
            manualAnimation = manualComposableCalls.manualAnimation(destination.route),
            isTablet = navController.context.resources.configuration.smallestScreenWidthDp >= TABLET_MIN_SCREEN_WIDTH_DP,
        )
        with(resolvedStyle) {
            addComposable(destination, navController, dependenciesContainerBuilder, manualComposableCalls)
        }
    }
}

private const val TABLET_MIN_SCREEN_WIDTH_DP = 600
