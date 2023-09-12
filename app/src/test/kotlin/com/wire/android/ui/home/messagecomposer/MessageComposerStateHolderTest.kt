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

package com.wire.android.ui.home.messagecomposer

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.common.bottomsheet.WireModalSheetState
import com.wire.android.ui.home.conversations.MessageComposerViewState
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionMenuState
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSelectItem
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionStateHolder
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.EnabledMessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposerStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputSize
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputState
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionType
import com.wire.android.ui.home.messagecomposer.state.MessageType
import com.wire.kalium.logic.feature.selfDeletingMessages.SelfDeletionTimer
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageComposerStateHolderTest {

    @MockK
    lateinit var context: Context

    private lateinit var messageComposerViewState: MutableState<MessageComposerViewState>

    private lateinit var messageComposition: MutableState<MessageComposition>
    private lateinit var messageCompositionInputStateHolder: MessageCompositionInputStateHolder

    private lateinit var messageCompositionHolder: MessageCompositionHolder

    private lateinit var additionalOptionStateHolder: AdditionalOptionStateHolder

    private lateinit var modalBottomSheetState: WireModalSheetState

    private lateinit var enabledMessageComposerStateHolder: EnabledMessageComposerStateHolder

    private lateinit var state: MessageComposerStateHolder

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        messageComposerViewState = mutableStateOf(MessageComposerViewState())
        messageComposition = mutableStateOf(MessageComposition())
        messageCompositionInputStateHolder = MessageCompositionInputStateHolder(
            messageComposition = messageComposition,
            selfDeletionTimer = mutableStateOf(SelfDeletionTimer.Disabled)
        )
        messageCompositionHolder = MessageCompositionHolder(
            context = context
        )
        additionalOptionStateHolder = AdditionalOptionStateHolder()
        modalBottomSheetState = WireModalSheetState()
        enabledMessageComposerStateHolder = EnabledMessageComposerStateHolder()

        state = MessageComposerStateHolder(
            messageComposerViewState = messageComposerViewState,
            messageCompositionInputStateHolder = messageCompositionInputStateHolder,
            messageCompositionHolder = messageCompositionHolder,
            additionalOptionStateHolder = additionalOptionStateHolder,
            modalBottomSheetState = modalBottomSheetState,
            enabledMessageComposerStateHolder = enabledMessageComposerStateHolder
        )
    }

//    @Test
//    fun `given state, when setting toInactive, then input state holder inactive is called`() =
//        runTest {
//            // given
//            // when
//            state.toInActive()
//
//            // then
//            assertEquals(true, messageCompositionInputStateHolder.inputVisibility)
//            assertEquals(
//                MessageCompositionInputSize.COLLAPSED,
//                messageCompositionInputStateHolder.inputSize
//            )
//            assertEquals(
//                MessageCompositionInputState.INACTIVE,
//                messageCompositionInputStateHolder.inputState
//            )
//        }

//    @Test
//    fun `given state, when setting toActive and show attachment true, then input state active is called`() =
//        runTest {
//            // given
//            // when
//            state.toActive(showAttachmentOption = true)
//
//            // then
//            assertEquals(true, messageCompositionInputStateHolder.inputVisibility)
//            assertEquals(
//                MessageCompositionInputSize.COLLAPSED,
//                messageCompositionInputStateHolder.inputSize
//            )
//            assertEquals(
//                MessageCompositionInputState.ACTIVE,
//                messageCompositionInputStateHolder.inputState
//            )
//            assertEquals(false, messageCompositionInputStateHolder.inputFocused)
//            assertEquals(
//                AdditionalOptionSelectItem.AttachFile,
//                additionalOptionStateHolder.selectedOption
//            )
//            assertEquals(
//                AdditionalOptionSubMenuState.AttachFile,
//                additionalOptionStateHolder.additionalOptionsSubMenuState
//            )
//        }

