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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.wire.android.navigation.transition.LocalSharedTransitionScope
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.isTablet as isTabletLayout
import com.wire.navigation.WireRoute
import com.wire.navigation.WireViewModelOwner

/**
 * Production Navigation 3 host used after the root-host switch.
 *
 * Entry lifecycle, saveable state and ViewModel stores are owned by Navigation 3 entries. The
 * parent owner propagates its default factory and CreationExtras to those entries, which is
 * required for Metro factories and SavedStateHandle creation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WireNav3Host(
    runtime: WireNavigation3Runtime,
    entryEnvironment: WireEntryEnvironment,
    entryProviderInstallers: List<WireEntryProviderInstaller>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { runtime.navigator.goBack() },
    responsivePresentationPolicy: WireResponsivePresentationPolicy =
        WireResponsivePresentationPolicy.None,
    isTablet: Boolean = isTabletLayout,
    parentViewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No parent ViewModelStoreOwner was provided to WireNav3Host"
    },
    sharedViewModelStoreProvider: ViewModelStoreProvider =
        rememberWireViewModelStoreProvider(parentViewModelStoreOwner),
    onViewModelOwnerAvailable: (WireViewModelOwner, ViewModelStoreOwner) -> Unit = { _, _ -> },
    onViewModelOwnerReleased: (WireViewModelOwner, ViewModelStoreOwner) -> Unit = { _, _ -> },
    onViewModelOwnerCleared: (String) -> Unit = {},
) {
    val registeredEntries = entryProvider<NavKey>(
        fallback = ::unknownWireEntry,
    ) {
        entryProviderInstallers.forEach { install -> install() }
    }
    val entries = responsiveEntryProvider(
        delegate = registeredEntries,
        policy = responsivePresentationPolicy,
        isTablet = isTablet,
    )

    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this,
            LocalWireEntryEnvironment provides entryEnvironment,
        ) {
            NavDisplay(
                backStack = runtime.backStack,
                onBack = onBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(parentViewModelStoreOwner),
                    rememberWireViewModelStoreNavEntryDecorator(
                        storeProvider = sharedViewModelStoreProvider,
                        onOwnerAvailable = onViewModelOwnerAvailable,
                        onOwnerReleased = onViewModelOwnerReleased,
                        onOwnerCleared = onViewModelOwnerCleared,
                    ),
                ),
                sceneStrategies = listOf(DialogSceneStrategy()),
                transitionSpec = wireDefaultTransitionSpec(),
                popTransitionSpec = wireDefaultPopTransitionSpec(),
                predictivePopTransitionSpec = wireDefaultPredictivePopTransitionSpec(),
                entryProvider = entries,
            )
        }
    }
}

/**
 * Adds responsive scene metadata after route registration.
 *
 * Responsive metadata is placed before entry-owned metadata so an explicit dialog presentation
 * always wins, including its custom [androidx.compose.ui.window.DialogProperties]. Returning the
 * original entry for phones and unmatched routes keeps their identity and metadata untouched.
 */
internal fun responsiveEntryProvider(
    delegate: (NavKey) -> NavEntry<NavKey>,
    policy: WireResponsivePresentationPolicy,
    isTablet: Boolean,
): (NavKey) -> NavEntry<NavKey> = { key ->
    val entry = delegate(key)
    val route = key as? WireRoute
    if (route == null || !policy.shouldPresentAsTabletDialog(route, isTablet)) {
        entry
    } else {
        val responsiveMetadata = policy
            .resolve(route = route, isTablet = true)
            .navEntryMetadata
        NavEntry(
            key = key,
            contentKey = entry.contentKey,
            metadata = responsiveMetadata + entry.metadata,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(dimensions().spacing20x))
                    .imePadding()
            ) {
                entry.Content()
            }
        }
    }
}
