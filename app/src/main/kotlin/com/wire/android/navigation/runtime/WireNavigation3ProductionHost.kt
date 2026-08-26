/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.navigation.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.wire.android.di.metro.WireViewModelDiagnostics
import com.wire.android.navigation.navigation3.WireEntryEnvironment
import com.wire.android.navigation.navigation3.WireNav3Host
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.navigation3.WireResponsivePresentationPolicy
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import com.wire.android.navigation.routes.auth.AuthenticationNavigation3Router
import com.wire.navigation.stableKey

/**
 * Production composition root for the migrated application navigation surface.
 *
 * Runtime creation stays with WireActivity because startup, session transitions and process-state
 * restoration are Activity policies. Once a runtime exists, this host owns the deterministic
 * contribution catalog and the only Navigation 3 display.
 */
@Composable
internal fun WireNavigation3ProductionHost(
    runtime: WireNavigation3Runtime,
    actions: WireNavigation3CompositeActions,
    authenticationRouter: AuthenticationNavigation3Router,
    entryEnvironment: WireEntryEnvironment,
    onRootBack: () -> Unit,
    sharedViewModelStoreProvider: ViewModelStoreProvider,
    modifier: Modifier = Modifier,
    responsivePresentationPolicy: WireResponsivePresentationPolicy =
        WireNavigation3ResponsivePresentationPolicy,
) {
    val catalog = remember(runtime, actions, authenticationRouter) {
        WireNavigation3Contributions.create(runtime, actions, authenticationRouter)
    }
    WireNav3Host(
        runtime = runtime,
        entryEnvironment = entryEnvironment,
        entryProviderInstallers = catalog.entryProviderInstallers,
        modifier = modifier,
        onBack = {
            navigateBackOrRunRootFallback(runtime.navigator::goBack, onRootBack)
        },
        responsivePresentationPolicy = responsivePresentationPolicy,
        sharedViewModelStoreProvider = sharedViewModelStoreProvider,
        onViewModelOwnerAvailable = { ownerIdentity, owner ->
            WireViewModelDiagnostics.ownerAvailable(owner, ownerIdentity.stableKey())
        },
        onViewModelOwnerReleased = { ownerIdentity, owner ->
            WireViewModelDiagnostics.ownerReleased(owner, ownerIdentity.stableKey())
        },
        onViewModelOwnerCleared = WireViewModelDiagnostics::ownerCleared,
    )
}

internal fun navigateBackOrRunRootFallback(
    navigateBack: () -> Boolean,
    onRootBack: () -> Unit,
) {
    if (!navigateBack()) onRootBack()
}
