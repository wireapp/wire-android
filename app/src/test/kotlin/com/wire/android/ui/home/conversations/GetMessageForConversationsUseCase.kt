package com.wire.android.ui.home.conversations

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.MessageMapper
import com.wire.android.model.UserStatus
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.User
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveConversationMembersUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetMessageForConversationsUseCaseTest {

    @MockK
    lateinit var getMessages: GetRecentMessagesUseCase

    @MockK
    lateinit var observeMemberDetails: ObserveConversationMembersUseCase

    @MockK
    lateinit var messageMapper: MessageMapper

    lateinit var getMessagesForConversationUseCase: GetMessagesForConversationUseCase


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        Dispatchers.setMain(StandardTestDispatcher(TestCoroutineScheduler()))

        getMessagesForConversationUseCase = GetMessagesForConversationUseCase(
            getMessages,
            observeMemberDetails,
            messageMapper,
            TestDispatcherProvider()
        )
    }

    @Test
    fun `given a member details and messages when getting the messages for the conversation then correctly propage the expected result`() = runTest {
        // Given
        val expectedUserName = "someName"
        val expectedMessageBody = "some body"

        val mockTextMessage = mockedTextMessage(expectedMessageBody)
        val mockSelfUserDetails = mockSelfUserDetails(expectedUserName)

        coEvery { getMessages(any()) } returns flowOf(listOf(mockTextMessage))
        coEvery { observeMemberDetails(any()) } returns flowOf(listOf(mockSelfUserDetails))
        coEvery { messageMapper.toUIMessages(eq(listOf(mockSelfUserDetails)), eq(listOf(mockTextMessage))) } returns listOf(
            mockUITextMessage(
                userName = mockSelfUserDetails.name!!,
                messageBody = (mockTextMessage.content as com.wire.kalium.logic.data.message.MessageContent.Text).value
            )
        )

        // When
        getMessagesForConversationUseCase(ConversationId("someValue", "someId")).collect { messages ->
            // Then
            assert(messages.size == 1)

            val onlyMessage = messages.first()

            with(onlyMessage) {
                assertEquals(expectedUserName, (messageHeader.username as UIText.DynamicString).value)
            }
        }
    }

    private fun mockedTextMessage(content: String = "Some Text Message") = Message(
        id = "messageID",
        content = com.wire.kalium.logic.data.message.MessageContent.Text(content),
        conversationId = ConversationId("someId", "someDomain"),
        date = "someDate",
        senderUserId = UserId("someValue", "someDomain"),
        senderClientId = ClientId("clientId"),
        status = Message.Status.SENT,
        editStatus = Message.EditStatus.NotEdited
    )

    private fun mockSelfUserDetails(
        name: String,
        id: UserId = UserId("self", "user")
    ): MemberDetails.Self = mockk<MemberDetails.Self>().also {
        every { it.selfUser } returns mockk<SelfUser>().also { user ->
            every { user.id } returns id
            every { user.name } returns name
            every { user.previewPicture } returns null
        }
    }

    private fun mockUITextMessage(userName: String = "mockUserName", messageBody: String): UIMessage {
        return mockk<UIMessage>().also {
            every { it.user } returns mockk<User>().also {
                every { it.avatarAsset } returns null
                every { it.availabilityStatus } returns UserStatus.AVAILABLE
            }
            every { it.messageSource } returns MessageSource.OtherUser
            every { it.messageHeader } returns mockk<MessageHeader>().also {
                every { it.messageId } returns "someId"
                every { it.username } returns UIText.DynamicString(userName)
                every { it.isLegalHold } returns false
                every { it.time } returns ""
                every { it.messageStatus } returns MessageStatus.Untouched
            }
            every { it.messageContent } returns MessageContent.TextMessage(MessageBody(UIText.DynamicString(messageBody)))
        }
    }

}
