package com.wire.android.feature.conversation.list.ui

import android.view.View
import android.view.ViewGroup
import com.wire.android.AndroidTest
import com.wire.android.any
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.Conversation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.*

class ConversationListAdapterTest : AndroidTest() {

    @Mock
    private lateinit var conversationList: List<Conversation>

    @Mock
    private lateinit var viewHolderInflater: ViewHolderInflater

    private lateinit var conversationListAdapter: ConversationListAdapter

    @Before
    fun setUp() {
        conversationListAdapter = ConversationListAdapter(conversationList, viewHolderInflater)
    }

    @Test
    fun `given onCreateViewHolder is called, then creates an instance of ConversationViewHolder`() {
        val parent = mock(ViewGroup::class.java)
        val itemView = mock(View::class.java)
        `when`(viewHolderInflater.inflate(anyInt(), any())).thenReturn(itemView)

        val viewHolder = conversationListAdapter.onCreateViewHolder(parent, 0)

        assertThat(viewHolder).isInstanceOf(ConversationViewHolder::class.java)
    }

    @Test
    fun `given onBindViewHolder is called, then calls holder to bind the item at the position`() {
        val holder = mock(ConversationViewHolder::class.java)
        val conversation = mock(Conversation::class.java)
        val position = 3
        `when`(conversationList[position]).thenReturn(conversation)

        conversationListAdapter.onBindViewHolder(holder, position)

        verify(conversationList)[position]
        verify(holder).bind(conversation)
    }

    @Test
    fun `given getItemCount is called, then returns the size of conversation list`() {
        val size = 5
        `when`(conversationList.size).thenReturn(size)

        val itemCount = conversationListAdapter.itemCount

        verify(conversationList).size
        assertThat(itemCount).isEqualTo(size)
    }
}
