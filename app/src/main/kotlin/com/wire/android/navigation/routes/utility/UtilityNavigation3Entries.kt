/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.routes.utility

import com.wire.android.navigation.navigation3.WireEntryPresentation
import com.wire.android.navigation.navigation3.WireEntryProviderInstaller
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.wireEntry
import com.wire.android.navigation.routes.auth.InitialSyncRoute
import com.wire.android.navigation.routes.media.AuthenticatedImportMediaRoute
import com.wire.android.ui.debug.DebugRouteScreen
import com.wire.android.ui.debug.LogManagementRouteScreen
import com.wire.android.ui.debug.cryptostats.ConversationCryptoStatsRouteScreen
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsRouteScreen
import com.wire.android.ui.debug.securityproviders.SecurityProvidersRouteScreen
import com.wire.android.ui.initialsync.InitialSyncRouteScreen
import com.wire.navigation.WireNavigationCommand

internal fun interface InitialSyncNavigation3Actions {
    fun completeInitialSync(route: InitialSyncRoute, shouldMoveToBackground: Boolean)
}

internal object UtilityNavigation3Contribution {
    val resultTypes = emptyList<com.wire.android.navigation.navigation3.WireNavigation3ResultType<*>>()

    fun entryProviderInstallers(
        runtime: WireNavigation3Runtime,
        actions: InitialSyncNavigation3Actions,
    ): List<WireEntryProviderInstaller> =
        listOf(utilityNavigation3Entries(runtime, actions))
}

internal fun utilityNavigation3Entries(
    runtime: WireNavigation3Runtime,
    initialSyncActions: InitialSyncNavigation3Actions,
): WireEntryProviderInstaller = {
    wireEntry<InitialSyncRoute>(presentation = WireEntryPresentation.None) { route ->
        InitialSyncRouteScreen { shouldMoveToBackground ->
            initialSyncActions.completeInitialSync(route, shouldMoveToBackground)
        }
    }
    wireEntry<DebugRoute> { route ->
        DebugRouteScreen(
            onBack = runtime.navigator::goBack,
            onShowFeatureFlags = {
                runtime.navigator.navigate(
                    WireNavigationCommand(DebugFeatureFlagsRoute(route.sessionId))
                )
            },
            onShowCryptoStats = {
                runtime.navigator.navigate(
                    WireNavigationCommand(ConversationCryptoStatsRoute(route.sessionId))
                )
            },
            onShowSecurityProviders = {
                runtime.navigator.navigate(
                    WireNavigationCommand(SecurityProvidersRoute(route.sessionId))
                )
            },
            onShareLogsViaWire = { uri ->
                runtime.navigator.navigate(
                    WireNavigationCommand(
                        AuthenticatedImportMediaRoute(route.sessionId, listOf(uri.toString()))
                    )
                )
            },
        )
    }
    wireEntry<LogManagementRoute> { route ->
        LogManagementRouteScreen(
            onBack = runtime.navigator::goBack,
            onShareLogsViaWire = { uri ->
                runtime.navigator.navigate(
                    WireNavigationCommand(
                        AuthenticatedImportMediaRoute(route.sessionId, listOf(uri.toString()))
                    )
                )
            },
        )
    }
    wireEntry<DebugFeatureFlagsRoute> {
        DebugFeatureFlagsRouteScreen(onBack = runtime.navigator::goBack)
    }
    wireEntry<ConversationCryptoStatsRoute> {
        ConversationCryptoStatsRouteScreen(onBack = runtime.navigator::goBack)
    }
    wireEntry<SecurityProvidersRoute> {
        SecurityProvidersRouteScreen(onBack = runtime.navigator::goBack)
    }
}
