package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.model.UIMessage
import kotlin.math.absoluteValue
import kotlin.math.min

@Stable
sealed interface SwipableMessageConfiguration {
    data object NotSwipable : SwipableMessageConfiguration
    class SwipableToReply(val onSwipedToReply: (uiMessage: UIMessage.Regular) -> Unit) : SwipableMessageConfiguration
}

enum class SwipeAnchor {
    CENTERED,
    START_TO_END
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SwipableToReplyBox(
    modifier: Modifier = Modifier,
    onSwipedToReply: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    var didVibrateOnCurrentDrag by remember { mutableStateOf(false) }

    // Finish the animation in the first 25% of the drag
    val progressUntilAnimationCompletion = 0.25f
    val dragWidth = screenWidth * progressUntilAnimationCompletion

    val currentViewConfiguration = LocalViewConfiguration.current
    val scopedViewConfiguration = object : ViewConfiguration by currentViewConfiguration {
        // Make it easier to scroll by giving the user a bit more length to identify the gesture as vertical
        override val touchSlop: Float
            get() = currentViewConfiguration.touchSlop * 3f
    }
    CompositionLocalProvider(LocalViewConfiguration provides scopedViewConfiguration) {
        val dragState = remember {
            AnchoredDraggableState(
                initialValue = SwipeAnchor.CENTERED,
                positionalThreshold = { dragWidth },
                velocityThreshold = { screenWidth },
                snapAnimationSpec = tween(),
                decayAnimationSpec = splineBasedDecay(density),
                confirmValueChange = { changedValue ->
                    if (changedValue == SwipeAnchor.START_TO_END) {
                        // Attempt to finish dismiss, notify reply intention
                        onSwipedToReply()
                    }
                    if (changedValue == SwipeAnchor.CENTERED) {
                        // Reset the haptic feedback when drag is stopped
                        didVibrateOnCurrentDrag = false
                    }
                    // Reject state change, only allow returning back to rest position
                    changedValue == SwipeAnchor.CENTERED
                },
                anchors = DraggableAnchors {
                    SwipeAnchor.CENTERED at 0f
                    SwipeAnchor.START_TO_END at screenWidth
                }
            )
        }
        val primaryColor = colorsScheme().primary

        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            // Drag indication
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        // TODO(RTL): Might need adjusting once RTL is supported
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(dragState.requireOffset().absoluteValue, size.height),
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (dragState.offset > 0f) {
                    val dragProgress = dragState.offset / dragWidth
                    val adjustedProgress = min(1f, dragProgress)
                    val progress = FastOutLinearInEasing.transform(adjustedProgress)
                    // Got to the end, user can release to perform action, so we vibrate to show it
                    if (progress == 1f && !didVibrateOnCurrentDrag) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        didVibrateOnCurrentDrag = true
                    }

                    ReplySwipeIcon(dragWidth, density, progress)
                }
            }
            // Message content, which is draggable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .anchoredDraggable(dragState, Orientation.Horizontal, startDragImmediately = false)
                    .offset {
                        val x = dragState
                            .requireOffset()
                            .toInt()
                        IntOffset(x, 0)
                    },
            ) { content() }
        }
    }
}

@Composable
private fun ReplySwipeIcon(dragWidth: Float, density: Density, progress: Float) {
    val midPointBetweenStartAndGestureEnd = dragWidth / 2
    val iconSize = dimensions().fabIconSize
    val targetIconAnchorPosition = midPointBetweenStartAndGestureEnd - with(density) { iconSize.toPx() / 2 }
    val xOffset = with(density) {
        val totalTravelDistance = iconSize.toPx() + targetIconAnchorPosition
        -iconSize.toPx() + (totalTravelDistance * progress)
    }
    Icon(
        painter = painterResource(id = R.drawable.ic_reply),
        contentDescription = "",
        modifier = Modifier
            .size(iconSize)
            .offset { IntOffset(xOffset.toInt(), 0) },
        tint = colorsScheme().onPrimary
    )
}
