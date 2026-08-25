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

package com.wire.android.navigation.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.wire.android.di.metro.AppAuthenticationViewModelGraph
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.metro.WireApplicationGraph
import com.wire.android.model.LocalWireSessionImageLoader
import com.wire.android.navigation.navigation3.WireEntryEnvironment
import com.wire.android.ui.authentication.LocalAuthenticationCancelUserId
import com.wire.android.ui.userprofile.self.LocalSelfUserProfileLogoutAction
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

/**
 * The Metro dependencies that are safe to expose to one Navigation 3 entry.
 */
internal data class MetroWireEntryGraph(
    val graph: MetroViewModelGraph,
    val viewModelFactory: MetroViewModelFactory,
    val sessionGraph: AppSessionViewModelGraph?,
)

internal sealed interface MetroWireEntryGraphResolution {

    data class Available(val entryGraph: MetroWireEntryGraph) : MetroWireEntryGraphResolution

    data class Unavailable(val routeScope: WireRouteScope.Unavailable) : MetroWireEntryGraphResolution
}

/**
 * Resolves a typed route to the graph owning that route.
 *
 * Authentication entries deliberately use the application factory, matching the existing
 * WireActivity behavior. Session entries use the factory from the lifecycle-retained graph of the
 * account encoded by the route itself.
 */
internal class MetroWireEntryGraphResolver(
    private val appGraph: WireApplicationGraph,
    private val authenticationGraph: AppAuthenticationViewModelGraph,
    private val sessionGraphStore: SessionGraphStoreViewModel,
) {

    fun resolve(route: WireRoute): MetroWireEntryGraphResolution =
        when (val routeScope = WireRouteScopeResolver.resolve(route)) {
            WireRouteScope.Authentication -> MetroWireEntryGraphResolution.Available(
                MetroWireEntryGraph(
                    graph = authenticationGraph,
                    viewModelFactory = appGraph.metroViewModelFactory,
                    sessionGraph = null,
                )
            )

            is WireRouteScope.Session -> {
                val userId = routeScope.sessionId.toKaliumUserId()
                try {
                    val sessionGraph = sessionGraphStore.graphFor(userId)
                    MetroWireEntryGraphResolution.Available(
                        MetroWireEntryGraph(
                            graph = sessionGraph,
                            viewModelFactory = sessionGraph.metroViewModelFactory,
                            sessionGraph = sessionGraph,
                        )
                    )
                } catch (failure: SessionGraphUnavailableException) {
                    val reason = when (failure.lifecycle) {
                        SessionGraphStoreViewModel.Lifecycle.INVALIDATING ->
                            WireRouteScope.Unavailable.Reason.SESSION_GRAPH_INVALIDATING

                        SessionGraphStoreViewModel.Lifecycle.REMOVED ->
                            WireRouteScope.Unavailable.Reason.SESSION_GRAPH_REMOVED

                        SessionGraphStoreViewModel.Lifecycle.ACTIVE -> error(
                            "Session graph failed with an active lifecycle"
                        )
                    }
                    MetroWireEntryGraphResolution.Unavailable(WireRouteScope.Unavailable(reason))
                }
            }

            is WireRouteScope.Unavailable -> MetroWireEntryGraphResolution.Unavailable(routeScope)
        }
}

/**
 * App-owned bridge between generic Navigation 3 entries and Metro.
 *
 * Keeping this in `app` preserves the dependency direction: `core:navigation` knows only about
 * [WireEntryEnvironment], while the application decides which concrete Metro graph owns a route.
 */
internal class MetroWireEntryEnvironment(
    private val graphResolver: MetroWireEntryGraphResolver,
    private val logoutAction: (wipeData: Boolean) -> Unit,
) : WireEntryEnvironment {

    constructor(
        appGraph: WireApplicationGraph,
        authenticationGraph: AppAuthenticationViewModelGraph,
        sessionGraphStore: SessionGraphStoreViewModel,
        logoutAction: (wipeData: Boolean) -> Unit,
    ) : this(
        graphResolver = MetroWireEntryGraphResolver(
            appGraph = appGraph,
            authenticationGraph = authenticationGraph,
            sessionGraphStore = sessionGraphStore,
        ),
        logoutAction = logoutAction,
    )

    @Composable
    override fun Provide(
        route: WireRoute,
        content: @Composable () -> Unit,
    ) {
        when (val resolution = graphResolver.resolve(route)) {
            is MetroWireEntryGraphResolution.Available -> {
                val entryGraph = resolution.entryGraph
                WireNavigationDiagnostics.metro(
                    route = route,
                    scope = if (entryGraph.sessionGraph == null) "authentication" else "session",
                    outcome = "available",
                )
                CompositionLocalProvider(
                    LocalMetroViewModelFactory provides entryGraph.viewModelFactory,
                    LocalAuthenticationCancelUserId provides
                        (route as? SessionRoute)?.sessionId?.toKaliumUserId(),
                    LocalWireSessionImageLoader provides entryGraph.sessionGraph?.wireSessionImageLoader,
                    LocalSelfUserProfileLogoutAction provides logoutAction,
                ) {
                    content()
                }
            }

            is MetroWireEntryGraphResolution.Unavailable -> {
                WireNavigationDiagnostics.metro(
                    route = route,
                    scope = "unavailable",
                    outcome = resolution.routeScope.reason.toString(),
                )
                when (resolution.routeScope.reason) {
                    WireRouteScope.Unavailable.Reason.SESSION_GRAPH_INVALIDATING,
                    WireRouteScope.Unavailable.Reason.SESSION_GRAPH_REMOVED -> Unit
                    WireRouteScope.Unavailable.Reason.ROUTE_HAS_NO_SCOPE,
                    WireRouteScope.Unavailable.Reason.ROUTE_HAS_MULTIPLE_SCOPES -> error(
                        "No Metro graph is available for route ${route::class.qualifiedName}: " +
                            resolution.routeScope.reason
                    )
                }
            }
        }
    }
}

internal fun WireSessionId.toKaliumUserId(): UserId = UserId(value = value, domain = domain)
