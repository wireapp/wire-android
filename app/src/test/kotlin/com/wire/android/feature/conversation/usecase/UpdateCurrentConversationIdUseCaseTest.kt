package com.wire.android.feature.conversation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.CacheFailure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.data.ConversationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class UpdateCurrentConversationIdUseCaseTest : UnitTest() {

    private lateinit var updateCurrentConversationIdUseCase: UpdateCurrentConversationIdUseCase

    @MockK
    private lateinit var conversationRepository: ConversationRepository

    @MockK
    private lateinit var updateCurrentConversationUseCaseParams: UpdateCurrentConversationUseCaseParams


    @Before
    fun setUp() {
        updateCurrentConversationIdUseCase = UpdateCurrentConversationIdUseCase(conversationRepository)
    }

    @Test
    fun `given run is called, when repository succeeds, then update the current conversation id`() = runBlocking {
        every { updateCurrentConversationUseCaseParams.conversationId } returns "conversation-id"
        coEvery { conversationRepository.updateCurrentConversationId(any()) } returns Either.Right(Unit)

        updateCurrentConversationIdUseCase.run(updateCurrentConversationUseCaseParams)

        verify(exactly = 1) { updateCurrentConversationUseCaseParams.conversationId }
        coVerify(exactly = 1) { conversationRepository.updateCurrentConversationId(any()) }
    }

    @Test
    fun `given run is called, when repository fails, then do not update the current conversation id`() = runBlocking {
        coEvery { conversationRepository.updateCurrentConversationId(any()) } returns Either.Left(CacheFailure)

        updateCurrentConversationIdUseCase.run(updateCurrentConversationUseCaseParams)

        coVerify(exactly = 1) { conversationRepository.updateCurrentConversationId(any()) }
    }
}
