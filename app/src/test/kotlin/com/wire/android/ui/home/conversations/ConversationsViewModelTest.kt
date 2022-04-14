package com.wire.android.ui.home.conversations

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.navigation.NavigationManager
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.LegalHoldStatus
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.SendImageMessageUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
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
    lateinit var getMessageAsset: GetMessageAssetUseCase

    @MockK
    lateinit var deleteMessage: DeleteMessageUseCase

    @MockK
    lateinit var observeConversationDetails: ObserveConversationDetailsUseCase

    @MockK
    lateinit var observeMemberDetails: ObserveConversationMembersUseCase

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        every { savedStateHandle.get<String>(any()) } returns ("")
        every { savedStateHandle.set(any(), any<String>()) } returns Unit

        // Default empty values
        coEvery { getMessages(any()) } returns flowOf(listOf())
        coEvery { observeMemberDetails(any()) } returns flowOf(listOf())
        coEvery { observeConversationDetails(any()) } returns flowOf()
    }

    private fun createTestSubject() = ConversationViewModel(
        savedStateHandle = savedStateHandle,
        navigationManager = navigationManager,
        getMessages = getMessages,
        observeConversationDetails = observeConversationDetails,
        observeMemberDetails = observeMemberDetails,
        sendTextMessage = sendTextMessage,
        sendImageMessage = sendImageMessage,
        getMessageAsset = getMessageAsset,
        deleteMessage = deleteMessage
    )

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for my message`() {
        val conversationsViewModel = createTestSubject()
        conversationsViewModel.showDeleteMessageDialog("", true)
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Hidden,
            forEveryone = DeleteMessageDialogActiveState.Visible("", conversationsViewModel.conversationId!!)
        )
    }

    @Test
    fun `validate deleteMessageDialogsState states when deleteMessageDialog is visible for others message`() {
        val conversationsViewModel = createTestSubject()
        conversationsViewModel.showDeleteMessageDialog("", false)
        conversationsViewModel.deleteMessageDialogsState shouldBeEqualTo DeleteMessageDialogsState.States(
            forYourself = DeleteMessageDialogActiveState.Visible("", conversationsViewModel.conversationId!!),
            forEveryone = DeleteMessageDialogActiveState.Hidden
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
            forYourself = DeleteMessageDialogActiveState.Hidden, forEveryone = DeleteMessageDialogActiveState.Hidden
        )
    }

    @Test
    fun `given a 1 on 1 conversation, when solving the conversation name, then the name of the other user is used`() = runTest {
        val conversationDetails = testConversationDetailsOneOnOne("Other User Name Goes Here")
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

    @Test
    fun `given message sent by self user, when solving the message header, then the state should contain the self user name`() = runTest {
        val senderId = UserId("value", "domain")
        val messages = listOf(testMessage(senderId = senderId))
        val selfUserName = "self user"
        val selfMember = testSelfUserDetails(selfUserName, senderId)

        coEvery { getMessages(any()) } returns flowOf(messages)
        coEvery { observeMemberDetails(any()) } returns flowOf(listOf(selfMember))

        val conversationsViewModel = createTestSubject()

        assertEquals(selfUserName, conversationsViewModel.conversationViewState.messages.first().messageHeader.username)
    }

    @Test
    fun `given message sent by another user, when solving the message header, then the state should contain that user name`() = runTest {
        val senderId = UserId("value", "domain")
        val messages = listOf(testMessage(senderId = senderId))
        val otherUserName = "other user"
        val otherMember = testOtherUserDetails(otherUserName, senderId)

        coEvery { getMessages(any()) } returns flowOf(messages)
        coEvery { observeMemberDetails(any()) } returns flowOf(listOf(otherMember))

        val conversationsViewModel = createTestSubject()

        assertEquals(otherUserName, conversationsViewModel.conversationViewState.messages.first().messageHeader.username)
    }

    @Test
    fun `given the sender is updated, when solving the message header, then the update is propagated in the state`() = runTest {
        val senderId = UserId("value", "domain")
        val messages = listOf(testMessage(senderId = senderId))
        val firstUserName = "other user"
        val secondUserName = "User changed their name"
        val otherMemberUpdatesChannel = Channel<List<MemberDetails>>(capacity = Channel.UNLIMITED)

        coEvery { getMessages(any()) } returns flowOf(messages)
        coEvery { observeMemberDetails(any()) } returns otherMemberUpdatesChannel.consumeAsFlow()

        val conversationsViewModel = createTestSubject()

        otherMemberUpdatesChannel.send(listOf(testOtherUserDetails(firstUserName, senderId)))
        assertEquals(firstUserName, conversationsViewModel.conversationViewState.messages.first().messageHeader.username)

        otherMemberUpdatesChannel.send(listOf(testOtherUserDetails(secondUserName, senderId)))
        assertEquals(secondUserName, conversationsViewModel.conversationViewState.messages.first().messageHeader.username)
    }

    private companion object {
        fun testConversationDetailsOneOnOne(senderName: String) = ConversationDetails.OneOne(
            mockk(), mockk<OtherUser>().apply {
                every { name } returns senderName
            }, ConversationDetails.OneOne.ConnectionState.OUTGOING, LegalHoldStatus.DISABLED
        )

        fun testConversationDetailsGroup(conversationName: String) = ConversationDetails.Group(mockk<Conversation>().apply {
            every { name } returns conversationName
        })

        fun testMessage(
            senderId: UserId,
            id: String = "messageID",
            content: MessageContent = MessageContent.Text(""),
            date: String = "date",
        ): Message = mockk<Message>().also {
            every { it.senderUserId } returns senderId
            every { it.id } returns id
            every { it.content } returns content
            every { it.date } returns date
        }

        fun testSelfUserDetails(
            name: String,
            id: UserId = UserId("self", "user")
        ): MemberDetails.Self = mockk<MemberDetails.Self>().also {
            every { it.selfUser } returns mockk<SelfUser>().also { user ->
                every { user.id } returns id
                every { user.name } returns name
                every { user.previewPicture } returns null
            }
        }

        fun testOtherUserDetails(
            name: String,
            id: UserId = UserId("other", "user")
        ): MemberDetails.Other = mockk<MemberDetails.Other>().also {
            every { it.otherUser } returns mockk<OtherUser>().also { user ->
                every { user.id } returns id
                every { user.name } returns name
                every { user.previewPicture } returns null
            }
        }
    }
}
