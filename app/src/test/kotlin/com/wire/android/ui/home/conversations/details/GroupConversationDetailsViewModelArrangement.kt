package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf

internal class GroupConversationDetailsViewModelArrangement {

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)

    private val viewModel by lazy {
        GroupConversationDetailsViewModel(savedStateHandle, navigationManager, observeConversationDetails)
    }

    init {
        // Tests setup
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns dummyConversationId
        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails): GroupConversationDetailsViewModelArrangement {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
        conversationDetailsChannel.send(conversationDetails)

        return this
    }

    fun arrange() = this to viewModel
}
