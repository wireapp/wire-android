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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue

sealed class MessageComposerState {

    data class Active(
        var messageComposition: MessageComposition,
        private val generalOptionItem: AdditionalOptionSubMenuState,
        private val messageCompositionInputType: MessageCompositionInputType.Composing,
        private val messageCompositionInputSize: MessageCompositionInputSize,
        private val additionalOptionsState: AdditionalOptionMenuState,
    ) : MessageComposerState() {

        var inputType: MessageCompositionInputType by mutableStateOf(messageCompositionInputType)

        var inputSize: MessageCompositionInputSize by mutableStateOf(messageCompositionInputSize)

        val additionalOptionsMenuState: AdditionalOptionMenuState by mutableStateOf(
            additionalOptionsState
        )

        fun toEphemeralInputType() {
            inputType = MessageCompositionInputType.Ephemeral
        }


//            onShowSelfDeletionOption = onShowSelfDeletionOption,
//            showSelfDeletingOption = showSelfDeletingOption,

        fun messageTextChanged(textFieldValue: TextFieldValue) {
            messageComposition = messageComposition.copy(textFieldValue = textFieldValue)
        }
    }

    data class InActive(val messageComposition: MessageComposition) : MessageComposerState()
}
