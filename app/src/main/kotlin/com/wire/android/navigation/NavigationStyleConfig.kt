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

import androidx.compose.animation.ExperimentalAnimationApi
import com.ramcosta.composedestinations.spec.DestinationStyle

enum class ScreenMode {
    KEEP_ON, // keep screen on while that NavigationItem is visible (i.e CallScreen)
    WAKE_UP, // wake up the device on navigating to that NavigationItem (i.e IncomingCall)
    NONE // do not wake up and allow device to sleep
}

interface ScreenModeStyle {
    val screenMode: ScreenMode
}

// TODO: implement animations, now only styles exist to handle different screen modes

@OptIn(ExperimentalAnimationApi::class)
object WakeUpScreenPopUpNavigationAnimation : DestinationStyle.Animated, ScreenModeStyle {
    override val screenMode: ScreenMode = ScreenMode.WAKE_UP
}

@OptIn(ExperimentalAnimationApi::class)
object KeepOnScreenPopUpNavigationAnimation : DestinationStyle.Animated, ScreenModeStyle {
    override val screenMode: ScreenMode = ScreenMode.KEEP_ON
}

@OptIn(ExperimentalAnimationApi::class)
object PopUpNavigationAnimation : DestinationStyle.Animated, ScreenModeStyle {
    override val screenMode: ScreenMode = ScreenMode.NONE
}

@OptIn(ExperimentalAnimationApi::class)
object SlideNavigationAnimation : DestinationStyle.Animated, ScreenModeStyle {
    override val screenMode: ScreenMode = ScreenMode.NONE
}
