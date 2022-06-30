package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import com.wire.android.mapper.UIParticipantMapper
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.model.UIParticipant
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
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

    @MockK
    lateinit var observeConversationMembersUseCase: ObserveConversationMembersUseCase

    @MockK
    private lateinit var wireSessionImageLoader: WireSessionImageLoader

    val uIParticipantMapper: UIParticipantMapper = UIParticipantMapper(UserTypeMapper(), wireSessionImageLoader)

    private val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)
    private val conversationMembersChannel = Channel<List<MemberDetails>>(capacity = Channel.UNLIMITED)

    private val viewModel by lazy {
        GroupConversationDetailsViewModel(
            savedStateHandle,
            navigationManager,
            observeConversationDetails,
            observeConversationMembersUseCase,
            uIParticipantMapper
        )
    }

    init {
        // Tests setup
        val dummyConversationId = "some-dummy-value@some.dummy.domain"
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns dummyConversationId
        // Default empty values
        coEvery { observeConversationDetails(any()) } returns flowOf()
        coEvery { observeConversationMembersUseCase(any()) } returns flowOf()
    }

    suspend fun withConversationDetailUpdate(conversationDetails: ConversationDetails): GroupConversationDetailsViewModelArrangement {
        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()
        conversationDetailsChannel.send(conversationDetails)
        return this
    }

    suspend fun withConversationParticipantsUpdate(members: List<MemberDetails>): GroupConversationDetailsViewModelArrangement {
        coEvery { observeConversationMembersUseCase(any()) } returns conversationMembersChannel.consumeAsFlow()
        conversationMembersChannel.send(members)
        return this
    }

    fun arrange() = this to viewModel
}
