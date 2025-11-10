/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations.usecase

import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.MessageMapper
import com.wire.android.mapper.testOtherUser
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.ExpirationStatus
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageFooter
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.UIQuotedMessage
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.theme.Accent
import com.wire.android.util.ui.toUIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetQuoteMessageForConversationUseCaseTest {

    val dispatchers = TestDispatcherProvider()

    @Test
    fun `given valid message when calling use case then data returned`() = runTest(dispatchers.main()) {
        val (_, useCase) = Arrangement()
            .withMappingSuccess()
            .withMessageQueryResult(
                GetMessageByIdUseCase.Result.Success(
                    message = Message.Regular(
                        id = "message-id",
                        content = MessageContent.Text("Hello"),
                        conversationId = CONVERSATION_ID,
                        date = Clock.System.now(),
                        senderUserId = USER_ID,
                        status = Message.Status.Read(0),
                        isSelfMessage = true,
                        senderClientId = CLIENT_ID,
                        editStatus = Message.EditStatus.NotEdited,
                    )
                )
            )
            .arrange()

        val result = useCase(CONVERSATION_ID, QUOTED_MESSAGE_ID)

        assertEquals(
            UIQuotedMessage.UIQuotedData(
                messageId = "message_id",
                senderId = USER_ID,
                senderName = "User".toUIText(),
                senderAccent = Accent.Unknown,
                originalMessageDateDescription = "".toUIText(),
                editedTimeDescription = "".toUIText(),
                quotedContent = UIQuotedMessage.UIQuotedData.Text("Hello".toUIText())
            ),
            result
        )
    }

    @Test
    fun `given message mapping fails when calling use case then unavailable data returned`() = runTest(dispatchers.main()) {
        val (_, useCase) = Arrangement()
            .withMappingFailure()
            .withMessageQueryResult(
                GetMessageByIdUseCase.Result.Success(
                    message = Message.Regular(
                        id = "message-id",
                        content = MessageContent.Text("Hello"),
                        conversationId = CONVERSATION_ID,
                        date = Clock.System.now(),
                        senderUserId = USER_ID,
                        status = Message.Status.Read(0),
                        isSelfMessage = true,
                        senderClientId = CLIENT_ID,
                        editStatus = Message.EditStatus.NotEdited,
                    )
                )
            )
            .arrange()

        val result = useCase(CONVERSATION_ID, QUOTED_MESSAGE_ID)

        assertEquals(UIQuotedMessage.UnavailableData, result)
    }

    @Test
    fun `given message mapped to system  when calling use case then unavailable data returned`() = runTest(dispatchers.main()) {
        val (_, useCase) = Arrangement()
            .withSystemUIMessage()
            .withMessageQueryResult(
                GetMessageByIdUseCase.Result.Success(
                    message = Message.Regular(
                        id = "message-id",
                        content = MessageContent.Text("Hello"),
                        conversationId = CONVERSATION_ID,
                        date = Clock.System.now(),
                        senderUserId = USER_ID,
                        status = Message.Status.Read(0),
                        isSelfMessage = true,
                        senderClientId = CLIENT_ID,
                        editStatus = Message.EditStatus.NotEdited,
                    )
                )
            )
            .arrange()

        val result = useCase(CONVERSATION_ID, QUOTED_MESSAGE_ID)

        assertEquals(UIQuotedMessage.UnavailableData, result)
    }

    @Test
    fun `given message query returns signalling message when calling use case then unavailable data returned`() =
        runTest(dispatchers.main()) {
            val (_, useCase) = Arrangement()
                .withMessageQueryResult(
                    GetMessageByIdUseCase.Result.Success(
                        message = Message.Signaling(
                            id = "message-id",
                            content = MessageContent.Calling(
                                value = "",
                            ),
                            conversationId = CONVERSATION_ID,
                            date = Clock.System.now(),
                            senderUserId = USER_ID,
                            senderClientId = CLIENT_ID,
                            status = Message.Status.Read(0),
                            isSelfMessage = true,
                            expirationData = null,
                        )
                    )
                )
                .arrange()

            val result = useCase(CONVERSATION_ID, QUOTED_MESSAGE_ID)

            assertEquals(UIQuotedMessage.UnavailableData, result)
        }

    @Test
    fun `given message query returns system message when calling use case then unavailable data returned`() = runTest(dispatchers.main()) {
        val (_, useCase) = Arrangement()
            .withMessageQueryResult(
                GetMessageByIdUseCase.Result.Success(
                    message = Message.System(
                        id = "message-id",
                        content = MessageContent.MissedCall,
                        conversationId = CONVERSATION_ID,
                        date = Clock.System.now(),
                        senderUserId = USER_ID,
                        status = Message.Status.Read(0),
                        expirationData = null,
                    )
                )
            )
            .arrange()

        val result = useCase(CONVERSATION_ID, QUOTED_MESSAGE_ID)

        assertEquals(UIQuotedMessage.UnavailableData, result)
    }

    @Test
    fun `given message query fails when calling use case then unavailable data returned`() = runTest(dispatchers.main()) {

        val (_, useCase) = Arrangement()
            .withMessageQueryResult(GetMessageByIdUseCase.Result.Failure(CoreFailure.Unknown(null)))
            .arrange()

        val result = useCase(CONVERSATION_ID, QUOTED_MESSAGE_ID)

        assertEquals(UIQuotedMessage.UnavailableData, result)
    }

    inner class Arrangement {

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        @MockK
        lateinit var getMessageById: GetMessageByIdUseCase

        @MockK
        lateinit var getUsersForMessage: GetUsersForMessageUseCase

        @MockK
        lateinit var messageMapper: MessageMapper

        @MockK
        lateinit var messageSystem: UIMessage.System

        fun withMessageQueryResult(result: GetMessageByIdUseCase.Result) = apply {
            coEvery { getMessageById.invoke(any(), any()) } returns result
            coEvery { getUsersForMessage(any()) } returns listOf(testOtherUser(1))
        }

        fun withMappingSuccess() = apply {
            coEvery { messageMapper.toUIMessage(any(), any()) } returns UIMessage.Regular(
                conversationId = CONVERSATION_ID,
                header = MessageHeader(
                    userId = USER_ID,
                    username = "User".toUIText(),
                    membership = Membership.Standard,
                    showLegalHoldIndicator = false,
                    messageTime = MessageTime(Clock.System.now()),
                    messageStatus = MessageStatus(
                        flowStatus = MessageFlowStatus.Sent,
                        expirationStatus = ExpirationStatus.NotExpirable
                    ),
                    messageId = "message_id",
                    connectionState = null,
                    isSenderDeleted = false,
                    isSenderUnavailable = false,
                ),
                source = MessageSource.OtherUser,
                userAvatarData = UserAvatarData(),
                messageContent = UIMessageContent.TextMessage(MessageBody("Hello".toUIText())),
                messageFooter = MessageFooter("message_id")
            )
        }

        fun withMappingFailure() = apply {
            coEvery { messageMapper.toUIMessage(any(), any()) } returns null
        }

        fun withSystemUIMessage() = apply {
            coEvery { messageMapper.toUIMessage(any(), any()) } returns messageSystem
        }

        fun arrange() = this to GetQuoteMessageForConversationUseCase(
            getMessageById = getMessageById,
            getUsersForMessage = getUsersForMessage,
            messageMapper = messageMapper,
            dispatchers = dispatchers
        )
    }

    private companion object {
        val CONVERSATION_ID = ConversationId("conversation-id", "domain")
        val USER_ID = QualifiedID("user-id", "domain")
        val CLIENT_ID = PlainId("client-id")
        const val QUOTED_MESSAGE_ID = "quoted-message-id"
    }
}
