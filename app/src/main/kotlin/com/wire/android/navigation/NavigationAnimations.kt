package com.wire.android.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

class ItemAnimationConfig {
    var enterAnimation = wireSlideInFromRight()
    var exitAnimation = wireSlideOutFromLeft()
    var popEnterAnimation = wireSlideInFromRight()
    var popExitAnimation = wireSlideOutFromLeft()
}

/**
 * Examples of animations on
 * https://developer.android.com/reference/kotlin/androidx/compose/animation/package-summary
 */
fun wireSlideInFromRight(): EnterTransition {
    return slideInHorizontally(animationSpec = tween(durationMillis = 200)) { fullWidth ->
        fullWidth / 3
    } + fadeIn(
        animationSpec = tween(durationMillis = 200)
    )
}

fun wireSlideOutFromLeft(): ExitTransition {
    return slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) {
        +200
    } + fadeOut(
        animationSpec = tween(durationMillis = 200)
    )
}
