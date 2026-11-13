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
package com.wire.android.ui.home.conversations.messagelist

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.util.lerp
import com.wire.android.ui.common.animation.rememberAnimationsEnabled
import com.wire.android.ui.home.conversations.messages.ReplyHighlightRequest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Highlight of the original message after navigating from a reply.
 * Intensity goes from 0 (nothing drawn) to 1 (peak of the pulse); each message scales its highlight colour alpha by it.
 */
@Stable
class ReplyHighlightState {
    var messageId: String? by mutableStateOf(null)
        private set
    private var intensity by mutableFloatStateOf(0f)

    /** Meant to be read in the draw phase, so that the animation only redraws the highlighted message. */
    fun intensityFor(messageId: String): Float = if (this.messageId == messageId) intensity else 0f

    internal fun show(messageId: String, intensity: Float) {
        this.messageId = messageId
        this.intensity = intensity
    }

    internal fun reset() {
        messageId = null
        intensity = 0f
    }
}

/**
 * Drives [ReplyHighlightState] for the given [request]: waits until the target message is visible and the list has settled,
 * then pulses, holds and fades out. Calls [onFinished] with the request token when the highlight ends, is cancelled by
 * user scrolling, or the target message never shows up. A newer request or leaving the composition cancels it silently.
 */
@Composable
fun rememberReplyHighlightState(
    request: ReplyHighlightRequest?,
    lazyListState: LazyListState,
    onFinished: (token: Long) -> Unit,
    animationsEnabled: Boolean = rememberAnimationsEnabled(),
): ReplyHighlightState {
    val state = remember { ReplyHighlightState() }
    val currentOnFinished by rememberUpdatedState(onFinished)
    // the list state is recreated for every reply jump, so the effect has to follow it
    LaunchedEffect(request, lazyListState, animationsEnabled) {
        state.reset()
        if (request == null) return@LaunchedEffect
        try {
            highlightUntilDoneOrUserScroll(request, lazyListState, animationsEnabled, state)
        } finally {
            state.reset()
        }
        currentOnFinished(request.token)
    }
    return state
}

private suspend fun highlightUntilDoneOrUserScroll(
    request: ReplyHighlightRequest,
    lazyListState: LazyListState,
    animationsEnabled: Boolean,
    state: ReplyHighlightState,
) = coroutineScope {
    var arrived by mutableStateOf(false)
    val highlightJob = launch {
        withTimeoutOrNull(ARRIVAL_TIMEOUT_MS) {
            snapshotFlow {
                !lazyListState.isScrollInProgress &&
                        lazyListState.layoutInfo.visibleItemsInfo.any { it.key == request.messageId }
            }.first { it }
        } ?: return@launch
        withFrameMillis { } // let the positioned layout land on screen before highlighting
        arrived = true
        if (animationsEnabled) {
            val startMillis = withFrameMillis { it }
            do {
                val elapsedMillis = withFrameMillis { it } - startMillis
                state.show(request.messageId, replyHighlightIntensity(elapsedMillis, animationsEnabled = true))
            } while (elapsedMillis < REPLY_HIGHLIGHT_TOTAL_MS)
        } else {
            state.show(request.messageId, replyHighlightIntensity(0, animationsEnabled = false))
            delay(REPLY_HIGHLIGHT_HOLD_UNTIL_MS)
        }
    }
    val userScrollJob = launch {
        merge(
            // before arrival programmatic positioning may still be scrolling, so only a drag counts as user scroll
            lazyListState.interactionSource.interactions.filter { it is DragInteraction.Start }.map { },
            // after arrival any scroll is the user's (drag, mouse wheel, keyboard), autoscroll is off while a request exists
            snapshotFlow { arrived && lazyListState.isScrollInProgress }.filter { it }.map { },
        ).first()
        highlightJob.cancel()
    }
    highlightJob.join()
    userScrollJob.cancel()
}

/**
 * Highlight intensity [elapsedMillis] after the target message arrived on screen:
 * one [REPLY_HIGHLIGHT_PULSE_MS] pulse up to the peak and back to [REPLY_HIGHLIGHT_HOLD_INTENSITY], hold until
 * [REPLY_HIGHLIGHT_HOLD_UNTIL_MS], then fade out over [REPLY_HIGHLIGHT_FADE_MS].
 * Without animations it is a static tint until [REPLY_HIGHLIGHT_HOLD_UNTIL_MS].
 */
internal fun replyHighlightIntensity(elapsedMillis: Long, animationsEnabled: Boolean): Float {
    val peakMillis = REPLY_HIGHLIGHT_PULSE_MS / 2
    return when {
        elapsedMillis < 0 -> 0f
        !animationsEnabled -> if (elapsedMillis < REPLY_HIGHLIGHT_HOLD_UNTIL_MS) REPLY_HIGHLIGHT_HOLD_INTENSITY else 0f
        elapsedMillis < peakMillis -> FastOutSlowInEasing.transform(elapsedMillis.toFloat() / peakMillis)

        elapsedMillis < REPLY_HIGHLIGHT_PULSE_MS -> lerp(
            start = 1f,
            stop = REPLY_HIGHLIGHT_HOLD_INTENSITY,
            fraction = FastOutSlowInEasing.transform((elapsedMillis - peakMillis).toFloat() / (REPLY_HIGHLIGHT_PULSE_MS - peakMillis))
        )

        elapsedMillis < REPLY_HIGHLIGHT_HOLD_UNTIL_MS -> REPLY_HIGHLIGHT_HOLD_INTENSITY

        elapsedMillis < REPLY_HIGHLIGHT_TOTAL_MS -> lerp(
            start = REPLY_HIGHLIGHT_HOLD_INTENSITY,
            stop = 0f,
            fraction = FastOutLinearInEasing.transform((elapsedMillis - REPLY_HIGHLIGHT_HOLD_UNTIL_MS).toFloat() / REPLY_HIGHLIGHT_FADE_MS)
        )

        else -> 0f
    }
}

internal const val REPLY_HIGHLIGHT_PULSE_MS = 650L
internal const val REPLY_HIGHLIGHT_HOLD_UNTIL_MS = 2500L
internal const val REPLY_HIGHLIGHT_FADE_MS = 250L
internal const val REPLY_HIGHLIGHT_TOTAL_MS = REPLY_HIGHLIGHT_HOLD_UNTIL_MS + REPLY_HIGHLIGHT_FADE_MS
internal const val REPLY_HIGHLIGHT_HOLD_INTENSITY = 0.6f
private const val ARRIVAL_TIMEOUT_MS = 3000L