//    @Test
//    fun `given state, when setting toActive and show attachment false, then input state active is called`() =
//        runTest {
//            // given
//            // when
//            state.toActive(showAttachmentOption = false)
//
//            // then
//            assertEquals(true, messageCompositionInputStateHolder.inputVisibility)
//            assertEquals(
//                MessageCompositionInputSize.COLLAPSED,
//                messageCompositionInputStateHolder.inputSize
//            )
//            assertEquals(
//                MessageCompositionInputState.ACTIVE,
//                messageCompositionInputStateHolder.inputState
//            )
//            assertEquals(true, messageCompositionInputStateHolder.inputFocused)
//            assertEquals(
//                AdditionalOptionSelectItem.None,
//                additionalOptionStateHolder.selectedOption
//            )
//            assertEquals(
//                AdditionalOptionSubMenuState.Hidden,
//                additionalOptionStateHolder.additionalOptionsSubMenuState
//            )
//        }

    @Test
    fun `given state, when setting toEdit, then input state toEdit is called`() = runTest {
        // given
        // when
        state.toEdit(
            messageId = "messageId",
            editMessageText = "edit_message_text",
            mentions = listOf()
        )

        // then
        assertEquals(
            MessageCompositionType.Editing(
                messageCompositionState = messageComposition,
                messageCompositionSnapShot = messageComposition.value
            ).isEditButtonEnabled,
            (messageCompositionInputStateHolder.inputType as MessageCompositionType.Editing).isEditButtonEnabled
        )
    }

    @Test
    fun `given state, when setting toReply, then composition holder is correctly set up`() =
        runTest {
            // given
            // when
            state.toReply(
                message = mockMessageWithText
            )

            // then
            assertEquals(
                TextFieldValue("").text,
                messageCompositionHolder.messageComposition.value.messageTextFieldValue.text
            )
            assertEquals(
                MessageType.Normal,
                (messageCompositionInputStateHolder.inputType as MessageCompositionType.Composing)
                    .messageType
                    .value
            )
        }

//    @Test
//    fun `given state, when input focus change to true, then hide additional options menu and request focus`() =
//        runTest {
//            // given
//            // when
//            state.onInputFocusedChanged(onFocused = true)
//
//            // then
//            assertEquals(
//                AdditionalOptionSelectItem.None,
//                additionalOptionStateHolder.selectedOption
//            )
//            assertEquals(
//                AdditionalOptionSubMenuState.Hidden,
//                additionalOptionStateHolder.additionalOptionsSubMenuState
//            )
//            assertEquals(true, messageCompositionInputStateHolder.inputFocused)
//        }

    @Test
    fun `given state, when input focus change to false, then clear focus`() = runTest {
        // given
        // when
        state.onInputFocusedChanged(onFocused = false)

        // then
        assertEquals(false, messageCompositionInputStateHolder.inputFocused)
    }

//    @Test
//    fun `given state, when setting toAudioRecording, then show audio recording additional sub menu`() =
//        runTest {
//            // given
//            // when
//            state.toAudioRecording()
//
//            // then
//            assertEquals(false, messageCompositionInputStateHolder.inputVisibility)
//            assertEquals(
//                AdditionalOptionMenuState.Hidden,
//                additionalOptionStateHolder.additionalOptionState
//            )
//            assertEquals(
//                AdditionalOptionSubMenuState.RecordAudio,
//                additionalOptionStateHolder.additionalOptionsSubMenuState
//            )
//        }

//    @Test
//    fun `given state, when changing keyboard visibility to false, then show keyboard`() = runTest {
//        // given
//        state.onKeyboardVisibilityChanged(isVisible = true) // Setting as true to be shown first time
//
//        // when
//        state.onKeyboardVisibilityChanged(isVisible = false) // Now setting to false so it is hidden
//
//        // then
//        assertEquals(
//            false,
//            messageCompositionInputStateHolder.inputFocused
//        )
//    }

    @Test
    fun `given state, when requesting to show additional options menu, then additional options menu is shown`() =
        runTest {
            // given
            // when
            state.showAdditionalOptionsMenu()

            // then
            assertEquals(
                AdditionalOptionSelectItem.AttachFile,
                additionalOptionStateHolder.selectedOption
            )
            assertEquals(
                AdditionalOptionSubMenuState.AttachFile,
                additionalOptionStateHolder.additionalOptionsSubMenuState
            )
            assertEquals(false, messageCompositionInputStateHolder.inputFocused)
        }

    @Test
    fun `given state, when message is sent, then message is cleared`() = runTest {
        // given
        // when
        state.clearMessage()

        // then
        assertEquals(
            TextFieldValue("").text,
            messageCompositionHolder.messageComposition.value.messageTextFieldValue.text
        )
        assertEquals(
            null,
            messageCompositionHolder.messageComposition.value.quotedMessage
        )
        assertEquals(
            null,
            messageCompositionHolder.messageComposition.value.quotedMessageId
        )
    }
}
