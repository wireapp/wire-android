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

package com.wire.android.navigation.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import com.wire.navigation.WireRoute
import com.wire.navigation.availableSharedViewModelOwners
import com.wire.navigation.entryViewModelOwner

/**
 * A feature-owned contribution to the application's Navigation 3 entry provider.
 */
typealias WireEntryProviderInstaller = EntryProviderScope<NavKey>.() -> Unit

/**
 * Supplies route-specific runtime dependencies without coupling feature entry providers to the
 * application module. The app implementation selects the Metro graph from the typed [route].
 */
fun interface WireEntryEnvironment {
    @Composable
    fun Provide(
        route: WireRoute,
        content: @Composable () -> Unit,
    )
}

private val PassThroughWireEntryEnvironment = WireEntryEnvironment { _, content -> content() }

val LocalWireEntryEnvironment = staticCompositionLocalOf {
    PassThroughWireEntryEnvironment
}

/**
 * Registers a Wire route and always applies its route-specific environment.
 *
 * Feature modules should use this instead of Navigation 3's raw `entry` function.
 */
inline fun <reified Route : WireRoute> EntryProviderScope<NavKey>.wireEntry(
    presentation: WireEntryPresentation = WireEntryPresentation.Default,
    metadata: Map<String, Any> = emptyMap(),
    noinline content: @Composable (Route) -> Unit,
) {
    entry<Route>(
        clazzContentKey = { route -> route.entryId.value },
        metadata = { route ->
            metadata +
                presentation.navEntryMetadata +
                WireViewModelStoreNavEntryDecorator.owners(
                    entryOwner = route.entryViewModelOwner(),
                    sharedOwners = route.availableSharedViewModelOwners(),
                )
        }
    ) { route ->
        LocalWireEntryEnvironment.current.Provide(route) {
            WireTransitionInteractionGate(presentation.interactionPolicy) {
                content(route)
            }
        }
    }
}

internal fun unknownWireEntry(key: NavKey): NavEntry<NavKey> =
    error("No Navigation 3 entry registered for ${key::class.qualifiedName}")
