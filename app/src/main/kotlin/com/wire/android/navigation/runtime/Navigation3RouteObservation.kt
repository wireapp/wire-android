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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.util.CurrentScreenManager
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Navigation 3 replacement for NavController destination listeners.
 *
 * Entry identity is part of each route, so navigating to the same destination with new arguments
 * still emits a screen transition while recomposition of the same entry does not.
 */
@Composable
internal fun ObserveNavigation3Routes(
    runtime: WireNavigation3Runtime,
    currentScreenManager: CurrentScreenManager,
    onRouteChanged: () -> Unit,
) {
    LaunchedEffect(runtime, currentScreenManager) {
        snapshotFlow { runtime.navigator.currentRoute }
            .distinctUntilChanged()
            .collect { route ->
                route ?: return@collect
                onRouteChanged()
                currentScreenManager.onRouteChanged(route)
            }
    }
}
