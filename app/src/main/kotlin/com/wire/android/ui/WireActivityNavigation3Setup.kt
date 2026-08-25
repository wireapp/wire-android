/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.wire.android.feature.SwitchAccountActions
import com.wire.android.navigation.navigation3.WireNavigation3Runtime
import com.wire.android.navigation.runtime.ObserveNavigation3Routes
import com.wire.android.navigation.runtime.WireActivityIntentCoordinator
import com.wire.android.navigation.runtime.WireActivityIntentRequest
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.SwitchAccountObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Owns the Activity-level observers that depend on the typed Navigation 3 runtime.
 *
 * Keeping these effects together gives [WireActivity] one setup call and prevents the Activity
 * from depending on a Nav2 controller or generated destination types.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun WireActivityNavigation3Setup(
    runtime: WireNavigation3Runtime,
    intentCoordinator: WireActivityIntentCoordinator,
    currentScreenManager: CurrentScreenManager,
    switchAccountObserver: SwitchAccountObserver,
    switchAccountActions: SwitchAccountActions,
    shakeEvents: Flow<Unit>,
    onIntentRequest: suspend (WireActivityIntentRequest) -> Unit,
    onShake: suspend () -> Unit,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val currentKeyboardController by rememberUpdatedState(keyboardController)
    val currentIntentHandler by rememberUpdatedState(onIntentRequest)
    val currentShakeHandler by rememberUpdatedState(onShake)

    ObserveNavigation3Routes(
        runtime = runtime,
        currentScreenManager = currentScreenManager,
        onRouteChanged = { currentKeyboardController?.hide() },
    )

    LaunchedEffect(intentCoordinator, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            intentCoordinator.requests.collectLatest { request ->
                currentKeyboardController?.hide()
                currentIntentHandler(request)
            }
        }
    }

    DisposableEffect(switchAccountObserver, switchAccountActions) {
        val unregister = registerWireActivityNavigation3SwitchAccountActions(
            observer = switchAccountObserver,
            actions = switchAccountActions,
        )
        onDispose(unregister)
    }

    LaunchedEffect(shakeEvents, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            shakeEvents.collectLatest {
                currentShakeHandler()
            }
        }
    }
}

internal fun registerWireActivityNavigation3SwitchAccountActions(
    observer: SwitchAccountObserver,
    actions: SwitchAccountActions,
): () -> Unit {
    observer.register(actions)
    return { observer.unregister(actions) }
}
