/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso.pressBack
import com.wire.android.ui.home.messagecomposer.state.AdditionalOptionSubMenuState
import com.wire.android.ui.home.messagecomposer.state.MessageCompositionInputStateHolder
import org.junit.Rule
import org.junit.Test

class MessageComposerBackHandlerTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun givenExpandedComposer_whenPressingSystemBack_thenComposerCollapses() {
        showComposer(expanded = true)

        composeTestRule.onNodeWithTag(COMPOSER_BACK_STATE_TAG).assertTextEquals("expanded")
        pressBack()
        composeTestRule.onNodeWithTag(COMPOSER_BACK_STATE_TAG).assertTextEquals("collapsed")
    }

    @Test
    fun givenCollapsedComposerWithFocusFlag_whenPressingSystemBack_thenConversationCloses() {
        showComposer(expanded = false)

        composeTestRule.onNodeWithTag(COMPOSER_BACK_STATE_TAG).assertTextEquals("collapsed")
        pressBack()
        composeTestRule.onNodeWithTag(COMPOSER_BACK_STATE_TAG).assertTextEquals("conversation list")
    }

    private fun showComposer(expanded: Boolean) {
        composeTestRule.setContent {
            var returnedToConversationList by remember { mutableStateOf(false) }
            val focusManager = LocalFocusManager.current
            val focusRequester = remember { FocusRequester() }
            val stateHolder = remember(focusManager) {
                MessageCompositionInputStateHolder(
                    messageTextState = TextFieldState(),
                    keyboardController = null,
                    focusManager = focusManager,
                    focusRequester = focusRequester
                )
            }

            LaunchedEffect(stateHolder) {
                // Closing the message menu sets this flag even when the keyboard stays hidden.
                stateHolder.setFocused()
                if (expanded) stateHolder.toggleInputSize()
            }

            BackHandler {
                returnedToConversationList = true
            }
            MessageComposerCollapseBackHandler(stateHolder, AdditionalOptionSubMenuState.Default, isImeVisible = false)

            Text(
                text = when {
                    returnedToConversationList -> "conversation list"
                    stateHolder.isTextExpanded -> "expanded"
                    else -> "collapsed"
                },
                modifier = Modifier.testTag(COMPOSER_BACK_STATE_TAG),
            )
        }
    }
}

private const val COMPOSER_BACK_STATE_TAG = "composer_back_state"
