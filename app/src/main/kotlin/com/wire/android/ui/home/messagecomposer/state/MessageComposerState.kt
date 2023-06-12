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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue

sealed class MessageComposerState {
    data class InActive(val messageComposition: MessageComposition) : MessageComposerState()
    class Active(
        private val focusManager: FocusManager,
        val focusRequester: FocusRequester,
        private val messageCompositionState: MutableState<MessageComposition>,
        defaultAdditionalOptionMenuState: AdditionalOptionMenuState = AdditionalOptionMenuState.AttachmentAndAdditionalOptionsMenu,
        defaultAdditionalOptionsSubMenuState: AdditionalOptionSubMenuState = AdditionalOptionSubMenuState.Hidden,
        private val onShowEphemeralOptionsMenu: () -> Unit
    ) : MessageComposerState() {

        init {
//            focusRequester.requestFocus()
        }

        val messageCompositionInputState = MessageCompositionInputState(messageCompositionState)

        var additionalOptionMenuState: AdditionalOptionMenuState by mutableStateOf(defaultAdditionalOptionMenuState)
            private set

        var additionalOptionsSubMenuState: AdditionalOptionSubMenuState by mutableStateOf(defaultAdditionalOptionsSubMenuState)
            private set

        fun toEphemeralInputType() {
            messageCompositionInputState.toEphemeral()
            onShowEphemeralOptionsMenu()
        }

        fun toggleAttachmentOptions() {
            additionalOptionsSubMenuState =
                if (additionalOptionsSubMenuState == AdditionalOptionSubMenuState.AttachFile) {
                    focusRequester.requestFocus()
                    AdditionalOptionSubMenuState.Hidden
                } else {
                    focusManager.clearFocus()
                    AdditionalOptionSubMenuState.AttachFile
                }
        }

        fun onInputFocused() {
            additionalOptionsSubMenuState = AdditionalOptionSubMenuState.Hidden
        }

        fun toggleGifMenu() {

        }

        fun messageTextChanged(textFieldValue: TextFieldValue) {
            messageCompositionState.value = messageCompositionState.value.copy(textFieldValue = textFieldValue)
        }
    }
}
