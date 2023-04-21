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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
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
class MessageComposerInnerStateTest {

    @MockK
    lateinit var context: Context

    @MockK
    lateinit var focusManager: FocusManager

    @MockK
    lateinit var focusRequester: FocusRequester

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
    }

    @Test
    fun `given some message, when mention symbol is added into message, then mention is queried`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("start text"))

        state.setMessageTextValue(textFieldValueWithSelection("start text @"))

        assertEquals("", state.mentionQueryFlowState.value)
    }

    @Test
    fun `given some message, when mention symbol is added without space before it, then mention is not queried`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("start text"))

        state.setMessageTextValue(textFieldValueWithSelection("start text@"))

        assertEquals(null, state.mentionQueryFlowState.value)
    }

    @Test
    fun `given mention is started in message, then mention is queried with corresponding query`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("start text @"))

        state.setMessageTextValue(textFieldValueWithSelection("start text @abc"))

        assertEquals("abc", state.mentionQueryFlowState.value)
    }

    @Test
    fun `given mention is started in message, when user type space, then mention stop querying`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("start text @"))

        state.setMessageTextValue(textFieldValueWithSelection("start text @abc"))
        assertEquals("abc", state.mentionQueryFlowState.value)

        state.setMessageTextValue(textFieldValueWithSelection("start text @abc "))
        assertEquals(null, state.mentionQueryFlowState.value)
    }

    @Test
    fun `when mention selected, then mention is added into list`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(TextFieldValue("start text @use testing", TextRange(15)))

        state.addMention(contact())

        val expectedMention = listOf(UiMention(11, 11, UserId("id_0", "domain"), "@user name0"))
        assertEquals(expectedMention, state.mentions)
        assertEquals("start text @user name0 testing", state.messageComposeInputState.messageText.text)
    }

    @Test
    fun `when text before mention was changed, then mention's position is updated`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(TextFieldValue("start text @use testing", TextRange(15)))
        state.addMention(contact())

        state.setMessageTextValue(TextFieldValue("start text changed @user name0 testing", TextRange(18)))

        val expected = listOf(UiMention(19, 11, UserId("id_0", "domain"), "@user name0"))
        assertEquals(expected, state.mentions)
    }

    @Test
    fun `when text before mention was changed 2, then mention's position is updated`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(TextFieldValue("start text @use testing", TextRange(15)))
        state.addMention(contact())

        state.setMessageTextValue(TextFieldValue("@user name0 testing", TextRange(0)))

        val expected = listOf(UiMention(0, 11, UserId("id_0", "domain"), "@user name0"))
        assertEquals(expected, state.mentions)
    }

    @Test
    fun `when mention-text was edited, then mention is removed from the list`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("start text @"))
        state.addMention(contact())

        state.setMessageTextValue(textFieldValueWithSelection("start text @user na"))

        val expected = listOf<UiMention>()
        assertEquals(expected, state.mentions)
    }

    // case was found by manual testing
    @Test
    fun `given message starts from mention, when mention symbol is removed, then mention is not requested anymore`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("@"))

        assertEquals("", state.mentionQueryFlowState.value)
        state.setMessageTextValue(textFieldValueWithSelection(""))

        assertEquals(null, state.mentionQueryFlowState.value)
    }

    // case was found by manual testing
    @Test
    fun `given selection goes just before mention symbol, then mention is not requested`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("@ @ "))

        state.setMessageTextValue(TextFieldValue("@ @ ", TextRange(3)))

        assertEquals("", state.mentionQueryFlowState.value)

        state.setMessageTextValue(TextFieldValue("@ @ ", TextRange(2)))

        assertEquals(null, state.mentionQueryFlowState.value)
    }

    // case was found by manual testing
    @Test
    fun `given cursor is at the begin of new line, when mention symbol is added, then mention is requested`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("some text\n"))

        state.setMessageTextValue(textFieldValueWithSelection("some text\n@"))

        assertEquals("", state.mentionQueryFlowState.value)
    }

    // case was found by manual testing
    @Test
    fun `given cursor is at the begin of new line, when add mention button clicked, then mention is requested`() = runTest {
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("some text\n"))
        state.startMention()

        assertEquals("", state.mentionQueryFlowState.value)
        assertEquals("some text\n@", state.messageComposeInputState.messageText.text)
    }

    @Test
    fun `given some text, when editing the message, input text changes to the original message text`() = runTest {
        val originalMessageText = "original message text"
        val state = createState(context, focusManager, focusRequester)
        state.setMessageTextValue(textFieldValueWithSelection("start text"))
        state.toEditMessage("message-id", originalMessageText, listOf())
        assert(state.messageComposeInputState.isEditMessage)
        assertEquals(originalMessageText, state.messageComposeInputState.messageText.text)
    }

    private fun textFieldValueWithSelection(text: String) = TextFieldValue(text, TextRange(text.length))

    private fun contact(suffix: Int = 0) = Contact(
        id = "id_$suffix",
        domain = "domain",
        name = "user name$suffix",
        connectionState = ConnectionState.ACCEPTED,
        membership = Membership.Guest
    )

    companion object {
        fun createState(context: Context, focusManager: FocusManager, focusRequester: FocusRequester) = MessageComposerInnerState(
            context,
            AttachmentInnerState(context),
            focusManager,
            focusRequester,
            SpanStyle(),
        )
    }
}
