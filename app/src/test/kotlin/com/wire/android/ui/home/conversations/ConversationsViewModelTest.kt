package com.wire.android.ui.home.conversations

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.message.DeleteMessageUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import com.wire.kalium.logic.feature.message.SendTextMessageUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ConversationsViewModelTest {
    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var navigationManager: NavigationManager

    @MockK
    lateinit var getMessages: GetRecentMessagesUseCase

    @MockK
    lateinit var sendTextMessage: SendTextMessageUseCase

    @MockK
    lateinit var sendImageMessage: SendImageMessageUseCase

    @MockK
    lateinit var deleteMessage: DeleteMessageUseCase

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.getLiveData<String>(any()) } returns MutableLiveData("")
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        // Default empty values
        coEvery { getMessages(any()) } returns flowOf(listOf())
        coEvery { observeConversationDetails(any()) } returns flowOf()
    }

    private fun createTestSubject() = ConversationViewModel(
        savedStateHandle = savedStateHandle,
        navigationManager = navigationManager,
        getMessages = getMessages,
        observeConversationDetails = observeConversationDetails,
        sendTextMessage = sendTextMessage,
        sendImageMessage = sendImageMessage,
        deleteMessage = deleteMessage
    )

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible`() {
        val conversationsViewModel = createTestSubject()
        conversationsViewModel.showDeleteMessageDialog("")
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Visible("", conversationsViewModel.conversationId!!)
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageForYourselfDialog is visible`() {
        val conversationsViewModel = createTestSubject()
        conversationsViewModel.showDeleteMessageForYourselfDialog("")
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Visible("", conversationsViewModel.conversationId!!),
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when dialogs are dismissed`() {
        val conversationsViewModel = createTestSubject()
        conversationsViewModel.onDialogDismissed()
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then the name of the other user is used`() = runTest {
        val conversationDetails = CONVERSATION_DETAILS_ONE_ON_ONE
        val otherUserName = conversationDetails.otherUser.name
        coEvery { observeConversationDetails(any()) } returns flowOf(conversationDetails)

        val conversationsViewModel = createTestSubject()

        assertEquals(otherUserName, conversationsViewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given a group conversation, when solving the conversation name, then the name of the conversation is used`() = runTest {
        val conversationDetails = testConversationDetailsGroup("Conversation Name Goes Here")
        val conversationName = conversationDetails.conversation.name
        coEvery { observeConversationDetails(any()) } returns flowOf(conversationDetails)

        val conversationsViewModel = createTestSubject()

        assertEquals(conversationName, conversationsViewModel.conversationViewState.conversationName)
    }

    @Test
    fun `given the conversation name is updated, when solving the conversation name, then the state is updated accordingly`() = runTest {
        val firstConversationDetails = testConversationDetailsGroup("Conversation Name Goes Here")
        val secondConversationDetails = testConversationDetailsGroup("Conversation Name Was Updated")
        val conversationDetailsChannel = Channel<ConversationDetails>(capacity = Channel.UNLIMITED)
        firstConversationDetails.conversation.name

        coEvery { observeConversationDetails(any()) } returns conversationDetailsChannel.consumeAsFlow()

        val conversationsViewModel = createTestSubject()

        conversationDetailsChannel.send(firstConversationDetails)
        assertEquals(firstConversationDetails.conversation.name, conversationsViewModel.conversationViewState.conversationName)

        conversationDetailsChannel.send(secondConversationDetails)
        assertEquals(secondConversationDetails.conversation.name, conversationsViewModel.conversationViewState.conversationName)
    }

    private companion object {
        val CONVERSATION_DETAILS_ONE_ON_ONE = ConversationDetails.OneOne(
            mockk(),
            mockk<OtherUser>().apply {
                every { name } returns "Other User Name Goes Here"
            },
            ConversationDetails.OneOne.ConnectionState.OUTGOING,
            LegalHoldStatus.DISABLED
        )

        fun testConversationDetailsGroup(conversationName: String) = ConversationDetails.Group(
            mockk<Conversation>().apply {
                every { name } returns conversationName
            }
        )
    }
}
