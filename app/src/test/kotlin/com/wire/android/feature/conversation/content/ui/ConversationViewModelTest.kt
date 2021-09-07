package com.wire.android.feature.conversation.content.ui

import com.wire.android.UnitTest
import com.wire.android.feature.conversation.content.usecase.GetConversationUseCase
import com.wire.android.feature.conversation.usecase.UpdateCurrentConversationIdUseCase
import com.wire.android.framework.coroutines.CoroutinesTestRule
import com.wire.android.framework.livedata.shouldBeUpdated
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConversationViewModelTest : UnitTest() {

    @get:Rule
    val coroutinesTestRule = CoroutinesTestRule()

    @MockK
    private lateinit var getConversationUseCase: GetConversationUseCase

    @MockK
    private lateinit var updateCurrentConversationIdUseCase: UpdateCurrentConversationIdUseCase

    private lateinit var conversationViewModel: ConversationViewModel

    @Before
    fun setup() {
        conversationViewModel = ConversationViewModel(
            coroutinesTestRule.dispatcherProvider,
            getConversationUseCase,
            updateCurrentConversationIdUseCase
        )
    }

    @Test
    fun `given cacheConversationId is called, when conversationId is valid, then updates conversationIdLiveData`() {
        conversationViewModel.cacheConversationId(TEST_CONVERSATION_ID)

        conversationViewModel.conversationIdLiveData.shouldBeUpdated {
            it shouldBeEqualTo TEST_CONVERSATION_ID
        }
    }

    @Test
    fun `given fetchMessages is called, when getConversationUseCase emits items, then updates conversationMessagesLiveData`() {
        val items = mockk<List<CombinedMessageContact>>()
        coEvery { getConversationUseCase.run(any()) } returns flowOf(items)

        conversationViewModel.fetchMessages(TEST_CONVERSATION_ID)

        conversationViewModel.conversationMessagesLiveData.shouldBeUpdated { it shouldBeEqualTo items }
        coVerify(exactly = 1) { getConversationUseCase.run(any()) }
    }

    @Test
    fun `given conversationId is valid, when updating current conversationId, then updates run usecase`() {
        conversationViewModel.updateCurrentConversationId(TEST_CONVERSATION_ID)

        coVerify(exactly = 1) { updateCurrentConversationIdUseCase.run(any()) }
    }

    companion object {
        private const val TEST_CONVERSATION_ID = "conversation-id"
    }
}
