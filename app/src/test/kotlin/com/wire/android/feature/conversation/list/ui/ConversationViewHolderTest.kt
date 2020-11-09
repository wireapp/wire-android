package com.wire.android.feature.conversation.list.ui

import android.view.ViewGroup
import android.widget.TextView
import com.wire.android.AndroidTest
import com.wire.android.R
import com.wire.android.core.ui.recyclerview.ViewHolderInflater
import com.wire.android.feature.conversation.list.usecase.Conversation
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ConversationViewHolderTest : AndroidTest() {

    @MockK
    private lateinit var parent: ViewGroup

    @MockK
    private lateinit var inflater: ViewHolderInflater

    @MockK
    private lateinit var itemView: TextView

    @MockK
    private lateinit var conversation: Conversation

    private lateinit var conversationViewHolder: ConversationViewHolder

    @Before
    fun setUp() {
        every { inflater.inflate(any(), any()) } returns itemView
    }

    @Test
    fun `given a ConversationViewHolder is created, then calls inflater to create an itemView from correct layout`() {
        conversationViewHolder = ConversationViewHolder(parent, inflater)

        verify(exactly = 1) { inflater.inflate(R.layout.item_conversation_list, parent) }
    }

    //TODO: not very scalable. Move to a UI test when we figure out how to mock data
    @Test
    fun `given bind is created, given conversation has name, then sets conversation name to itemView`() {
        every { conversation.name } returns TEST_NAME

        conversationViewHolder = ConversationViewHolder(parent, inflater)
        conversationViewHolder.bind(conversation)

        verify(exactly = 1) { itemView.text = TEST_NAME }
    }

    @Test
    fun `given bind is created, when conversation has no name, then sets conversation id to itemView`() {
        every { conversation.name } returns null
        every { conversation.id } returns TEST_ID

        conversationViewHolder = ConversationViewHolder(parent, inflater)
        conversationViewHolder.bind(conversation)

        verify(exactly = 1) { itemView.text = TEST_ID }
    }

    companion object {
        private const val TEST_ID = "35Y669DH0-76672389DHJ-D76"
        private const val TEST_NAME = "ConversationName"
    }
}
