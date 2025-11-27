package com.wire.android.ui.home.conversations.messages.item

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
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
import kotlin.math.absoluteValue
import kotlin.math.min

@Stable
sealed interface SwipeableMessageConfiguration {
    data object NotSwipeable : SwipeableMessageConfiguration
    class Swipeable(
        val onSwipedRight: (() -> Unit)? = null,
        val onSwipedLeft: (() -> Unit)? = null,
    ) : SwipeableMessageConfiguration
}

enum class SwipeAnchor {
    CENTERED,
    START_TO_END,
    END_TO_START,
}

data class SwipeAction(
    val icon: Int,
    val action: () -> Unit,
)

@Composable
internal fun SwipeableMessageBox(
    configuration: SwipeableMessageConfiguration,
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwipeableBox(
        messageStyle = messageStyle,
        modifier = modifier,
        onSwipeRight = (configuration as? SwipeableMessageConfiguration.Swipeable)?.onSwipedRight?.let {
            SwipeAction(
                icon = R.drawable.ic_reply,
                action = it,
            )
        },
        onSwipeLeft = (configuration as? SwipeableMessageConfiguration.Swipeable)?.onSwipedLeft?.let {
            SwipeAction(
                icon = R.drawable.ic_react,
                action = it,
            )
        },
        content = content,
    )
}

@Suppress("CyclomaticComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableBox(
    messageStyle: MessageStyle,
    modifier: Modifier = Modifier,
    onSwipeRight: SwipeAction? = null,
    onSwipeLeft: SwipeAction? = null,
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

    val backgroundColor: Color = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().bubblesBackground
        MessageStyle.BUBBLE_OTHER -> colorsScheme().bubblesBackground
        MessageStyle.NORMAL -> colorsScheme().primary
    }

    val tintColor = when (messageStyle) {
        MessageStyle.BUBBLE_SELF -> colorsScheme().primary
        MessageStyle.BUBBLE_OTHER -> colorsScheme().primary
        MessageStyle.NORMAL -> colorsScheme().onPrimary
    }

    CompositionLocalProvider(LocalViewConfiguration provides scopedViewConfiguration) {
        val dragState = remember(onSwipeLeft, onSwipeRight) {
            AnchoredDraggableState(
                initialValue = SwipeAnchor.CENTERED,
                positionalThreshold = { dragWidth },
                velocityThreshold = { screenWidth },
                snapAnimationSpec = tween(),
                decayAnimationSpec = splineBasedDecay(density),
                anchors = DraggableAnchors {

                    SwipeAnchor.CENTERED at 0f

                    if (onSwipeRight != null) {
                        SwipeAnchor.START_TO_END at dragWidth
                    }

                    if (onSwipeLeft != null) {
                        SwipeAnchor.END_TO_START at -dragWidth
                    }
                }
            )
        }

        LaunchedEffect(dragState.settledValue) {
            when (dragState.settledValue) {
                SwipeAnchor.START_TO_END -> {
                    onSwipeRight?.action?.invoke()
                    dragState.animateTo(SwipeAnchor.CENTERED)
                }

                SwipeAnchor.END_TO_START -> {
                    onSwipeLeft?.action?.invoke()
                    dragState.animateTo(SwipeAnchor.CENTERED)
                }

                SwipeAnchor.CENTERED -> {}
            }
            didVibrateOnCurrentDrag = false
        }

        Box(
            modifier = modifier.fillMaxSize(),
        ) {

            val dragOffset = dragState.requireOffset()

            // Drag indication
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        drawRect(
                            color = backgroundColor,
                            topLeft = if (dragOffset >= 0f) {
                                Offset(0f, 0f)
                            } else {
                                Offset(size.width - dragOffset.absoluteValue, 0f)
                            },
                            size = Size(dragOffset.absoluteValue, size.height),
                        )
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {

                val dragProgress = dragState.offset.absoluteValue / dragWidth
                val adjustedProgress = min(1f, dragProgress)
                val progress = FastOutLinearInEasing.transform(adjustedProgress)

                // Got to the end, user can release to perform action, so we vibrate to show it
                if (progress == 1f && !didVibrateOnCurrentDrag) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    didVibrateOnCurrentDrag = true
                }

                if (dragState.offset > 0f) {
                    onSwipeRight?.let { action ->
                        SwipeActionIcon(action.icon, screenWidth, dragWidth, density, progress, tintColor)
                    }
                } else if (dragState.offset < 0f) {
                    onSwipeLeft?.let {
                        SwipeActionIcon(it.icon, screenWidth, dragWidth, density, progress, tintColor, false)
                    }
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
private fun SwipeActionIcon(
    resourceId: Int,
    screenWidth: Float,
    dragWidth: Float,
    density: Density,
    progress: Float,
    tint: Color,
    swipeRight: Boolean = true
) {
    val midPointBetweenStartAndGestureEnd = dragWidth / 2
    val iconSize = dimensions().fabIconSize
    val targetIconAnchorPosition = midPointBetweenStartAndGestureEnd - with(density) { iconSize.toPx() / 2 }
    val xOffset = with(density) {
        val totalTravelDistance = iconSize.toPx() + targetIconAnchorPosition
        if (swipeRight) {
            (totalTravelDistance * progress) - iconSize.toPx()
        } else {
            (totalTravelDistance * progress) - iconSize.toPx() / 2
        }
    }
    Icon(
        painter = painterResource(id = resourceId),
        contentDescription = "",
        modifier = Modifier
            .size(iconSize)
            .offset {
                if (swipeRight) {
                    IntOffset(xOffset.toInt(), 0)
                } else {
                    IntOffset(screenWidth.toInt() - xOffset.toInt(), 0)
                }
            },
        tint = tint
    )
}
