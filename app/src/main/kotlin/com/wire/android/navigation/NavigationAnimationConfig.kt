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

sealed class NavigationAnimationConfig(
    open val enterAnimation: EnterTransition,
    open val exitAnimation: ExitTransition,
    open val popEnterAnimation: EnterTransition = enterAnimation,
    open val popExitAnimation: ExitTransition = exitAnimation,
) {
    object NoAnimationConfig : NavigationAnimationConfig(
        enterAnimation = EnterTransition.None,
        exitAnimation = ExitTransition.None
    )

    object DefaultAnimationConfig :
        NavigationAnimationConfig(
            enterAnimation = slideInHorizontally(),
            exitAnimation = slideOutHorizontally()
        )

    class CustomAnimationConfig(
        override val enterAnimation: EnterTransition,
        override val exitAnimation: ExitTransition
    ) : NavigationAnimationConfig(enterAnimation = enterAnimation, exitAnimation = exitAnimation)
}

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
