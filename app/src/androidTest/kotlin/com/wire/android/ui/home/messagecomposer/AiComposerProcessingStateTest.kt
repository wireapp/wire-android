/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.textfield.MessageComposerDefault
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.InputType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import org.junit.Rule
import org.junit.Test

class AiComposerProcessingStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenIdleComposer_whenNoAiActionIsRunning_thenActionButtonsAndSendAreVisible() {
        composeTestRule.setContent {
            WireTestTheme {
                TestActiveMessageComposerInput(
                    activeAiAction = null,
                    canUndo = true,
                )
            }
        }

        composeTestRule.onNodeWithTag(AI_PROOFREAD_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_FORMAL_TONE_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_INFORMAL_TONE_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_CUSTOM_PROMPT_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_UNDO_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_SEND_ACTIONS_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_PROCESSING_INDICATOR_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Processing on device...").assertDoesNotExist()
    }

    @Test
    fun givenAiActionIsRunning_whenComposerRecomposes_thenShowsProcessingAndHidesButtons() {
        composeTestRule.setContent {
            WireTestTheme {
                TestActiveMessageComposerInput(
                    activeAiAction = AiMessageComposerAction.Proofread,
                    canUndo = true,
                )
            }
        }

        composeTestRule.onNodeWithTag(AI_PROCESSING_INDICATOR_TAG).assertExists()
        composeTestRule.onNodeWithText("Processing on device...").assertExists()
        composeTestRule.onNodeWithTag(AI_PROOFREAD_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AI_FORMAL_TONE_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AI_INFORMAL_TONE_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AI_CUSTOM_PROMPT_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AI_UNDO_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AI_SEND_ACTIONS_TAG).assertDoesNotExist()
    }

    @Test
    fun givenCustomPromptIsRunning_whenComposerRecomposes_thenShowsSameProcessingState() {
        composeTestRule.setContent {
            WireTestTheme {
                TestActiveMessageComposerInput(
                    activeAiAction = AiMessageComposerAction.CustomPrompt,
                    canUndo = true,
                )
            }
        }

        composeTestRule.onNodeWithTag(AI_PROCESSING_INDICATOR_TAG).assertExists()
        composeTestRule.onNodeWithText("Processing on device...").assertExists()
        composeTestRule.onNodeWithTag(AI_CUSTOM_PROMPT_BUTTON_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(AI_SEND_ACTIONS_TAG).assertDoesNotExist()
    }

    @Test
    fun givenAiActionFinishes_whenComposerReturnsToIdle_thenButtonsBecomeVisibleAgain() {
        val activeAiAction = mutableStateOf<AiMessageComposerAction?>(AiMessageComposerAction.FormalTone)

        composeTestRule.setContent {
            WireTestTheme {
                TestActiveMessageComposerInput(
                    activeAiAction = activeAiAction.value,
                    canUndo = true,
                )
            }
        }

        composeTestRule.runOnIdle {
            activeAiAction.value = null
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(AI_PROCESSING_INDICATOR_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Processing on device...").assertDoesNotExist()
        composeTestRule.onNodeWithTag(AI_PROOFREAD_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_FORMAL_TONE_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_INFORMAL_TONE_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_CUSTOM_PROMPT_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_UNDO_BUTTON_TAG).assertExists()
        composeTestRule.onNodeWithTag(AI_SEND_ACTIONS_TAG).assertExists()
    }
}

@androidx.compose.runtime.Composable
private fun TestActiveMessageComposerInput(
    activeAiAction: AiMessageComposerAction?,
    canUndo: Boolean,
) {
    ActiveMessageComposerInput(
        conversationId = ConversationId("conversation-id", "domain"),
        messageComposition = MessageComposition(ConversationId("conversation-id", "domain")),
        messageTextState = TextFieldState("Hello"),
        isTextExpanded = true,
        inputType = InputType.Composing(isSendButtonEnabled = true),
        focusRequester = remember { FocusRequester() },
        keyboardOptions = KeyboardOptions.MessageComposerDefault,
        onKeyboardAction = null,
        canSendMessage = true,
        selfDeletionTimer = SelfDeletionTimer.Disabled,
        activeAiAction = activeAiAction,
        canUndo = canUndo,
        onSendButtonClicked = {},
        onUndoButtonClicked = {},
        onProofreadButtonClicked = {},
        onAdjustToneButtonClicked = {},
        onCustomPromptButtonClicked = {},
        onEditButtonClicked = {},
        onChangeSelfDeletionClicked = {},
        onToggleInputSize = {},
        onCancelReply = {},
        onCancelEdit = {},
        onFocused = {},
        onSelectedLineIndexChanged = {},
        onLineBottomYCoordinateChanged = {},
        showOptions = true,
        optionsSelected = false,
        onPlusClick = {},
    )
}
