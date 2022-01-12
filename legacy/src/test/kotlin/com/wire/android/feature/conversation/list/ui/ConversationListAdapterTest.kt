package com.wire.android.feature.conversation.list.ui

import android.view.View
import android.view.ViewGroup
import com.wire.android.UnitTest
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore("WIP")
class ConversationListAdapterTest : UnitTest() {

    @MockK
    private lateinit var conversationListItems: List<ConversationListItem>

    @MockK
    private lateinit var viewHolderInflater: ViewHolderInflater

    @MockK
    private lateinit var diffCallback: ConversationListDiffCallback

    @MockK
    private lateinit var clickListener : (conversationListItem: ConversationListItem?) -> Unit

    private lateinit var conversationListAdapter: ConversationListAdapter

    @Before
    fun setUp() {
        conversationListAdapter = ConversationListAdapter(viewHolderInflater, diffCallback, mockk(), clickListener)
//        conversationListAdapter.updateData(conversationList)
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
        val item = mockk<ConversationListItem>()
        val position = 3
        every { conversationListItems[position] } returns item

        conversationListAdapter.onBindViewHolder(holder, position)

        verify(exactly = 1) { conversationListItems[position] }
        verify(exactly = 1) { holder.bind(item, clickListener) }
    }

    @Test
    fun `given getItemCount is called, then returns the size of conversation list`() {
        val size = 5
        every { conversationListItems.size } returns size

        val itemCount = conversationListAdapter.itemCount

        verify(exactly = 1) { conversationListItems.size }
        itemCount shouldBeEqualTo size
    }
}
