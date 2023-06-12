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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MessageCompositionInputState(
    private val messageCompositionState: MutableState<MessageComposition>,
    defaultInputFocused: Boolean = true,
    defaultInputType: MessageCompositionInputType = MessageCompositionInputType.Composing(messageCompositionState),
    defaultInputSize: MessageCompositionInputSize = MessageCompositionInputSize.COLLAPSED
) {
    var inputFocused: Boolean by mutableStateOf(defaultInputFocused)
        private set
    var type: MessageCompositionInputType by mutableStateOf(defaultInputType)
        private set

    var size: MessageCompositionInputSize by mutableStateOf(defaultInputSize)
        private set

    fun toEphemeral(onShowEphemeralOptionsMenu: () -> Unit) {
        type = MessageCompositionInputType.SelfDeleting(messageCompositionState, onShowEphemeralOptionsMenu)
    }

    fun toFullscreen() {
        size = MessageCompositionInputSize.EXPANDED
    }

    fun toCollapsed() {
        size = MessageCompositionInputSize.COLLAPSED
    }

    fun clearFocus() {
        inputFocused = false
    }

    fun focusInput() {
        inputFocused = true
    }

}

sealed class MessageCompositionInputType(val messageCompositionState: MutableState<MessageComposition>) {
    class Composing(messageCompositionState: MutableState<MessageComposition>) :
        MessageCompositionInputType(messageCompositionState) {

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }

    }

    class Editing(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType(messageCompositionState)
    class SelfDeleting(messageCompositionState: MutableState<MessageComposition>, private val onShowEphemeralOptionsMenu: () -> Unit) :
        MessageCompositionInputType(messageCompositionState) {

        fun showSelfDeletingTimeOption() {
            onShowEphemeralOptionsMenu()
        }
    }
}

enum class MessageCompositionInputSize {
    COLLAPSED,
    EXPANDED;
}
