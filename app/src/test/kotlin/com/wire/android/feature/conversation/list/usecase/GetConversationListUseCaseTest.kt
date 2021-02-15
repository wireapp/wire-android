package com.wire.android.feature.conversation.list.usecase

import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.feature.conversation.list.ConversationListRepository
import com.wire.android.feature.conversation.list.ui.ConversationListItem
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class GetConversationListUseCaseTest : UnitTest() {

    @MockK
    private lateinit var conversationListRepository: ConversationListRepository

    private lateinit var getConversationListUseCase: GetConversationListUseCase

    @Before
    fun setUp() {
        getConversationListUseCase = GetConversationListUseCase(conversationListRepository)
    }

    @Test
    fun `given run is called, when conversationListRepo emits items, then propagates items`() {
        val items = mockk<PagedList<ConversationListItem>>()
        coEvery { conversationListRepository.conversationListInBatch(any()) } returns flowOf(items)

        val params = GetConversationListUseCaseParams(pageSize = TEST_PAGE_SIZE)

        val result = runBlocking { getConversationListUseCase.run(params) }

        runBlocking { result.first() shouldBeEqualTo items }
    }

    companion object {
        private const val TEST_PAGE_SIZE = 10
    }
}
