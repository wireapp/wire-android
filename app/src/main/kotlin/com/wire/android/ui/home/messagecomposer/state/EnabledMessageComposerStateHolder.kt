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
package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

@Stable
class EnabledMessageComposerStateHolder {

    var keyboardHeight by mutableStateOf(220.dp)
    var optionsHeight by mutableStateOf(0.dp)
    var showOptions by mutableStateOf(false)
    var showSubOptions by mutableStateOf(false)
    var isTextExpanded by mutableStateOf(false)
    var previousOffset by mutableStateOf(0.dp)

    fun handleIMEVisibility(isImeVisible: Boolean) {
        if (isImeVisible) {
            showOptions = true
        } else if (!showSubOptions) {
            showOptions = false
        }
    }

    fun handleOffsetChange(offset: Dp, navBarHeight: Dp) {
        val actualOffset = max(offset - navBarHeight, 0.dp)

        if (previousOffset < actualOffset) {
            if (!showSubOptions || optionsHeight <= actualOffset) {
                optionsHeight = actualOffset
                showSubOptions = false
            }
        } else if (previousOffset > actualOffset) {
            if (!showSubOptions) {
                optionsHeight = actualOffset
                if (actualOffset == 0.dp) {
                    showOptions = false
                    isTextExpanded = false
                }
            }
        }

        previousOffset = actualOffset

        if (keyboardHeight == actualOffset) {
            showSubOptions = false
        }

        if (keyboardHeight < actualOffset) {
            keyboardHeight = actualOffset
        }
    }

    fun calculateOptionsMenuHeight(additionalOptionsSubMenuState: AdditionalOptionSubMenuState): Dp {
        return optionsHeight + if (additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) 0.dp else composeTextHeight
    }

    fun handleBackPressed(isImeVisible: Boolean, additionalOptionsSubMenuState: AdditionalOptionSubMenuState, focusManager: FocusManager) {
        if ((isImeVisible || showOptions) && additionalOptionsSubMenuState != AdditionalOptionSubMenuState.RecordAudio) {
            showOptions = false
            showSubOptions = false
            isTextExpanded = false
            optionsHeight = 0.dp
            focusManager.clearFocus(force = true)
        }
    }

    companion object {

        /**
         * This height was based on the size of Input Text + Additional Options (Text Format, Ping, etc)
         */
        val composeTextHeight = 128.dp

        fun saver(density: Density): Saver<EnabledMessageComposerStateHolder, *> = Saver(
            save = {
                listOf(
                    it.keyboardHeight.value,
                    it.optionsHeight.value,
                    it.showOptions,
                    it.showSubOptions,
                    it.isTextExpanded,
                    it.previousOffset.value
                )
            },
            restore = { savedState ->
                with(density) {
                    EnabledMessageComposerStateHolder().apply {
                        keyboardHeight = (savedState[0] as Float).toDp()
                        optionsHeight = (savedState[1] as Float).toDp()
                        showOptions = savedState[2] as Boolean
                        showSubOptions = savedState[3] as Boolean
                        isTextExpanded = savedState[4] as Boolean
                        previousOffset = (savedState[5] as Float).toDp()
                    }
                }
            }
        )
    }
}
