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

import android.content.Context
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.delete
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.TextRange
import com.wire.android.config.SnapshotExtension
import com.wire.android.framework.TestConversation
import com.wire.android.model.Contact
import com.wire.android.ui.common.attachmentdraft.model.AttachmentDraftUi
import com.wire.android.ui.home.conversations.model.UIMention
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.messagecomposer.model.ComposableMessageBundle
import com.wire.android.ui.home.messagecomposer.model.MessageComposition
import com.wire.android.ui.home.messagecomposer.model.toDraft
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import io.mockk.mockk
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(SnapshotExtension::class)
class MessageCompositionHolderTest {

    @MockK
    lateinit var context: Context

    private lateinit var state: MessageCompositionHolder

    private lateinit var messageComposition: MutableState<MessageComposition>
    private lateinit var messageTextState: TextFieldState
    private val dispatcher = StandardTestDispatcher()
    private val everyone = UIMention(0, 4, null, "@all")
    private val member = UIMention(5, 4, UserId("member", "example.com"), "@all")

    @BeforeEach
    fun before() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(dispatcher)

        messageComposition = mutableStateOf(MessageComposition(TestConversation.ID))
        messageTextState = TextFieldState()
        state = MessageCompositionHolder(
            messageComposition = messageComposition,
            messageTextState = messageTextState,
            onClearDraft = {},
            onSaveDraft = {},
            onMessageTextUpdate = {},
            onSearchMentionQueryChanged = {},
            onClearMentionSearchResult = {},
            onTypingEvent = {},
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given empty text, when adding header markdown, then # is added to the text`() = runTest {
        // given
        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Header)

