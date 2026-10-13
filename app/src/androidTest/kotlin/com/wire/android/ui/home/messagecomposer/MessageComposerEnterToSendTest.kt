/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextRange
import com.wire.android.ui.WireTestTheme
import com.wire.android.ui.common.textfield.MessageComposerDefault
import com.wire.android.ui.common.textfield.MessageComposerEnterToSend
import com.wire.android.ui.common.textfield.wireTextFieldColors
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MessageComposerEnterToSendTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var textState: TextFieldState
    private var sends = 0

    @Test
    fun givenSettingOff_whenSoftwareKeyboardInsertsNewline_thenInsertsAtCursorWithoutSending() {
        showInput(enterToSend = false, selection = TextRange(5))

        composeTestRule.onNode(hasSetTextAction()).performTextInput("\n")

        assertInput("Hello\n World", TextRange(6))
    }

    @Test
    fun givenSettingOff_whenSoftwareKeyboardInsertsNewline_thenReplacesSelectionWithoutSending() {
        showInput(enterToSend = false, selection = TextRange(5, 11))

        composeTestRule.onNode(hasSetTextAction()).performTextInput("\n")

        assertInput("Hello\n", TextRange(6))
    }

    @Test
    fun givenSettingOff_whenEnterKeyIsPressed_thenInsertsAtCursorWithoutSending() {
        showInput(enterToSend = false, selection = TextRange(5))

        pressEnter(Key.Enter)

        assertInput("Hello\n World", TextRange(6))
    }

    @Test
    fun givenSettingOff_whenNumpadEnterIsPressed_thenReplacesSelectionWithoutSending() {
        showInput(enterToSend = false, selection = TextRange(5, 11))

        pressEnter(Key.NumPadEnter)

        assertInput("Hello\n", TextRange(6))
    }

    @Test
    fun givenSettingOff_whenKeyboardReportsEditorAction_thenDoesNotSend() {
        showInput(enterToSend = false)

        composeTestRule.onNode(hasSetTextAction()).performSemanticsAction(SemanticsActions.OnImeAction) { it() }

        composeTestRule.runOnIdle { assertEquals(0, sends) }
    }

    @Test
    fun givenSettingOn_whenSoftwareKeyboardSends_thenSendsExactlyOnce() {
        showInput(enterToSend = true)

        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        composeTestRule.runOnIdle { assertEquals(1, sends) }
    }

    @Test
    fun givenSettingOn_whenEnterKeyIsPressed_thenSendsExactlyOnce() {
        showInput(enterToSend = true)

        pressEnter(Key.Enter)

        composeTestRule.runOnIdle {
            assertEquals(1, sends)
            assertEquals("Hello World", textState.text.toString())
        }
    }

    @Test
    fun givenSettingOn_whenShiftEnterIsPressed_thenAddsNewlineWithoutSending() {
        showInput(enterToSend = true)

        composeTestRule.onNode(hasSetTextAction()).performKeyInput {
            keyDown(Key.ShiftLeft)
            keyDown(Key.Enter)
            keyUp(Key.Enter)
            keyUp(Key.ShiftLeft)
        }

        assertInput("Hello World\n", TextRange(12))
    }

    @Test
    fun givenMessageCannotBeSent_whenSoftwareKeyboardSends_thenDoesNotSend() {
        showInput(enterToSend = true, canSendMessage = false)

        composeTestRule.onNode(hasSetTextAction()).performImeAction()

        composeTestRule.runOnIdle { assertEquals(0, sends) }
    }

    @Test
    fun givenMessageCannotBeSent_whenEnterKeyIsPressed_thenDoesNotSend() {
        showInput(enterToSend = true, canSendMessage = false)

        pressEnter(Key.Enter)

        composeTestRule.runOnIdle { assertEquals(0, sends) }
    }

    private fun showInput(
        enterToSend: Boolean,
        selection: TextRange = TextRange(11),
        canSendMessage: Boolean = true,
    ) {
        textState = TextFieldState(initialText = "Hello World", initialSelection = selection)
        composeTestRule.setContent {
            WireTestTheme {
                val focusRequester = remember { FocusRequester() }
                MessageComposerTextInput(
                    messageTextState = textState,
                    focusRequester = focusRequester,
                    colors = wireTextFieldColors(),
                    placeHolderText = "Type a message",
                    onFocused = {},
                    keyboardOptions = if (enterToSend) {
                        KeyboardOptions.MessageComposerEnterToSend
                    } else {
                        KeyboardOptions.MessageComposerDefault
                    },
                    onKeyBoardAction = messageComposerKeyboardActionHandler(enterToSend, canSendMessage) { sends++ },
                    onHardwareEnter = { isShiftPressed ->
                        handleMessageComposerEnter(
                            enterToSend = enterToSend,
                            isShiftPressed = isShiftPressed,
                            canSendMessage = canSendMessage,
                            onNewLine = { textState.edit { append("\n") } },
                            onSend = { sends++ },
                        )
                    },
                    onHardwareTab = { false },
                    onHardwareEscape = { false },
                    useKeyboardActivationGate = false,
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun pressEnter(key: Key) {
        composeTestRule.onNode(hasSetTextAction()).performKeyInput {
            keyDown(key)
            keyUp(key)
        }
    }

    private fun assertInput(text: String, selection: TextRange) {
        composeTestRule.runOnIdle {
            assertEquals(text, textState.text.toString())
            assertEquals(selection, textState.selection)
            assertEquals(0, sends)
        }
    }
}
