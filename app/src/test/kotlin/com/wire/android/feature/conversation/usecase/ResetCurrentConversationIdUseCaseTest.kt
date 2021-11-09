package com.wire.android.feature.conversation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.EmptyCacheFailure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.data.ConversationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any

class ResetCurrentConversationIdUseCaseTest : UnitTest() {

    private lateinit var resetCurrentConversationIdUseCase: ResetCurrentConversationIdUseCase

    @MockK
    private lateinit var conversationRepository: ConversationRepository

    @Before
    fun setUp() {
        resetCurrentConversationIdUseCase = ResetCurrentConversationIdUseCase(conversationRepository)
    }

    @Test
    fun `given the repository succeeds when resetting the current conversation id, then the success is propagated`() = runBlocking {
        coEvery { conversationRepository.restCurrentConversationId() } returns Either.Right(Unit)

        val result = resetCurrentConversationIdUseCase.run(Unit)
        assert(true) {result == Either.Right(Unit)}

        coVerify(exactly = 1) { conversationRepository.restCurrentConversationId() }
    }

    @Test
    fun `given the repository fails, when resetting the current conversation id, then the failure is propagate`() = runBlocking {
        coEvery { conversationRepository.restCurrentConversationId() } returns Either.Left(EmptyCacheFailure)

        val result = resetCurrentConversationIdUseCase.run(Unit)
        assert(true) {result == Either.Left(EmptyCacheFailure)}

        coVerify(exactly = 1) { conversationRepository.restCurrentConversationId() }
    }

    @Test
    fun `given any parameters, when resetting the current conversation id, then repository is used`() = runBlocking {
        resetCurrentConversationIdUseCase.run(any())

        coVerify(exactly = 1) { conversationRepository.restCurrentConversationId() }
    }
}
