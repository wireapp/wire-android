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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

sealed class NavigationAnimationConfig(
    open val enterTransition: EnterTransition?,
    open val exitTransition: ExitTransition?,
    open val popEnterTransition: EnterTransition? = enterTransition,
    open val popExitTransition: ExitTransition? = exitTransition,
) {
    object NoAnimation : NavigationAnimationConfig(
        enterTransition = EnterTransition.None,
        exitTransition = ExitTransition.None
    )

    object DelegatedAnimation : NavigationAnimationConfig(
        enterTransition = null,
        exitTransition = null
    )

    object DefaultAnimation :
        NavigationAnimationConfig(
            enterTransition = slideInHorizontally(),
            exitTransition = slideOutHorizontally()
        )

    class CustomAnimation(
        override val enterTransition: EnterTransition?,
        override val exitTransition: ExitTransition?
    ) : NavigationAnimationConfig(enterTransition = enterTransition, exitTransition = exitTransition)
}
