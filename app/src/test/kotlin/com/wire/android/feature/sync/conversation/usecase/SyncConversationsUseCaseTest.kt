package com.wire.android.feature.sync.conversation.usecase

import com.wire.android.UnitTest
import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.feature.conversation.data.ConversationRepository
import com.wire.android.framework.functional.shouldFail
import com.wire.android.framework.functional.shouldSucceed
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class SyncConversationsUseCaseTest : UnitTest() {

    @MockK
    private lateinit var conversationRepository: ConversationRepository

    private lateinit var syncConversationsUseCase: SyncConversationsUseCase

    @Before
    fun setUp() {
        syncConversationsUseCase = SyncConversationsUseCase(conversationRepository)
    }

    @Test
    fun `given run is called, when conversationRepo fails to fetch conversations, then propagates the failure`() {
        val failure = mockk<Failure>()
        coEvery { conversationRepository.fetchConversations() } returns Either.Left(failure)

        val result = runBlocking { syncConversationsUseCase.run(Unit) }

        result shouldFail { it shouldBeEqualTo failure }
    }

    @Test
    fun `given run is called, when conversationRepo fetches conversations, then propagates success`() {
        coEvery { conversationRepository.fetchConversations() } returns Either.Right(Unit)

        val result = runBlocking { syncConversationsUseCase.run(Unit) }

        result shouldSucceed { it shouldBe Unit }
    }
}
