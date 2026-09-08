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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.ui.LocalNavAnimatedContentScope

/**
 * Prevents transition-time double taps from issuing a second navigation command.
 *
 * Navigation 3 normally keeps transitioning entries below `RESUMED`. Both signals are required:
 * an entry lifecycle can lag behind a completed transition after session setup, while a nested
 * scene can leave its animation scope running after the visible entry has resumed. Neither stale
 * signal may permanently cover the screen with the input-consuming overlay.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
@PublishedApi
internal fun WireTransitionInteractionGate(
    policy: WireTransitionInteractionPolicy,
    content: @Composable () -> Unit,
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val transitionRunning = LocalNavAnimatedContentScope.current.transition.isRunning

    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (
            policy.shouldBlockInput(
                isTransitionRunning = transitionRunning,
                isEntryResumed = lifecycleState == Lifecycle.State.RESUMED,
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { change -> change.consume() }
                            }
                        }
                    }
            )
        }
    }
}
