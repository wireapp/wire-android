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
package com.wire.android.ui.home.messagecomposer.state

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.ui.unit.dp
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.util.EMPTY
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class MessageCompositionInputStateHolderTest {

    @Test
    fun `when offset increases and is bigger than previous and options height, options height is updated`() = runTest {
        val (state, _) = Arrangement().arrange()
        // When
        state.handleImeOffsetChange(
            50.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 50.dp
        state.subOptionsVisible shouldBeEqualTo false
    }

    @Test
    fun `when offset decreases and showSubOptions is false, options height is updated`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(previousOffset = 50.dp)

        // When
        state.handleImeOffsetChange(
            20.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 20.dp
    }

    @Test
    fun `when offset decreases to zero, showOptions and isTextExpanded are set to false`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(previousOffset = 50.dp)

        // When
        state.handleImeOffsetChange(
            0.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsVisible shouldBeEqualTo false
        state.isTextExpanded shouldBeEqualTo false
    }

    @Test
    fun `when offset equals keyboard height, showSubOptions is set to false`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(keyboardHeight = 30.dp)

        // When
        state.handleImeOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.subOptionsVisible shouldBeEqualTo false
    }

    @Test
    fun `when offset is greater than keyboard height, keyboardHeight is updated`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(keyboardHeight = 20.dp)

        // When
        state.handleImeOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.keyboardHeight shouldBeEqualTo 30.dp
    }

    @Test
    fun `when offset increases and is greater than keyboardHeight but is less than previousOffset, keyboardHeight is updated`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(previousOffset = 50.dp, keyboardHeight = 20.dp)

        // When
        state.handleImeOffsetChange(
            30.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.keyboardHeight shouldBeEqualTo 30.dp
        state.optionsHeight shouldBeEqualTo 30.dp
    }

    @Test
    fun `when offset decreases, showSubOptions is true, and actualOffset is greater than optionsHeight, values remain unchanged`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            state.updateValuesForTesting(
                previousOffset = 50.dp,
                keyboardHeight = 20.dp,
                showSubOptions = true,
                optionsHeight = 10.dp
            )

            // When
            state.handleImeOffsetChange(
                30.dp,
                NAVIGATION_BAR_HEIGHT,
                SOURCE,
                TARGET
            )

            // Then
            state.optionsHeight shouldBeEqualTo 10.dp
        }

    @Test
    fun `when offset decreases, showSubOptions is false, and actualOffset is greater than optionsHeight, optionsHeight is updated`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            state.updateValuesForTesting(
                previousOffset = 50.dp,
                keyboardHeight = 20.dp,
                showSubOptions = false,
                optionsHeight = 10.dp
            )

            // When
            state.handleImeOffsetChange(
                30.dp,
                NAVIGATION_BAR_HEIGHT,
                SOURCE,
                TARGET
            )

            // Then
            state.optionsHeight shouldBeEqualTo 30.dp
        }

    @Test
    fun `when offset is the same as previousOffset and greater than current keyboardHeight, keyboardHeight is updated`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(previousOffset = 40.dp, keyboardHeight = 20.dp)

        // When
        state.handleImeOffsetChange(
            40.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.keyboardHeight shouldBeEqualTo 40.dp
        state.optionsHeight shouldBeEqualTo 0.dp
    }

    @Test
    fun `given first keyboard appear when source equals target, then initialKeyboardHeight is set`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        val imeValue = 50.dp
        state.updateValuesForTesting(initialKeyboardHeight = 0.dp)

        // When
        state.handleImeOffsetChange(20.dp, NAVIGATION_BAR_HEIGHT, source = imeValue, target = imeValue)

        // Then
        state.initialKeyboardHeight shouldBeEqualTo imeValue
    }

    @Test
    fun `given extended keyboard height when attachment button is clicked, then keyboardHeight is set to initialKeyboardHeight`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            val initialKeyboardHeight = 10.dp
            state.updateValuesForTesting(previousOffset = 40.dp, keyboardHeight = 20.dp, initialKeyboardHeight = initialKeyboardHeight)

            // When
            state.showOptions()
            state.handleImeOffsetChange(0.dp, NAVIGATION_BAR_HEIGHT, source = TARGET, target = SOURCE)

            // Then
            state.keyboardHeight shouldBeEqualTo 20.dp
            state.optionsHeight shouldBeEqualTo initialKeyboardHeight
        }

    @Test
    fun `when offset decreases but is not zero, only optionsHeight is updated`() = runTest {
        // Given
        val (state, _) = Arrangement().arrange()
        state.updateValuesForTesting(previousOffset = 50.dp)

        // When
        state.handleImeOffsetChange(
            10.dp,
            NAVIGATION_BAR_HEIGHT,
            SOURCE,
            TARGET
        )

        // Then
        state.optionsHeight shouldBeEqualTo 10.dp
        state.optionsVisible shouldBeEqualTo false
        state.isTextExpanded shouldBeEqualTo false
    }

    @Test
    fun `when keyboard is still in a process of hiding from the previous screen after navigating, options should not be visible`() =
        runTest {
            // Given
            val (state, _) = Arrangement().arrange()
            state.updateValuesForTesting(previousOffset = 0.dp)

            // When
            state.handleImeOffsetChange(
                offset = 40.dp,
                navBarHeight = NAVIGATION_BAR_HEIGHT,
                source = 50.dp,
                target = 0.dp
            )

            // Then
            state.optionsHeight shouldBeEqualTo 0.dp
            state.optionsVisible shouldBeEqualTo false
            state.isTextExpanded shouldBeEqualTo false
        }

    @Test
    fun `given empty text, when composing, then send button should be disabled`() = runTest {
        // Given
        val messageText = String.EMPTY
        val (state, _) = Arrangement()
            .withText(messageText)
            .arrange()

        // When
        state.toComposing()

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Composing>().let {
            it.isSendButtonEnabled shouldBeEqualTo false
        }
    }

    @Test
    fun `given non-empty text but with only empty markdown, when composing, then send button should be disabled`() = runTest {
        // Given
        val messageText = "# " // just an example, more combinations are tested in StringUtilTest
        val (state, _) = Arrangement()
            .withText(messageText)
            .arrange()

        // When
        state.toComposing()

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Composing>().let {
            it.isSendButtonEnabled shouldBeEqualTo false
        }
    }

    @Test
    fun `given non-empty text, when composing, then send button should be enabled`() = runTest {
        // Given
        val messageText = "text"
        val (state, _) = Arrangement()
            .withText(messageText)
            .arrange()

        // When
        state.toComposing()

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Composing>().let {
            it.isSendButtonEnabled shouldBeEqualTo true
        }
    }

    @Test
    fun `given empty text, when editing, then send button should be disabled`() = runTest {
        // Given
        val editMessageText = "edit"
        val messageText = String.EMPTY
        val (state, _) = Arrangement()
            .withText(messageText)
            .arrange()

        // When
        state.toEdit(editMessageText)

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Editing>().let {
            it.isEditButtonEnabled shouldBeEqualTo false
        }
    }

    @Test
    fun `given non-empty text bit with only empty markdown, when editing, then send button should be disabled`() = runTest {
        // Given
        val editMessageText = "edit"
        val messageText = "# " // just an example, more combinations are tested in StringUtilTest
        val (state, _) = Arrangement()
            .withText(messageText)
            .arrange()

        // When
        state.toEdit(editMessageText)

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Editing>().let {
            it.isEditButtonEnabled shouldBeEqualTo false
        }
    }

    @Test
    fun `given the same text as edit message text, when editing, then send button should be disabled`() = runTest {
        // Given
        val editMessageText = "edit"
        val (state, _) = Arrangement()
            .withText(editMessageText)
            .arrange()

        // When
        state.toEdit(editMessageText)

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Editing>().let {
            it.isEditButtonEnabled shouldBeEqualTo false
        }
    }

    @Test
    fun `given different text than edit message text, when editing, then send button should be enabled`() = runTest {
        // Given
        val editMessageText = "edit"
        val messageText = "$editMessageText new"
        val (state, _) = Arrangement()
            .withText(messageText)
            .arrange()

        // When
        state.toEdit(editMessageText)

        // Then
        state.inputType.shouldBeInstanceOf<InputType.Editing>().let {
            it.isEditButtonEnabled shouldBeEqualTo true
        }
    }

    class Arrangement {

        private val textFieldState = TextFieldState()
        private val state = MessageCompositionInputStateHolder(textFieldState)

        fun withText(text: String) = apply {
            textFieldState.setTextAndPlaceCursorAtEnd(text)
        }

        fun arrange() = state to this
    }

    companion object {
        // I set it 0 to make tests more straight forward
        val NAVIGATION_BAR_HEIGHT = 0.dp
        val SOURCE = 0.dp
        val TARGET = 50.dp
    }
}
