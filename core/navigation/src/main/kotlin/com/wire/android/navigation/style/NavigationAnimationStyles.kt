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

import androidx.compose.ui.window.DialogProperties
import com.ramcosta.composedestinations.spec.DestinationStyle

typealias DefaultNavigationAnimation = SlideNavigationAnimation

// Changed from object to class because WireDestinationStyleAnimated is now an abstract class
class SlideNavigationAnimation : WireDestinationStyleAnimated(), BackgroundStyle {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.SLIDE
}

class PopUpNavigationAnimation : WireDestinationStyleAnimated(), BackgroundStyle {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.POP_UP
}

class AuthSlideNavigationAnimation : WireDestinationStyleAnimated(), BackgroundStyle {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.SLIDE
    override fun backgroundType(): BackgroundType = BackgroundType.Auth
}

class AuthPopUpNavigationAnimation : WireDestinationStyleAnimated(), BackgroundStyle {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.POP_UP
    override fun backgroundType(): BackgroundType = BackgroundType.Auth
}

class AuthNoNavigationAnimation : WireDestinationStyleAnimated(), BackgroundStyle {
    override fun animationType(): TransitionAnimationType = TransitionAnimationType.NONE
    override fun backgroundType(): BackgroundType = BackgroundType.Auth
}

// Dialog style now extends the abstract class
class DialogNavigation : DestinationStyle.Dialog() {
    override val properties: DialogProperties
        get() = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = false,
        )
}
