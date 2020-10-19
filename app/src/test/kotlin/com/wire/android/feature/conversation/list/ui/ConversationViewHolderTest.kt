package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import android.widget.TextView
import com.wire.android.AndroidTest
import com.wire.android.R
import com.wire.android.any
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.Conversation
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

class ConversationViewHolderTest : AndroidTest() {

    @Mock
    private lateinit var parent: ViewGroup

    @Mock
    private lateinit var inflater: ViewHolderInflater

    @Mock
    private lateinit var itemView: TextView

    private lateinit var conversationViewHolder: ConversationViewHolder

    @Before
    fun setUp() {
        `when`(inflater.inflate(anyInt(), any())).thenReturn(itemView)
    }

    @Test
    fun `given a ConversationViewHolder is created, then calls inflater to create an itemView from correct layout`() {
        conversationViewHolder = ConversationViewHolder(parent, inflater)

        verify(inflater).inflate(R.layout.item_conversation_list, parent)
    }

    //TODO: not very scalable. Move to a UI test when we figure out how to mock data
    @Test
    fun `given bind is created, then sets conversation data to itemView`() {
        conversationViewHolder = ConversationViewHolder(parent, inflater)

        conversationViewHolder.bind(TEST_CONVERSATION)

        verify(itemView).text = TEST_CONVERSATION.name
    }

    companion object {
        private val TEST_CONVERSATION = Conversation("Android Team")
    }
}