        // then
        assertEquals(
            "# ",
            state.messageTextState.text.toString()
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
            state.messageTextState.text.toString()
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
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given non empty text, when adding header markdown on selection, then # is added to the text`() = runTest {
        // given
        state.messageTextState.edit {
            replace(0, length, "header")
            selection = TextRange(
                start = 0,
                end = 6
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Header)

        // then
        assertEquals(
            "# header",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given non empty text, when adding bold markdown on selection, then 2x star char is added to the text`() = runTest {
        // given
        state.messageTextState.edit {
            replace(0, length, "bold")
            selection = TextRange(
                start = 0,
                end = 4
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Bold)

        // then
        assertEquals(
            "**bold**",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given non empty text, when adding italic markdown on selection, then 2x _ is added to the text`() = runTest {
        // given
        state.messageTextState.edit {
            replace(0, length, "italic")
            selection = TextRange(
                start = 0,
                end = 6
            )
        }

        // when
        state.addOrRemoveMessageMarkdown(markdown = RichTextMarkdown.Italic)

        // then
        assertEquals(
            "_italic_",
            state.messageTextState.text.toString()
        )
    }

    @Test
    fun `given non empty text, when adding mention, mention is added`() = runTest {
        // given
        val text = "@men"
        state.messageTextState.edit {
            replace(0, length, text)
            selection = TextRange(start = text.length, end = text.length)
        }

        // when
        state.addMention(
            Contact(
                id = "id",
                domain = "domain",
                name = "name",
                handle = "men handle",
                connectionState = ConnectionState.ACCEPTED,
                membership = Membership.Guest
            )
        )

        // then
        assertEquals("@name ", state.messageTextState.text.toString())
        assertEquals(1, state.messageComposition.value.selectedMentions.size)
    }

    @Test
    fun `given non empty text without @ symbol, when adding mention, nothing happen`() = runTest {
        // given
        val text = "mann"
        state.messageTextState.edit {
            replace(0, length, text)
            selection = TextRange(start = 4, end = 4)
        }

        // when
        state.addMention(
            Contact(
                id = "id",
                domain = "domain",
                name = "name",
                handle = "men handle",
                connectionState = ConnectionState.ACCEPTED,
                membership = Membership.Guest
            )
        )

        // then
        assertEquals(text, state.messageTextState.text.toString())
        assertEquals(0, state.messageComposition.value.selectedMentions.size)
    }

    @Test
    fun `given all query, when choosing everyone, then draft retains everyone target`() = runTest {
        messageTextState.edit {
            replace(0, length, "Hi @al")
            selection = TextRange(length)
        }

        state.addEveryoneMention()

        assertEquals("Hi @all ", messageTextState.text.toString())
        assertEquals(TextRange(8), messageTextState.selection)
        val mention = messageComposition.value.selectedMentions.single()
        assertEquals(3, mention.start)
        assertEquals(4, mention.length)
        assertNull(mention.userId)
        assertTrue(mention.isEveryone)
        val draft = messageComposition.value.toDraft(messageTextState.text.toString())
        assertTrue(draft.selectedMentionList.single().isEveryone)
        state.restoreComposition(
            MessageComposition(TestConversation.ID, draft.text, selectedMentions = listOf(mention))
        )
        assertTrue(messageComposition.value.selectedMentions.single().isEveryone)
    }

    @Test
    fun `user deletion cannot turn an identical member into everyone in drafts or sends`() = runTest {
        restoreIdenticalLabels(listOf(everyone, member))

        userEdit { delete(0, 5) }

        assertEquals(listOf(member.copy(start = 0)), messageComposition.value.selectedMentions)
        val draft = messageComposition.value.toDraft(messageTextState.text.toString())
        assertEquals(member.userId, draft.selectedMentionList.single().userId)
        val bundle = state.toMessageBundle(TestConversation.ID, emptyList()) as ComposableMessageBundle.SendTextMessageBundle
        assertFalse(bundle.mentions.single().isEveryone)
    }

    @Test
    fun `user deletion cannot transfer everyone to identical plain text`() = runTest {
        restoreIdenticalLabels(listOf(everyone))

        userEdit { delete(0, 5) }

        assertEquals("@all ", messageTextState.text.toString())
        assertTrue(messageComposition.value.selectedMentions.isEmpty())
        assertTrue(messageComposition.value.toDraft(messageTextState.text.toString()).selectedMentionList.isEmpty())
    }

    @Test
    fun `immediate send after an untracked edit discards stale targets`() = runTest {
        restoreIdenticalLabels(listOf(everyone))
        messageTextState.edit { replace(0, 5, "") }

        val bundle = state.toMessageBundle(TestConversation.ID, emptyList()) as ComposableMessageBundle.SendTextMessageBundle

        assertEquals("@all ", bundle.message)
        assertTrue(bundle.mentions.isEmpty())
    }

    @Test
    fun `tracked edit after an unobserved history change cannot reuse targets`() = runTest {
        restoreIdenticalLabels(listOf(everyone.copy(start = 5)))
        userEdit { delete(0, 5) }
        messageTextState.edit { replace(0, length, "@all @all ") }

        userEdit { delete(5, 10) }

        assertTrue(messageComposition.value.selectedMentions.isEmpty())
    }

    @Test
    fun `trusted draft hydration keeps its own everyone target`() = runTest {
        state.restoreComposition(
            MessageComposition(TestConversation.ID, "Hi @all", selectedMentions = listOf(everyone.copy(start = 3)))
        )

        val bundle = state.toMessageBundle(TestConversation.ID, emptyList()) as ComposableMessageBundle.SendTextMessageBundle

        assertEquals("Hi @all", bundle.message)
        assertEquals(listOf(everyone.copy(start = 3)), bundle.mentions)
    }

    @Test
    fun `formatting and mention insertion preserve existing targets before and after the edits`() = runTest {
        restoreIdenticalLabels(listOf(everyone, member))
        messageTextState.edit { selection = TextRange(0, 4) }

        state.addOrRemoveMessageMarkdown(RichTextMarkdown.Bold)

        assertEquals(listOf(everyone.copy(start = 2), member.copy(start = 9)), messageComposition.value.selectedMentions)
        messageTextState.edit { selection = TextRange(0) }
        state.startMention()
        state.addEveryoneMention()
        assertEquals("@all **@all** @all ", messageTextState.text.toString())
        assertEquals(
            listOf(everyone, everyone.copy(start = 7), member.copy(start = 14)),
            messageComposition.value.selectedMentions
        )
    }

    @Test
    fun `everyone survives attachment captions and multipart edit drafts`() {
        state.restoreComposition(MessageComposition(TestConversation.ID, "@all", selectedMentions = listOf(everyone)))
        val sent = state.toMessageBundle(TestConversation.ID, listOf(mockk<AttachmentDraftUi>()))
            as ComposableMessageBundle.SendMultipartMessageBundle
        assertTrue(sent.mentions.single().intoMessageMention().isEveryone)

        state.setEditText("message-id", "@all", listOf(everyone.intoMessageMention()), isMultipart = true)
        val draft = messageComposition.value.toDraft(messageTextState.text.toString())
        assertTrue(draft.isMultipartEdit)
        assertTrue(draft.selectedMentionList.single().isEveryone)
        val edited = state.toMessageBundle(TestConversation.ID, emptyList())
            as ComposableMessageBundle.EditMultipartMessageBundle
        assertTrue(edited.newMentions.single().isEveryone)
    }

    @Test
    fun `removing formatting preserves untouched everyone and member targets`() {
        state.restoreComposition(
            MessageComposition(
                TestConversation.ID,
                "**@all** @all ",
                selectedMentions = listOf(everyone.copy(start = 2), member.copy(start = 9)),
            )
        )
        messageTextState.edit { selection = TextRange(8, 0) }

        state.addOrRemoveMessageMarkdown(RichTextMarkdown.Bold)

        assertEquals("@all @all ", messageTextState.text.toString())
        assertEquals(listOf(everyone, member), messageComposition.value.selectedMentions)
    }

    private fun restoreIdenticalLabels(mentions: List<UIMention>) {
        state.restoreComposition(MessageComposition(TestConversation.ID, "@all @all ", selectedMentions = mentions))
    }

    private fun userEdit(edit: TextFieldBuffer.() -> Unit) {
        messageTextState.edit {
            edit()
            with(state.mentionInputTransformation) { transformInput() }
        }
    }

    @Test
    fun `given member named all, when choosing member, then mention keeps member target`() = runTest {
        messageTextState.edit {
            replace(0, length, "@al")
            selection = TextRange(length)
        }

        state.addMention(
            Contact(
                id = "all-user-id",
                domain = "domain",
                name = "all",
                handle = "all",
                connectionState = ConnectionState.ACCEPTED,
                membership = Membership.Standard,
            )
        )

        assertEquals("@all ", messageTextState.text.toString())
        val mention = messageComposition.value.selectedMentions.single()
        assertFalse(mention.isEveryone)
        assertEquals("all-user-id", mention.userId?.value)
    }
}
