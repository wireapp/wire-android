package com.wire.android.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Alignment

/**
 * Animation that allows a smooth transition from rtl, adding a fade in effect
 */
@Suppress("MagicNumber")
fun smoothSlideInFromRight(): EnterTransition {
    return slideInHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
        fullWidth / 3
    } + fadeIn(
        animationSpec = tween(durationMillis = 200)
    )
}

/**
 * Animation that allows a smooth transition from ltr, adding a fade out effect
 */
@Suppress("MagicNumber")
fun smoothSlideOutFromLeft(): ExitTransition {
    return slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) {
        +200
    } + fadeOut(
        animationSpec = tween(durationMillis = 200)
    )
}

/**
 * Animation that allows a transition from expand in from bottom, adding a fade in effect
 */
@Suppress("MagicNumber")
fun expandInToView(): EnterTransition {
    return expandVertically(initialHeight = { it * 2 }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(
        animationSpec = tween(
            durationMillis = 400
        )
    )
}

/**
 * Animation that allows a transition from full content to shrink to zero, adding a fade out effect
 */
@Suppress("MagicNumber")
fun shrinkOutFromView(): ExitTransition {
    return shrinkVertically(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        shrinkTowards = Alignment.Top,
        targetHeight = { it / 2 }
    ) + fadeOut(
        animationSpec = tween(durationMillis = 400)
    )
}
