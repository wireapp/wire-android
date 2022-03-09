package com.wire.android.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

sealed class NavigationAnimationConfig(
    open val enterTransition: EnterTransition,
    open val exitTransition: ExitTransition,
    open val popEnterTransition: EnterTransition = enterTransition,
    open val popExitTransition: ExitTransition = exitTransition,
) {
    object NoAnimation : NavigationAnimationConfig(
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None
    )

    object DefaultAnimation :
        NavigationAnimationConfig(
            enterTransition = slideInHorizontally(),
            exitTransition = slideOutHorizontally()
        )

    class CustomAnimation(
        override val enterTransition: EnterTransition,
        override val exitTransition: ExitTransition
    ) : NavigationAnimationConfig(enterTransition = enterTransition, exitTransition = exitTransition)
}
