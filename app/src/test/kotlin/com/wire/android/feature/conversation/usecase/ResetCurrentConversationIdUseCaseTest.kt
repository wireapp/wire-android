package com.wire.android.feature.conversation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.data.ConversationRepository
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
        result.shouldSucceed {}
        coVerify(exactly = 1) { conversationRepository.restCurrentConversationId() }
    }

    @Test
    fun `given the repository fails, when resetting the current conversation id, then the failure is propagate`() = runBlocking {
        val failure = mockk<Failure>()
        coEvery { conversationRepository.restCurrentConversationId() } returns Either.Left(failure)

        val result = resetCurrentConversationIdUseCase.run(Unit)

        result.shouldFail {
            it shouldBeEqualTo failure
        }
        coVerify(exactly = 1) { conversationRepository.restCurrentConversationId() }
    }

    @Test
    fun `given any parameters, when resetting the current conversation id, then repository is used`() = runBlocking {
        resetCurrentConversationIdUseCase.run(any())

        coVerify(exactly = 1) { conversationRepository.restCurrentConversationId() }
    }
}
