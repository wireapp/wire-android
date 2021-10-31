package com.wire.android.feature.conversation.content.usecase

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.shared.prekey.PreKeyRepository
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test

class OutgoingMessageRecipientsRetrieverTest : UnitTest() {

    @MockK
    private lateinit var preKeyRepository: PreKeyRepository

    @MockK
    private lateinit var conversationRepository: ConversationRepository

    @MockK
    private lateinit var messageRepository: MessageRepository

    private lateinit var recipientsRetriever: OutgoingMessageRecipientsRetriever

    @Before
    fun setUp() {
        recipientsRetriever = OutgoingMessageRecipientsRetriever(preKeyRepository, conversationRepository, messageRepository)
    }

}
