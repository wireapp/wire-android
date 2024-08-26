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

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import io.github.esentsov.PackagePrivate

private val fadeAnimationSpec: FiniteAnimationSpec<Float> = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = Spring.DefaultDisplacementThreshold,
)
private val slideAnimationSpec: FiniteAnimationSpec<IntOffset> = spring(
    stiffness = Spring.StiffnessMediumLow,
    visibilityThreshold = IntOffset.Companion.VisibilityThreshold,
)

/**
 * Animation that allows a smooth transition from rtl, adding a fade in effect
 */
@Suppress("MagicNumber")
@PackagePrivate
internal fun smoothSlideInFromRight(): EnterTransition {
    return slideInHorizontally(
        animationSpec = slideAnimationSpec,
        initialOffsetX = { fullWidth -> fullWidth / 3 },
    ) + fadeIn(
        animationSpec = fadeAnimationSpec,
    )
}

/**
 * Animation that allows a smooth transition from ltr, adding a fade out effect
 */
@Suppress("MagicNumber")
@PackagePrivate
internal fun smoothSlideOutFromLeft(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = slideAnimationSpec,
        targetOffsetX = { fullWidth -> fullWidth / 3 },
    ) + fadeOut(
        animationSpec = fadeAnimationSpec,
    )
}

/**
 * Animation that allows a transition from expand in from bottom, adding a fade in effect
 */
@Suppress("MagicNumber")
@PackagePrivate
internal fun expandInToView(): EnterTransition {
    return slideInVertically(
        animationSpec = slideAnimationSpec,
        initialOffsetY = { fullHeight -> fullHeight / 4 },
    ) + fadeIn(
        animationSpec = fadeAnimationSpec,
    )
}

/**
 * Animation that allows a transition from full content to shrink to zero, adding a fade out effect
 */
@Suppress("MagicNumber")
@PackagePrivate
internal fun shrinkOutFromView(): ExitTransition {
    return slideOutVertically(
        animationSpec = slideAnimationSpec,
        targetOffsetY = { fullHeight -> fullHeight / 4 },
    ) + fadeOut(
        animationSpec = fadeAnimationSpec,
    )
}

/**
 * Animation that allows a transition that shows the content using fade in effect
 */
@PackagePrivate
internal fun fadeInToView(): EnterTransition {
    return fadeIn(
        animationSpec = fadeAnimationSpec,
    )
}

/**
 * Animation that allows a transition that hides the content using fade out effect
 */
@PackagePrivate
internal fun fadeOutFromView(): ExitTransition {
    return fadeOut(
        animationSpec = fadeAnimationSpec,
    )
}
