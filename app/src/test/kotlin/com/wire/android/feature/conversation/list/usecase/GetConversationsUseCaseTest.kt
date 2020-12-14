package com.wire.android.feature.conversation.list.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.Conversation
import com.wire.android.feature.conversation.data.ConversationsRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
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
    fun `given run is called with params, when conversationsRepository successfully returns conversations, then propagates result`() {
        val conversations = mockk<List<Conversation>>()
        coEvery { conversationsRepository.fetchConversations(any(), any()) } returns Either.Right(conversations)

        val result = runBlocking { getConversationsUseCase.run(TEST_PARAMS) }

        result shouldSucceed { it shouldBeEqualTo conversations }
        coVerify(exactly = 1) { conversationsRepository.fetchConversations(TEST_START_ID, TEST_PAGE_SIZE) }
    }

    @Test
    fun `given run is called with params, when conversationsRepository fails to return conversations, then propagates failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationsRepository.fetchConversations(any(), any()) } returns Either.Left(failure)

        val result = runBlocking { getConversationsUseCase.run(TEST_PARAMS) }

        result shouldFail { it shouldBeEqualTo failure }
        coVerify(exactly = 1) { conversationsRepository.fetchConversations(TEST_START_ID, TEST_PAGE_SIZE) }
    }

    companion object {
        private const val TEST_START_ID = "startId"
        private const val TEST_PAGE_SIZE = 10
        private val TEST_PARAMS = GetConversationsParams(TEST_START_ID, TEST_PAGE_SIZE)
    }
}
