/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

// Custom implementation of animation defaults for v2
// Since RootNavGraphDefaultAnimations is an enum in v2, we'll use a custom object
object WireAnimationDefaults {
    // Fading animations similar to ACCOMPANIST_FADING
    val fadeInTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(700))
    }

    val fadeOutTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(700))
    }
}

// Custom NestedNavGraphDefaultAnimations for Wire
data class WireNestedNavGraphAnimations(
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = { null },
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = { null },
    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition? = { null },
    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition? = { null }
)