/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionHolder
import com.wire.kalium.logic.data.id.ConversationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class MessageComposerMentionUndoTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val conversation = ConversationId("group", "example.com")
    private lateinit var holder: MessageCompositionHolder

    @Test
    fun nativeUndoCannotTransferEveryoneToRestoredLiteralText() {
        showEditor()
        deleteRange(0, 5)
        composeTestRule.runOnIdle { assertEquals("@all ", holder.messageTextState.text.toString()) }

        composeTestRule.onNodeWithTag("editor").performKeyInput {
            keyDown(Key.CtrlLeft)
            keyDown(Key.Z)
            keyUp(Key.Z)
            keyUp(Key.CtrlLeft)
        }
        composeTestRule.runOnIdle { assertEquals("@all @all ", holder.messageTextState.text.toString()) }
        deleteRange(5, 10)

        composeTestRule.runOnIdle {
            assertEquals("@all ", holder.messageTextState.text.toString())
            assertTrue(outgoing().mentions.isEmpty())
        }
    }

    @Test
    fun immediateSendAfterNativeUndoCannotUseStaleRanges() {
        showEditor()
        deleteRange(0, 5)

        composeTestRule.runOnIdle {
            assertTrue(holder.messageTextState.undoState.canUndo)
            holder.messageTextState.undoState.undo()
            assertEquals("@all @all ", holder.messageTextState.text.toString())
            assertTrue(outgoing().mentions.isEmpty())
        }
    }

    private fun showEditor() {
        holder = MessageCompositionHolder(
            messageComposition = mutableStateOf(
                MessageComposition(
                    conversationId = conversation,
                    selectedMentions = listOf(UIMention(5, 4, null, "@all")),
                )
            ),
            messageTextState = TextFieldState("@all @all "),
            onClearDraft = {},
            onSaveDraft = {},
            onMessageTextUpdate = {},
            onSearchMentionQueryChanged = {},
            onClearMentionSearchResult = {},
            onTypingEvent = {},
        )
        composeTestRule.setContent {
            LaunchedEffect(holder) { holder.handleMessageTextUpdates() }
            BasicTextField(
                state = holder.messageTextState,
                inputTransformation = holder.mentionInputTransformation,
                modifier = Modifier.testTag("editor"),
            )
        }
        composeTestRule.onNodeWithTag("editor").performClick()
    }

    private fun deleteRange(start: Int, end: Int) {
        val editor = composeTestRule.onNodeWithTag("editor")
        editor.performTextInputSelection(TextRange(start, end))
        editor.performKeyInput {
            keyDown(Key.Backspace)
            keyUp(Key.Backspace)
        }
    }

    private fun outgoing() = holder.toMessageBundle(conversation, emptyList()) as ComposableMessageBundle.SendTextMessageBundle
}
