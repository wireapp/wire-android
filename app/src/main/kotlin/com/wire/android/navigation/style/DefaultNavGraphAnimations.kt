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

import androidx.compose.animation.ExperimentalAnimationApi
import com.ramcosta.composedestinations.animations.defaults.NestedNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations

@OptIn(ExperimentalAnimationApi::class)
val DefaultRootNavGraphAnimations = RootNavGraphDefaultAnimations(
    enterTransition = { with(DefaultNavigationAnimation) { enterTransition() } },
    exitTransition = { with(DefaultNavigationAnimation) { exitTransition() } },
    popEnterTransition = { with(DefaultNavigationAnimation) { popEnterTransition() } },
    popExitTransition = { with(DefaultNavigationAnimation) { popExitTransition() } },
)

@OptIn(ExperimentalAnimationApi::class)
val DefaultNestedNavGraphAnimations = NestedNavGraphDefaultAnimations(
    enterTransition = { with(DefaultNavigationAnimation) { enterTransition() } },
    exitTransition = { with(DefaultNavigationAnimation) { exitTransition() } },
    popEnterTransition = { with(DefaultNavigationAnimation) { popEnterTransition() } },
    popExitTransition = { with(DefaultNavigationAnimation) { popExitTransition() } },
)
