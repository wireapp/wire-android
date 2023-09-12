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
 */

package com.wire.android.navigation.style

typealias DefaultNavigationAnimation = SlideNavigationAnimation

object SlideNavigationAnimation : WireDestinationStyleAnimated {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.SLIDE
}

object PopUpNavigationAnimation : WireDestinationStyleAnimated {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.POP_UP
}

object WakeUpScreenPopUpNavigationAnimation : WireDestinationStyleAnimated, ScreenModeStyle {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.POP_UP
    override fun screenMode(): ScreenMode = ScreenMode.WAKE_UP
}

object KeepOnScreenPopUpNavigationAnimation : WireDestinationStyleAnimated, ScreenModeStyle {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.POP_UP
    override fun screenMode(): ScreenMode = ScreenMode.KEEP_ON
}
