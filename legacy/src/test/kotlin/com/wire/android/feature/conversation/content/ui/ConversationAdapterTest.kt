package com.wire.android.feature.conversation.content.ui

import android.view.ViewGroup
import com.wire.android.UnitTest
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.contact.Contact
import com.wire.android.feature.conversation.content.Message
import com.wire.android.shared.asset.ui.imageloader.UserAvatarProvider
import com.wire.android.shared.conversation.content.ConversationTimeGenerator
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.amshove.kluent.any
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("WIP")
class ConversationAdapterTest : UnitTest() {

    @MockK
    private lateinit var viewHolderInflater: ViewHolderInflater

    @MockK
    private lateinit var userAvatarProvider: UserAvatarProvider

    @MockK
    private lateinit var messages: List<Any>

    @MockK
    private lateinit var conversationTimeGenerator: ConversationTimeGenerator

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var spyAdapter: ConversationAdapter

    @Before
    fun setUp() {
        conversationAdapter = ConversationAdapter(viewHolderInflater, userAvatarProvider, conversationTimeGenerator)
        spyAdapter = spyk(conversationAdapter)
        every { spyAdapter.notifyDataSetChanged() } returns Unit
    }

    @Test
    fun `given onCreateViewHolder is called, then creates an instance of ConversationTextMessageViewHolder`() {
        val parent = mockk<ViewGroup>()

        val viewHolder = conversationAdapter.onCreateViewHolder(parent, 0)

        viewHolder shouldBeInstanceOf ConversationTextMessageViewHolder::class.java
    }

    @Test
    fun `given onBindViewHolder is called, when viewType is text message, then calls message holder to bind the item at the position`() {
        val holder = mockk<ConversationTextMessageViewHolder>(relaxUnitFun = true)
        val message = mockk<Message>(relaxed = true)
        val contact = mockk<Contact>(relaxed = true)
        val item = mockk<CombinedMessageContact>().also {
            every { it.message } returns message
            every { it.contact } returns contact
        }
        every { messages[TEST_POSITION] } returns item

        val spyAdapterLocal = spyk(conversationAdapter, recordPrivateCalls = true)
        every { spyAdapterLocal["shouldShowAvatar"](TEST_POSITION) } returns false
        every { spyAdapterLocal.notifyDataSetChanged() } returns Unit
        every { spyAdapterLocal.getItemViewType(TEST_POSITION) } returns ConversationAdapter.VIEW_TYPE_TEXT_MESSAGE

        spyAdapterLocal.onBindViewHolder(holder, TEST_POSITION)

        verify(exactly = 1) { spyAdapterLocal.getItemViewType(TEST_POSITION) }
        verify(exactly = 1) { messages[TEST_POSITION] }
        verify(exactly = 1) { holder.bindMessage(item, false, false, false) }
    }

    @Test
    fun `given onBindViewHolder is called, when viewType is unknown, then do not bind items`() {
        val holder = mockk<ConversationTextMessageViewHolder>(relaxUnitFun = true)
        every { spyAdapter.getItemViewType(any()) } returns ConversationAdapter.VIEW_TYPE_UNKNOWN

        spyAdapter.onBindViewHolder(holder, any())

        verify(exactly = 0) { holder.bindMessage(any(), false, false, false) }
    }

    @Test
    fun `given getItemViewType is called, when item is unknown, then return VIEW_TYPE_UNKNOWN`() {
        every { messages[TEST_POSITION] } returns ""

        val result = spyAdapter.getItemViewType(TEST_POSITION)

        result shouldBeEqualTo ConversationAdapter.VIEW_TYPE_UNKNOWN
    }

    @Test
    fun `given getItemViewType is called, when item is MessageText, then return VIEW_TYPE_TEXT_MESSAGE`() {
        val combinedMessageContact = mockk<CombinedMessageContact>()
        every { messages[TEST_POSITION] } returns combinedMessageContact

        val result = spyAdapter.getItemViewType(TEST_POSITION)

        result shouldBeEqualTo ConversationAdapter.VIEW_TYPE_TEXT_MESSAGE
    }


    @Test
    fun `given getItemCount is called, then returns the size of message list`() {
        val messages = mockk<List<Any>>().also {
            every { it.size } returns TEST_LIST_SIZE
        }

        val itemCount = spyAdapter.itemCount

        itemCount shouldBeEqualTo TEST_LIST_SIZE
    }

    private companion object {
        private const val TEST_LIST_SIZE = 5
        private const val TEST_POSITION = 3
    }
}
