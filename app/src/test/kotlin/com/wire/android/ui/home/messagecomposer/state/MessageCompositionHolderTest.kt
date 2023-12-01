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

import android.content.Context
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MessageCompositionHolderTest {

    @MockK
    lateinit var context: Context

    private lateinit var state: MessageCompositionHolder

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        state = MessageCompositionHolder(context = context)
    }

    @Test
    fun `given empty text, when adding header markdown, then # is added to the text`() = runTest {
        // given
        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Header)

        // then
        assertEquals(
            "# ",
            state.messageComposition.value.messageText
        )
    }

    @Test
    fun `given empty text, when adding bold markdown, then 2x star char is added to the text`() = runTest {
        // given
        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Bold)

        // then
        assertEquals(
            "****",
            state.messageComposition.value.messageText
        )
    }

    @Test
    fun `given empty text, when adding italic markdown, then 2x _ is added to the text`() = runTest {
        // given
        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Italic)

        // then
        assertEquals(
            "__",
            state.messageComposition.value.messageText
        )
    }

    @Test
    fun `given non empty text, when adding header markdown on selection, then # is added to the text`() = runTest {
        // given
        state.messageComposition.update {
            it.copy(
                messageTextFieldValue = TextFieldValue(
                    text = "header",
                    selection = TextRange(
                        start = 0,
                        end = 6
                    )
                )
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Header)

        // then
        assertEquals(
            "# header",
            state.messageComposition.value.messageText
        )
    }

    @Test
    fun `given non empty text, when adding bold markdown on selection, then 2x star char is added to the text`() = runTest {
        // given
        state.messageComposition.update {
            it.copy(
                messageTextFieldValue = TextFieldValue(
                    text = "bold",
                    selection = TextRange(
                        start = 0,
                        end = 4
                    )
                )
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Bold)

        // then
        assertEquals(
            "**bold**",
            state.messageComposition.value.messageText
        )
    }

    @Test
    fun `given non empty text, when adding italic markdown on selection, then 2x _ is added to the text`() = runTest {
        // given
        state.messageComposition.update {
            it.copy(
                messageTextFieldValue = TextFieldValue(
                    text = "italic",
                    selection = TextRange(
                        start = 0,
                        end = 6
                    )
                )
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Italic)

        // then
        assertEquals(
            "_italic_",
            state.messageComposition.value.messageText
        )
    }
}
