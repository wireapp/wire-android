package com.wire.android.feature.conversation.list.ui

import com.wire.android.UnitTest
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.conversation.Conversation
import io.mockk.every
import io.mockk.mockk
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class ConversationListDiffCallbackTest : UnitTest() {

    private lateinit var conversationListDiffCallback: ConversationListDiffCallback

    @Before
    fun setUp() {
        conversationListDiffCallback = ConversationListDiffCallback()
    }

    @Test
    fun `given two items with same conversation ids, when areItemsTheSame is called, then returns true`() {
        val conversation1 = mockk<Conversation>().also {
            every { it.id } returns TEST_ID
        }
        val oldItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation1
        }

        val conversation2 = mockk<Conversation>().also {
            every { it.id } returns TEST_ID
        }
        val newItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation2
        }

        val result = conversationListDiffCallback.areItemsTheSame(oldItem, newItem)

        result shouldBeEqualTo true
    }

    @Test
    fun `given two items with different conversation ids, when areItemsTheSame is called, then returns false`() {
        val conversation1 = mockk<Conversation>().also {
            every { it.id } returns "abc"
        }
        val oldItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation1
        }

        val conversation2 = mockk<Conversation>().also {
            every { it.id } returns "def"
        }
        val newItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation2
        }

        val result = conversationListDiffCallback.areItemsTheSame(oldItem, newItem)

        result shouldBeEqualTo false
    }

    @Test
    fun `given two items with different conversations, when areContentsTheSame is called, then returns false`() {
        val conversation1 = mockk<Conversation>()
        val oldItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation1
        }

        val conversation2 = mockk<Conversation>()
        val newItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation2
        }

        val result = conversationListDiffCallback.areContentsTheSame(oldItem, newItem)

        result shouldBeEqualTo false
    }

    @Test
    fun `given two items with same conversation but different members, when areContentsTheSame is called, then returns false`() {
        val conversation = mockk<Conversation>()
        val oldItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation
            every { it.members } returns listOf(mockk())
        }

        val newItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation
            every { it.members } returns listOf(mockk())
        }

        val result = conversationListDiffCallback.areContentsTheSame(oldItem, newItem)

        result shouldBeEqualTo false
    }

    @Test
    fun `given two items with same contents, when areContentsTheSame is called, then returns true`() {
        val conversation = mockk<Conversation>()
        val members = listOf<Contact>(mockk())

        val oldItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation
            every { it.members } returns members
        }

        val newItem = mockk<ConversationListItem>().also {
            every { it.conversation } returns conversation
            every { it.members } returns members
        }

        val result = conversationListDiffCallback.areContentsTheSame(oldItem, newItem)

        result shouldBeEqualTo true
    }

    companion object {
        private const val TEST_ID = "conversation-id"
    }
}
