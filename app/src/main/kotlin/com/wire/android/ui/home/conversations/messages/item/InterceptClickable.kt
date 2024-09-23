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
package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.foundation.Indication
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.interceptCombinedClickable(
    interactionSource: MutableInteractionSource,
    indication: Indication?,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
) = this
    .indication(interactionSource, indication)
    .pointerInput(Unit) {
        awaitEachGesture {
            val pass = PointerEventPass.Initial
            val longPressTimeout = viewConfiguration.longPressTimeoutMillis

            // wait for the first down press
            val downEvent = awaitFirstDown(pass = pass)
            val press = PressInteraction.Press(downEvent.position)

            if (onClick != null) {
                // if click needs to be intercepted then consume the down event
                downEvent.consume()
                // also animate press
                interactionSource.tryEmit(press)
            }
            try {
                // listen to if there is cancel or up gesture within the longPressTimeout limit
                withTimeout(longPressTimeout) {
                    // wait for up or cancel gesture
                    waitForUpOrCancellation(pass = pass)
                }.let { upEvent ->
                    // up or cancel event happened within the longPressTimeout limit
                    if (onClick != null && upEvent != null) {
                        // consume the up gesture
                        upEvent.consume()
                        // handle click
                        onClick()
                    }
                }
            } catch (_: PointerEventTimeoutCancellationException) {
                // up or cancel event did not happen within the longPressTimeout limit
                if (onLongPress != null) {
                    // animate long press
                    interactionSource.tryEmit(press)
                    // handle long press
                    onLongPress()
                    // wait and consume the up or cancel gesture
                    waitForUpOrCancellation(pass = pass)?.consume()
                } else if (onClick != null) {
                    // animate click
                    interactionSource.tryEmit(press)
                    // wait for the up or cancel gesture
                    waitForUpOrCancellation(pass = pass)?.let { upEvent ->
                        // consume the up gesture
                        upEvent.consume()
                        // handle click
                        onClick()
                    }
                }
            } finally {
                // animate release
                interactionSource.tryEmit(PressInteraction.Release(press))
            }
        }
    }
