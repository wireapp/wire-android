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
import androidx.compose.runtime.remember
import com.wire.android.di.metro.AppSessionViewModelGraph
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.model.LocalWireSessionImageLoader
import com.wire.android.ui.authentication.LocalAuthenticationCancelUserId
import com.wire.android.ui.userprofile.self.LocalSelfUserProfileLogoutAction
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireRoute
import com.wire.navigation.WireSessionId
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory

internal data class WireActivityGraphContext(
    val graph: MetroViewModelGraph,
    val viewModelFactory: MetroViewModelFactory,
    val sessionId: WireSessionId?,
    val sessionGraph: AppSessionViewModelGraph?,
)

/**
 * Resolves Activity-owned chrome and dialog ViewModels from the same typed route environment as
 * its Navigation 3 entry. This is the production replacement for generated-route classification
 * and argument Bundle inspection.
 */
@Composable
internal fun rememberWireNavigation3ActivityGraphContext(
    route: WireRoute?,
    graphResolver: MetroWireEntryGraphResolver,
    isUserUiBlocked: Boolean,
): WireActivityGraphContext? {
    if (route == null || isUserUiBlocked) return null

    return remember(route, graphResolver) {
        when (val resolution = graphResolver.resolve(route)) {
            is MetroWireEntryGraphResolution.Available -> {
                val entryGraph = resolution.entryGraph
                WireActivityGraphContext(
                    graph = entryGraph.graph,
                    viewModelFactory = entryGraph.viewModelFactory,
                    sessionId = (route as? SessionRoute)?.sessionId,
                    sessionGraph = entryGraph.sessionGraph,
                )
            }

            is MetroWireEntryGraphResolution.Unavailable -> null
        }
    }
}

@Composable
internal fun WireActivityGraphContext.ProvideViewModelGraph(
    logoutAction: (wipeData: Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMetroViewModelFactory provides viewModelFactory,
        LocalAuthenticationCancelUserId provides sessionId?.toKaliumUserId(),
        LocalWireSessionImageLoader provides sessionGraph?.wireSessionImageLoader,
        LocalSelfUserProfileLogoutAction provides logoutAction,
        content = content,
    )
}
