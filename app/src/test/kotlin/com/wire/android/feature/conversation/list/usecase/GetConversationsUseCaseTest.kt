package com.wire.android.feature.conversation.list.usecase

import androidx.paging.PagedList
import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class GetConversationsUseCaseTest : UnitTest() {

    private lateinit var getConversationsUseCase: GetConversationsUseCase

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @Before
    fun setup() {
        getConversationsUseCase = GetConversationsUseCase(conversationsRepository)
    }

    @Ignore("Refactor after we add ObservableUseCase and executor")
    @Test
    fun `given invoke is called with a scope and params, then calls conversationsRepo to get data of conversations`() {
        val scope = mockk<CoroutineScope>()
        val pageSize = 10
        val repoLiveData: Flow<Either<Failure, PagedList<Conversation>>> = mockk()
        coEvery { conversationsRepository.conversationsByBatch(any()) } returns repoLiveData

        getConversationsUseCase(scope, GetConversationsParams(pageSize)) {
            it shouldBeEqualTo repoLiveData
            coVerify(exactly = 1) { conversationsRepository.conversationsByBatch(any()) }
            //TODO: verify ConversationsPagingDelegate
        }
    }
}
