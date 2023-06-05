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

class MessageCompositionInputState(private val messageCompositionState: MutableState<MessageComposition>) {

    var inputType: MessageCompositionInputType by mutableStateOf(MessageCompositionInputType.Composing(messageCompositionState))
        private set

    var size: MessageCompositionInputSize by mutableStateOf(MessageCompositionInputSize.COLLAPSED)
        private set

    fun toEphemeral() {
        inputType = MessageCompositionInputType.Ephemeral(messageCompositionState)
    }

    fun toFullscreen() {
        size = MessageCompositionInputSize.EXPANDED
    }

    fun toCollapsed() {
        size = MessageCompositionInputSize.COLLAPSED
    }

}

sealed class MessageCompositionInputType(val messageCompositionState: MutableState<MessageComposition>) {
    class Composing(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType(messageCompositionState) {

        val isSendButtonEnabled by derivedStateOf {
            messageCompositionState.value.messageText.isNotBlank()
        }

    }

    class Editing(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType(messageCompositionState)
    class Ephemeral(messageCompositionState: MutableState<MessageComposition>) : MessageCompositionInputType(messageCompositionState)
}

enum class MessageCompositionInputSize {
    COLLAPSED,
    EXPANDED;
}
