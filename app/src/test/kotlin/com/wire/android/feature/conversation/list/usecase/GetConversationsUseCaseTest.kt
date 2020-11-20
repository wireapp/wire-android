package com.wire.android.feature.conversation.list.usecase

import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsPagingDelegate
import com.wire.android.feature.conversation.data.ConversationsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class GetConversationsUseCaseTest : UnitTest() {

    private lateinit var getConversationsUseCase: GetConversationsUseCase

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @Before
    fun setup() {
        getConversationsUseCase = GetConversationsUseCase(conversationsRepository)
    }

    @Test
    fun `given run is called with params, then calls conversationsRepo to get data of conversations`() {
        val pagingDelegate = mockk<ConversationsPagingDelegate>()
        val params = mockk<GetConversationsParams>()
        every { params.pagingDelegate } returns pagingDelegate
        val repoResult: Flow<Either<Failure, PagedList<Conversation>>> = mockk()
        coEvery { conversationsRepository.conversationsByBatch(any()) } returns repoResult

        val result = runBlocking { getConversationsUseCase.run(params) }

        result shouldBeEqualTo repoResult
        coVerify(exactly = 1) { conversationsRepository.conversationsByBatch(pagingDelegate) }
    }
}
