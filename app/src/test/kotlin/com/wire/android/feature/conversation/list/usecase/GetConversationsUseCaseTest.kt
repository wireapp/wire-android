package com.wire.android.feature.conversation.list.usecase

import androidx.paging.DataSource
import com.wire.android.UnitTest
import com.wire.android.core.exception.ServerError
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
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
    fun `given run is called, when conversationsRepository returns success, then propagate list of conversations`() {
        val conversation = mockk<Conversation>(relaxed = true)
        val conversationList = listOf(conversation, conversation)

        coEvery {
            conversationsRepository.conversationsByBatch(start = TEST_START, size = TEST_SIZE, ids = TEST_IDS)
        } returns Either.Right(conversationList)

        val result = runBlocking {
            getConversationsUseCase.run(GetConversationsParams(start = TEST_START, size = TEST_SIZE, ids = TEST_IDS))
        }

        result shouldSucceed { it shouldBe conversationList }
    }

    @Test
    fun `given run is called, when conversationsRepository returns failure, then propagate failure`() {
        coEvery {
            conversationsRepository.conversationsByBatch(start = TEST_START, size = TEST_SIZE, ids = TEST_IDS)
        } returns Either.Left(ServerError)

        val result = runBlocking {
            getConversationsUseCase.run(GetConversationsParams(start = TEST_START, size = TEST_SIZE, ids = TEST_IDS))
        }

        result shouldFail { it shouldBe ServerError }
    }

    @Test
    fun `given conversationsDataFactory is called, then returns repository's data source factory`() {
        val repoFactory = mockk<DataSource.Factory<Int, Conversation>>()
        every { conversationsRepository.conversationsDataFactory() } returns repoFactory

        val dataFactory = getConversationsUseCase.conversationsDataFactory()

        dataFactory shouldBe repoFactory
        verify(exactly = 1) { conversationsRepository.conversationsDataFactory() }
    }

    companion object {
        private const val TEST_START = "87dehhe883=jdgegge7730"
        private const val TEST_SIZE = 10
        private val TEST_IDS = emptyList<String>()
    }
}
