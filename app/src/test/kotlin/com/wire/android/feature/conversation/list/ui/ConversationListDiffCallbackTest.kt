package com.wire.android.feature.conversation.list.ui

import com.wire.android.UnitTest
import com.wire.android.feature.contact.Contact
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
    fun `given two items with same ids, when areItemsTheSame is called, then returns true`() {
        val oldItem = mockk<ConversationListItem>().also {
            every { it.id } returns TEST_ID
        }

        val newItem = mockk<ConversationListItem>().also {
            every { it.id } returns TEST_ID
        }

        val result = conversationListDiffCallback.areItemsTheSame(oldItem, newItem)

        result shouldBeEqualTo true
    }

    @Test
    fun `given two items with different ids, when areItemsTheSame is called, then returns false`() {
        val oldItem = mockk<ConversationListItem>().also {
            every { it.id } returns "abc"
        }

        val newItem = mockk<ConversationListItem>().also {
            every { it.id } returns "def"
        }

        val result = conversationListDiffCallback.areItemsTheSame(oldItem, newItem)

        result shouldBeEqualTo false
    }

    @Test
    fun `given two items with different names, when areContentsTheSame is called, then returns false`() {
        val oldItem = mockConversationListItem(name = "Old Name")
        val newItem = mockConversationListItem(name = "New Name")

        val result = conversationListDiffCallback.areContentsTheSame(oldItem, newItem)

        result shouldBeEqualTo false
    }

    @Test
    fun `given two items with different members, when areContentsTheSame is called, then returns false`() {
        val oldItem = mockConversationListItem(members = listOf(mockk()))
        val newItem = mockConversationListItem(members = listOf(mockk()))

        val result = conversationListDiffCallback.areContentsTheSame(oldItem, newItem)

        result shouldBeEqualTo false
    }

    @Test
    fun `given two items with same contents, when areContentsTheSame is called, then returns true`() {
        val oldItem = mockConversationListItem()
        val newItem = mockConversationListItem()

        val result = conversationListDiffCallback.areContentsTheSame(oldItem, newItem)

        result shouldBeEqualTo true
    }

    companion object {
        private const val TEST_ID = "conversation-id"
        private const val TEST_NAME = "Conversation Name"
        private val TEST_MEMBERS = listOf<Contact>(mockk())

        private fun mockConversationListItem(
            name: String = TEST_NAME,
            members: List<Contact> = TEST_MEMBERS
        ) = mockk<ConversationListItem>().also {
            every { it.name } returns name
            every { it.members } returns members
        }
    }
}
