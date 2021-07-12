package com.wire.android.feature.conversation.content.usecase

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.content.MessageRepository
import com.wire.android.feature.conversation.content.ui.CombinedMessageContact
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class GetConversationUseCaseTest : UnitTest() {

    @MockK
    private lateinit var messageRepository: MessageRepository

    private lateinit var getConversationUseCase: GetConversationUseCase

    @Before
    fun setUp() {
        getConversationUseCase = GetConversationUseCase(messageRepository)
    }

    @Test
    fun `given run is called, when messageRepository emits items, then propagates items`() {
        val items = mockk<List<CombinedMessageContact>>()
        val params = GetConversationUseCaseParams(conversationId = TEST_CONVERSATION_ID)
        coEvery { messageRepository.conversationMessages(any()) } returns flowOf(items)

        val result = runBlocking { getConversationUseCase.run(params) }

        runBlocking { result.first() shouldBeEqualTo items }
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conversation-id"
    }
}
