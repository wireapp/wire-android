/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

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
