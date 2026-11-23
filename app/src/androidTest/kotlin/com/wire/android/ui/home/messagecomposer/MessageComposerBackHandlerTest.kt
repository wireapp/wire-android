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
import androidx.compose.runtime.remember
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
        composeTestRule.setContent {
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
                stateHolder.inputFocused = true
                stateHolder.toggleInputSize()
            }

            BackHandler(stateHolder.inputFocused) {
                stateHolder.collapseComposer(AdditionalOptionSubMenuState.Default)
            }

            Text(
                text = if (stateHolder.isTextExpanded) "expanded" else "collapsed",
                modifier = Modifier.testTag(COMPOSER_BACK_STATE_TAG)
            )
        }

        composeTestRule.onNodeWithTag(COMPOSER_BACK_STATE_TAG).assertTextEquals("expanded")
        pressBack()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(COMPOSER_BACK_STATE_TAG).assertTextEquals("collapsed")
    }
}

private const val COMPOSER_BACK_STATE_TAG = "composer_back_state"
