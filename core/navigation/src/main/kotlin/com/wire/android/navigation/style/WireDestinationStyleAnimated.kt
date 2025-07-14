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
package com.wire.android.navigation.style

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.ramcosta.composedestinations.utils.destination

/**
 * Default transition animations style class.
 * Chooses the right transition animation to be used depending on the recently opened destination's style.
 * Thanks to that animations are consistent and not mixed when both destinations involved in the transition use different styles.
 */
internal interface WireDestinationStyleAnimated : DestinationStyle.Animated {
    fun animationType(): TransitionAnimationType = TransitionAnimationType.SLIDE
    private fun DestinationSpec<*>.getAnimationTypeStyle() =
        (this.style as? WireDestinationStyleAnimated)?.animationType() ?: animationType()

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTransition() =
        targetState.destination().getAnimationTypeStyle().enterTransition

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTransition() =
        targetState.destination().getAnimationTypeStyle().exitTransition

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition() =
        initialState.destination().getAnimationTypeStyle().popEnterTransition

    override fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition(): ExitTransition =
        initialState.destination().getAnimationTypeStyle().popExitTransition
}

/**
 * Transition animations according to the Wire design system documentation.
 *
 * navigate A -> B
 * A style	B style		    A exit animation	    B enter animation
 * Slide	Slide		    FadeOut     	        SlideFromRight
 * Slide	PopUp		    FadeOut			        SlideFromBottom
 * PopUp	Slide		    FadeOut     	        SlideFromRight
 * PopUp	PopUp		    FadeOut			        SlideFromBottom
 *
 * navigate back B -> A
 * A style	B style		    B popExit animation     A popEnter animation
 * Slide	Slide		    SlideToRight            FadeIn
 * Slide	PopUp		    SlideToBottom           FadeIn
 * PopUp	Slide		    SlideToRight            FadeIn
 * PopUp	PopUp		    SlideToBottom           FadeIn
 */
enum class TransitionAnimationType(
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
    val popEnterTransition: EnterTransition,
    val popExitTransition: ExitTransition
) {
    SLIDE( // new destination slides from right
        enterTransition = smoothSlideInFromRight(),
        exitTransition = fadeOutFromView(),
        popEnterTransition = fadeInToView(),
        popExitTransition = smoothSlideOutFromLeft()
    ),
    POP_UP( // new destination slides from bottom
        enterTransition = expandInToView(),
        exitTransition = fadeOutFromView(),
        popEnterTransition = fadeInToView(),
        popExitTransition = shrinkOutFromView()
    ),
    NONE(
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None,
        popEnterTransition = EnterTransition.None,
        popExitTransition = ExitTransition.None
    )
}
