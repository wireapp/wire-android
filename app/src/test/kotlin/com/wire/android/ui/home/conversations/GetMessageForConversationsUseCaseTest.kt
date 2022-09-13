package com.wire.android.ui.home.conversations

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.MessageMapper
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.usecase.GetMessagesForConversationUseCase
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import com.wire.kalium.logic.feature.message.GetRecentMessagesUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
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
    private lateinit var getMessages: GetRecentMessagesUseCase

    @MockK
    private lateinit var observeUserListByIdUseCase: ObserveUserListByIdUseCase

    @MockK
    private lateinit var messageMapper: MessageMapper

    private lateinit var getMessagesForConversationUseCase: GetMessagesForConversationUseCase

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)

        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

        Dispatchers.setMain(dispatcher)

        getMessagesForConversationUseCase = GetMessagesForConversationUseCase(
            getMessages,
            observeUserListByIdUseCase,
            messageMapper,
            TestDispatcherProvider(dispatcher)
        )
    }

    @Test
    fun `given a member details and messages when getting the messages for the conversation then correctly propage the expected result`() =
        runTest {
            // Given
            val expectedUserName = "someName"
            val expectedMessageBody = "some body"

            val mockTextMessage = mockedTextMessage(expectedMessageBody)
            val mockSelfUserDetails = mockSelfUserDetails(expectedUserName).user

            coEvery { getMessages(any()) } returns flowOf(listOf(mockTextMessage))
            coEvery { observeUserListByIdUseCase(any()) } returns flowOf(listOf(mockSelfUserDetails))
            coEvery { messageMapper.toUIMessages(any(), any()) } returns listOf(
                mockUITextMessage(
                    userName = mockSelfUserDetails.name!!,
                    messageBody = (mockTextMessage.content as com.wire.kalium.logic.data.message.MessageContent.Text).value
                )
            )
            every { messageMapper.memberIdList(any()) } returns listOf(mockSelfUserDetails.id)

            // When
            getMessagesForConversationUseCase(ConversationId("someValue", "someId")).collect { messages ->
                // Then
                assert(messages.size == 1)

                val onlyMessage = messages.first()

                with(onlyMessage) {
                    assertEquals(expectedUserName, (messageHeader.username as UIText.DynamicString).value)

                    val messageBody = (messageContent as UIMessageContent.TextMessage).messageBody
                    assertEquals(expectedMessageBody, ((messageBody.message as UIText.DynamicString).value))
                }
            }

            coVerify(exactly = 1) { messageMapper.toUIMessages(listOf(mockSelfUserDetails), listOf(mockTextMessage)) }
        }

    private fun mockedTextMessage(content: String = "Some Text Message") = Message.Regular(
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
    ): MemberDetails = mockk<MemberDetails>().also {
        every { it.user } returns mockk<SelfUser>().also { user ->
            every { user.id } returns id
            every { user.name } returns name
            every { user.previewPicture } returns null
        }
    }

    private fun mockUITextMessage(userName: String = "mockUserName", messageBody: String): UIMessage {
        return mockk<UIMessage>().also {
            every { it.userAvatarData } returns UserAvatarData()
            every { it.messageSource } returns MessageSource.OtherUser
            every { it.messageHeader } returns mockk<MessageHeader>().also {
                every { it.messageId } returns "someId"
                every { it.username } returns UIText.DynamicString(userName)
                every { it.isLegalHold } returns false
                every { it.messageTime } returns MessageTime("")
                every { it.messageStatus } returns MessageStatus.Untouched
            }
            every { it.messageContent } returns UIMessageContent.TextMessage(MessageBody(UIText.DynamicString(messageBody)))
        }
    }

}
