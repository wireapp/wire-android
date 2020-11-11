package com.wire.android.feature.conversation.list.ui

import android.view.View
import android.view.ViewGroup
import com.wire.android.UnitTest
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.Conversation
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Test

class ConversationListAdapterTest : UnitTest() {

    @MockK
    private lateinit var conversationList: List<Conversation>

    @MockK
    private lateinit var viewHolderInflater: ViewHolderInflater

    private lateinit var conversationListAdapter: ConversationListAdapter

    @Before
    fun setUp() {
        conversationListAdapter = ConversationListAdapter(viewHolderInflater)
        conversationListAdapter.updateData(conversationList)
    }

    @Test
    fun `given onCreateViewHolder is called, then creates an instance of ConversationViewHolder`() {
        val parent = mockk<ViewGroup>()
        val itemView = mockk<View>(relaxed = true)
        every { viewHolderInflater.inflate(any(), any()) } returns itemView

        val viewHolder = conversationListAdapter.onCreateViewHolder(parent, 0)

        viewHolder shouldBeInstanceOf ConversationViewHolder::class.java
    }

    @Test
    fun `given onBindViewHolder is called, then calls holder to bind the item at the position`() {
        val holder = mockk<ConversationViewHolder>(relaxUnitFun = true)
        val conversation = mockk<Conversation>()
        val position = 3
        every { conversationList[position] } returns conversation

        conversationListAdapter.onBindViewHolder(holder, position)

        verify(exactly = 1) { conversationList[position] }
        verify(exactly = 1) { holder.bind(conversation) }
    }

    @Test
    fun `given getItemCount is called, then returns the size of conversation list`() {
        val size = 5
        every { conversationList.size } returns size

        val itemCount = conversationListAdapter.itemCount

        verify(exactly = 1) { conversationList.size }
        itemCount shouldBeEqualTo size
    }
}
