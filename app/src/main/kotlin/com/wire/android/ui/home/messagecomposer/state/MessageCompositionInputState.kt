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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WireTextFieldColors
import com.wire.android.ui.common.textfield.wireTextFieldColors

class MessageCompositionInputState(
    val messageCompositionState: MutableState<MessageComposition>,
    defaultInputFocused: Boolean = true,
    defaultInputType: MessageCompositionInputType = MessageCompositionInputType.Composing(messageCompositionState),
    defaultInputSize: MessageCompositionInputSize = MessageCompositionInputSize.EXPANDED
) {
    var inputFocused: Boolean by mutableStateOf(defaultInputFocused)
        private set
    var type: MessageCompositionInputType by mutableStateOf(defaultInputType)
        private set

    var inputSize by mutableStateOf(defaultInputSize)

    fun toEphemeral(onShowEphemeralOptionsMenu: () -> Unit) {
        type = MessageCompositionInputType.SelfDeleting(messageCompositionState, onShowEphemeralOptionsMenu)
    }

    fun toComposing() {
        type = MessageCompositionInputType.Composing(messageCompositionState)
    }

    fun clearFocus() {
        inputFocused = false
    }

    fun inputFocus() {
        inputFocused = true
    }

    fun toFullscreen() {
        inputSize = MessageCompositionInputSize.EXPANDED
    }

    fun toCollapsed() {
        inputSize = MessageCompositionInputSize.COLLAPSED
    }

}

sealed class MessageCompositionInputType(
    val messageCompositionState: MutableState<MessageComposition>
) {
    @Composable
    open fun inputTextColor(): WireTextFieldColors = wireTextFieldColors(
        backgroundColor = Color.Transparent,
        borderColor = Color.Transparent,
        focusColor = Color.Transparent,
        placeholderColor = colorsScheme().secondaryText
    )

    @Composable
    open fun backgroundColor(): Color = colorsScheme().messageComposerBackgroundColor

    class Composing(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType(messageCompositionState) {

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }

    }

    class Editing(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType(messageCompositionState) {

        @Composable
        override fun backgroundColor(): Color = colorsScheme().messageComposerEditBackgroundColor

    }

    class SelfDeleting(
        messageCompositionState: MutableState<MessageComposition>,
        private val onShowEphemeralOptionsMenu: () -> Unit
    ) : MessageCompositionInputType(messageCompositionState) {
        @Composable
        override fun inputTextColor() =
            wireTextFieldColors(
                backgroundColor = Color.Transparent,
                borderColor = Color.Transparent,
                focusColor = Color.Transparent,
                placeholderColor = colorsScheme().primary
            )

        fun showSelfDeletingTimeOption() {
            onShowEphemeralOptionsMenu()
        }
    }
}

enum class MessageCompositionInputSize {
    COLLAPSED,
    EXPANDED;
}
