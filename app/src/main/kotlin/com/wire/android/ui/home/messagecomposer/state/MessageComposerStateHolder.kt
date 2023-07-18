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

package com.wire.android.ui.home.messagecomposer.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.ui.common.KeyboardHelper
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.kalium.logic.data.message.mention.MessageMention
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import com.wire.kalium.logic.util.isPositiveNotNull

@Suppress("LongParameterList")
@Composable
fun rememberMessageComposerStateHolder(
    messageComposerViewState: MutableState<MessageComposerViewState>,
    modalBottomSheetState: WireModalSheetState
): MessageComposerStateHolder {
    // we "extract" the initialization outside of MessageComposition to be able to
    // invoke rememberSaveable with the  built-in TextFieldValue.Saver, this is needed
    // for rebuilding the UI on Activity recreation.
    val messageText = rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    val messageCompositionHolder = MessageCompositionHolder(
        context = LocalContext.current,
        messageText = messageText
    )

    val messageType = remember {
        derivedStateOf {
            val selfDeletionTimer = messageComposerViewState.value.selfDeletionTimer

            if (selfDeletionTimer.duration.isPositiveNotNull()) {
                MessageType.SelfDeleting(selfDeletionTimer)
            } else {
                MessageType.Normal
            }
        }
    }

    return rememberSaveable(
        saver = MessageComposerStateHolder.saver(
            context = LocalContext.current,
            modalBottomSheetState = modalBottomSheetState,
            messageComposerViewState = messageComposerViewState,
            messageType = messageType,
            messageTextFieldValue = messageText
        )
    ) {
        val messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
            messageTextFieldValue = messageText,
            messageType = messageType
        )

        MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            modalBottomSheetState = modalBottomSheetState,
            messageCompositionInputStateHolder = messageCompositionInputStateHolder,
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = AdditionalOptionStateHolder()
        )
    }
}

/**
 * Class holding the whole UI state for the MessageComposer, this is the class that is used by the out-side world to give the control
 * of the state to the parent Composables
 */
class MessageComposerStateHolder(
    val modalBottomSheetState: WireModalSheetState,
    val messageComposerViewState: MutableState<MessageComposerViewState>,
    val messageCompositionInputStateHolder: MessageCompositionInputStateHolder,
    val messageCompositionHolder: MessageCompositionHolder,
    val additionalOptionStateHolder: AdditionalOptionStateHolder
) {
    companion object {
        @Suppress("MagicNumber")
        fun saver(
            context: Context,
            modalBottomSheetState: WireModalSheetState,
            messageType: State<MessageType>,
            messageComposerViewState: MutableState<MessageComposerViewState>,
            messageTextFieldValue: MutableState<TextFieldValue>
        ): Saver<MessageComposerStateHolder, *> = Saver(
            save = {
                listOf(
                    it.additionalOptionStateHolder.selectedOption,
                    it.additionalOptionStateHolder.additionalOptionsSubMenuState,
                    it.additionalOptionStateHolder.additionalOptionState,
                    it.messageCompositionInputStateHolder.inputSize,
                    it.messageCompositionInputStateHolder.inputVisibility,
                    it.messageCompositionInputStateHolder.inputFocused,
                    it.messageCompositionInputStateHolder.inputState
                )
            },
            restore = {
                MessageComposerStateHolder(
                    messageComposerViewState = messageComposerViewState,
                    messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
                        messageTextFieldValue = messageTextFieldValue,
                        messageType = messageType,
                        inputSize = it[3] as MessageCompositionInputSize,
                        inputVisibility = it[4] as Boolean,
                        inputFocused = it[5] as Boolean,
                        inputState = it[6] as MessageCompositionInputState
                    ),
                    messageCompositionHolder = MessageCompositionHolder(
                        context = context
                    ),
                    additionalOptionStateHolder = AdditionalOptionStateHolder(
                        ininitialSelectedOption = it[0] as AdditionalOptionSelectItem,
                        initialOptionsSubMenuState = it[1] as AdditionalOptionSubMenuState,
                        initialOptionStateHolder = it[2] as AdditionalOptionMenuState,
                    ),
                    modalBottomSheetState = modalBottomSheetState
                )
            }
        )
    }

    val isTransitionToKeyboardOnGoing
        @Composable get() = additionalOptionStateHolder.additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden &&
                !KeyboardHelper.isKeyboardVisible() &&
                messageCompositionInputStateHolder.inputFocused
    val additionalOptionSubMenuVisible
        @Composable get() = additionalOptionStateHolder.additionalOptionsSubMenuState != AdditionalOptionSubMenuState.Hidden &&
                !KeyboardHelper.isKeyboardVisible()

    private var isKeyboardVisible = false

    val isSelfDeletingSettingEnabled = messageComposerViewState.value.selfDeletionTimer !is SelfDeletionTimer.Disabled &&
            messageComposerViewState.value.selfDeletionTimer !is SelfDeletionTimer.Enforced

    fun toInActive() {
        messageCompositionInputStateHolder.toInActive()
        messageCompositionInputStateHolder.clearFocus()
    }

    fun toActive(showAttachmentOption: Boolean) {
        messageCompositionInputStateHolder.toActive(!showAttachmentOption)
        if (showAttachmentOption) {
            additionalOptionStateHolder.showAdditionalOptionsMenu()
        } else {
            additionalOptionStateHolder.hideAdditionalOptionsMenu()
        }
    }

    fun toEdit(messageId: String, editMessageText: String, mentions: List<MessageMention>) {
        messageCompositionHolder.setEditText(messageId, editMessageText, mentions)
        messageCompositionInputStateHolder.toEdit()
    }

    fun toReply(message: UIMessage.Regular) {
        messageCompositionHolder.setReply(message)
        messageCompositionInputStateHolder.toComposing()
    }

    fun onInputFocusedChanged(onFocused: Boolean) {
        if (onFocused) {
            additionalOptionStateHolder.hideAdditionalOptionsMenu()
            messageCompositionInputStateHolder.requestFocus()
        } else {
            messageCompositionInputStateHolder.clearFocus()
        }
    }

    fun toAudioRecording() {
        messageCompositionInputStateHolder.hide()
        additionalOptionStateHolder.toAudioRecording()
    }

    fun toCloseAudioRecording() {
        messageCompositionInputStateHolder.show()
        additionalOptionStateHolder.hideAudioRecording()
    }

    fun onKeyboardVisibilityChanged(isVisible: Boolean) {
        val isKeyboardClosed = isKeyboardVisible && !isVisible

        if (isKeyboardClosed) {
            if (!modalBottomSheetState.isVisible &&
                additionalOptionStateHolder.additionalOptionsSubMenuState == AdditionalOptionSubMenuState.Hidden
            ) {
                messageCompositionInputStateHolder.toInActive()
            } else {
                messageCompositionInputStateHolder.clearFocus()
            }
        }

        isKeyboardVisible = isVisible
    }

    fun cancelEdit() {
        messageCompositionInputStateHolder.toComposing()
        messageCompositionHolder.clearMessage()
    }

    fun showAdditionalOptionsMenu() {
        additionalOptionStateHolder.showAdditionalOptionsMenu()
        messageCompositionInputStateHolder.clearFocus()
    }

    fun onMessageSend() {
        messageCompositionHolder.clearMessage()
    }
}
