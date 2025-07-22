/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.mapper.MessageMapper
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GetConversationMessagesFromSearchUseCaseTest {

    @Test
    fun `given below minimum characters to search, when searching, then return an empty list`() =
        runTest {
            // given
            val (arrangement, useCase) = Arrangement()
                .arrange()

            // when
            val result = useCase("", arrangement.conversationId, 100).asSnapshot()
            advanceUntilIdle()

            // then
            assert(result.isEmpty())
        }

    @Test
    fun `given search term, when searching messages, then return messages list`() = runTest {
        // given
        val (arrangement, useCase) = Arrangement()
            .withSearchSuccess()
            .withMemberIdList()
            .withMemberDetails()
            .withMappedMessage(
                user = Arrangement.user1,
                message = Arrangement.message1
            )
            .withMappedMessage(
                user = Arrangement.user2,
                message = Arrangement.message2
            )
            .arrange()

        // when
        val result = useCase(arrangement.searchTerm, arrangement.conversationId, 100).asSnapshot()
        advanceUntilIdle()

        // then
        assertEquals(
            Arrangement.messages.size,
            result.size
        )
    }

    class Arrangement {
        val searchTerm = "message"
        val conversationId = ConversationId(
            value = "some-dummy-value",
            domain = "some-dummy-domain"
        )

        @MockK
        lateinit var getMessagesSearch: GetPaginatedFlowOfMessagesBySearchQueryAndConversationIdUseCase

        @MockK
        lateinit var getUsersForMessages: GetUsersForMessageUseCase

        @MockK
        lateinit var messageMapper: MessageMapper

        private val useCase: GetConversationMessagesFromSearchUseCase by lazy {
            GetConversationMessagesFromSearchUseCase(
                getMessagesSearch,
                getUsersForMessages,
                messageMapper,
                dispatchers = TestDispatcherProvider(),
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        suspend fun withSearchSuccess() = apply {
            coEvery {
                getMessagesSearch(searchTerm, conversationId, any(), any())
            } returns flowOf(
                PagingData.from(
                    messages
                )
            )
        }

        fun withMemberIdList() = apply {
            every { messageMapper.memberIdList(messages) } returns listOf(
                message1.senderUserId,
                message2.senderUserId
            )
            every { messageMapper.memberIdList(listOf(message1)) } returns listOf(
                message1.senderUserId,
            )

            every { messageMapper.memberIdList(listOf(message2)) } returns listOf(
                message2.senderUserId,
            )
        }

        suspend fun withMemberDetails() = apply {
            coEvery { getUsersForMessages(any()) } returns listOf(user1, user2)
        }

        fun withMappedMessage(user: User, message: Message.Standalone) = apply {
            every { messageMapper.toUIMessage(any(), message) } returns UIMessage.Regular(
                conversationId = conversationId,
                userAvatarData = UserAvatarData(
                    asset = null,
                    availabilityStatus = UserAvailabilityStatus.NONE
                ),
                source = MessageSource.OtherUser,
                header = TestMessage.UI_MESSAGE_HEADER.copy(
                    messageId = UUID.randomUUID().toString(),
                    userId = user.id
                ),
                messageContent = UIMessageContent.TextMessage(
                    MessageBody(
                        UIText.DynamicString(
                            (message.content as MessageContent.Text).value
                        )
                    )
                ),
                messageFooter = com.wire.android.ui.home.conversations.model.MessageFooter(
                    TestMessage.UI_MESSAGE_HEADER.messageId
                )
            )
        }

        fun arrange() = this to useCase

        companion object {
            val user1 = TestUser.OTHER_USER.copy(
                id = UserId("user-id1", "domain")
            )
            val user2 = TestUser.OTHER_USER.copy(
                id = UserId("user-id2", "domain")
            )
            val users = listOf(user1, user2)

            val message1 = TestMessage.TEXT_MESSAGE.copy(
                content = MessageContent.Text("message1"),
                senderUserId = user1.id
            )
            val message2 = TestMessage.TEXT_MESSAGE.copy(
                content = MessageContent.Text("message2"),
                senderUserId = user2.id
            )
            val messages = listOf(message1, message2)
            val conversationId = ConversationId("value", "domain")
        }
    }
}
