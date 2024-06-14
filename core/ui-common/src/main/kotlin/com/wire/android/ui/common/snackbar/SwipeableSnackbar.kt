/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.common.snackbar

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireDimensions
import kotlin.math.roundToInt

/**
 * A swipeable [Snackbar] that allows users to manually dismiss it by dragging.
 *
 * This composable function extends the default Snackbar behavior by adding a draggable gesture.
 * The Snackbar can be swiped horizontally to dismiss it, based on predefined positional and velocity thresholds.
 *
 * @param hostState The state of the [SnackbarHostState] this Snackbar is associated with. This allows
 * the Snackbar to notify its host when it's dismissed.
 * @param data The [SnackbarData] containing the message and optional action to display on the Snackbar.
 * @param onDismiss An optional callback function to be executed when the Snackbar is swiped away.
 * The default behavior will dismiss the current Snackbar from the [hostState].
 * @see Snackbar
 * @see SnackbarData
 * @see SnackbarHostState
 */

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableSnackbar(
    hostState: SnackbarHostState,
    data: SnackbarData,
    onDismiss: () -> Unit = { hostState.currentSnackbarData?.dismiss() },
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    val currentScreenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

    val anchors = DraggableAnchors {
        SnackBarState.Visible at 0f
        SnackBarState.DismissedLeft at currentScreenWidth
        SnackBarState.DismissedRight at -currentScreenWidth
    }

    // Determines how far the user needs to drag (as a fraction of total distance) for an action to be triggered.
    // In this example, the Snackbar will trigger an action if dragged to half (0.5) of its width.
    val positionalThreshold: (Float) -> Float = { distance -> distance * 0.5f }

    // Determines the minimum velocity (in pixels per second) with which the user needs to drag for an action to be triggered,
    // even if the positional threshold hasn't been reached.
    // Here, it's set to 125 device-independent pixels per second.
    val velocityThreshold: () -> Float = with(density) { { 125.dp.toPx() } }

    val state = remember {
        AnchoredDraggableState(
            initialValue = SnackBarState.Visible,
            anchors = anchors,
            positionalThreshold = positionalThreshold,
            velocityThreshold = velocityThreshold,
            snapAnimationSpec = SpringSpec(),
            decayAnimationSpec = splineBasedDecay(density),
            confirmValueChange = { true }
        )
    }

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SnackBarState.DismissedLeft || state.currentValue == SnackBarState.DismissedRight) {
            onDismiss()
        }
    }

    Snackbar(
        snackbarData = data,
        shape = RoundedCornerShape(MaterialTheme.wireDimensions.buttonSmallCornerSize),
        modifier = Modifier
            .anchoredDraggable(
                state = state,
                orientation = Orientation.Horizontal
            ).offset { IntOffset(state.requireOffset().roundToInt(), 0) }
    )
}

private enum class SnackBarState { Visible, DismissedLeft, DismissedRight }
