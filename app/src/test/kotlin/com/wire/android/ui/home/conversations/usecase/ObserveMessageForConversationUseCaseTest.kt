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

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestMessage
import com.wire.android.framework.TestUser
import com.wire.android.mapper.MessageMapper
import com.wire.android.ui.home.conversations.mock.mockHeader
import com.wire.android.ui.home.conversations.mock.mockMessageWithText
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.UIMessage
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.StorageFailure
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase
import com.wire.kalium.logic.feature.message.ObserveMessageByIdUseCase.Result
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ObserveMessageForConversationUseCaseTest {

    val dispatchers = TestDispatcherProvider()

    @Test
    fun `given message exists, when observing, then return UIMessage`() = runTest(dispatchers.main()) {
        val message = TEXT_MESSAGE()
        val uiMessage = UI_TEXT_MESSAGE()
        val (_, useCase) = Arrangement()
            .withObserveMessageById(flowOf(Result.Success(message)))
            .withGetUsersForMessage(listOf(SELF_USER))
            .withMappedMessage(message, uiMessage)
            .arrange()

        useCase(message.conversationId, message.id).test {
            awaitItem().let {
                assertNotNull(it)
                assertEquals(uiMessage.header.messageId, it!!.header.messageId)
            }
            awaitComplete()
        }
    }

    @Test
    fun `given message does not exist, when observing, then return null`() = runTest(dispatchers.main()) {
        val message = TEXT_MESSAGE()
        val (_, useCase) = Arrangement()
            .withObserveMessageById(flowOf(Result.Failure(StorageFailure.DataNotFound)))
            .arrange()

        useCase(message.conversationId, message.id).test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `given message updated, when observing, then return updated UIMessage`() = runTest(dispatchers.main()) {
        val message = TEXT_MESSAGE(status = Message.Status.Sent)
        val uiMessage = UI_TEXT_MESSAGE(status = MessageFlowStatus.Sent)
        val updatedMessage = TEXT_MESSAGE(status = Message.Status.Delivered)
        val updatedUiMessage = UI_TEXT_MESSAGE(status = MessageFlowStatus.Delivered)
        val (_, useCase) = Arrangement()
            .withObserveMessageById(flowOf(Result.Success(message), Result.Success(updatedMessage)))
            .withGetUsersForMessage(listOf(SELF_USER))
            .withMappedMessage(message, uiMessage)
            .withMappedMessage(updatedMessage, updatedUiMessage)
            .arrange()

        useCase(message.conversationId, message.id).test {
            awaitItem().let {
                assertNotNull(it)
                assertEquals(uiMessage.header.messageId, it!!.header.messageId)
                assertEquals(uiMessage.header.messageStatus.flowStatus, it.header.messageStatus.flowStatus)
            }
            awaitItem().let {
                assertNotNull(it)
                assertEquals(updatedUiMessage.header.messageId, it!!.header.messageId)
                assertEquals(updatedUiMessage.header.messageStatus.flowStatus, it.header.messageStatus.flowStatus)
            }
            awaitComplete()
        }
    }

    @Test
    fun `given message removed, when observing, then return null`() = runTest(dispatchers.main()) {
        val message = TEXT_MESSAGE()
        val uiMessage = UI_TEXT_MESSAGE()
        val (_, useCase) = Arrangement()
            .withObserveMessageById(flowOf(Result.Success(message), Result.Failure(StorageFailure.DataNotFound)))
            .withGetUsersForMessage(listOf(SELF_USER))
            .withMappedMessage(message, uiMessage)
            .arrange()

        useCase(message.conversationId, message.id).test {
            awaitItem().let {
                assertNotNull(it)
                assertEquals(uiMessage.header.messageId, it!!.header.messageId)
                assertEquals(uiMessage.header.messageStatus.flowStatus, it.header.messageStatus.flowStatus)
            }
            awaitItem().let {
                assertNull(it)
            }
            awaitComplete()
        }
    }

    inner class Arrangement {

        @MockK
        lateinit var observeMessageById: ObserveMessageByIdUseCase

        @MockK
        lateinit var getUsersForMessage: GetUsersForMessageUseCase

        @MockK
        lateinit var messageMapper: MessageMapper

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun withObserveMessageById(result: Flow<Result>) = apply {
            coEvery { observeMessageById(any(), any()) } returns result
        }

        fun withGetUsersForMessage(result: List<User>) = apply {
            coEvery { getUsersForMessage(any()) } returns result
        }

        fun withMappedMessage(message: Message.Standalone, uiMessage: UIMessage) = apply {
            coEvery { messageMapper.toUIMessage(any(), eq(message)) } returns uiMessage
        }

        fun arrange() = this to ObserveMessageForConversationUseCase(observeMessageById, getUsersForMessage, messageMapper, dispatchers)
    }

    val SELF_USER = TestUser.SELF_USER
    fun TEXT_MESSAGE(status: Message.Status = Message.Status.Sent) = TestMessage.TEXT_MESSAGE.copy(
        senderUserId = SELF_USER.id,
        senderUserName = SELF_USER.name,
        status = status,
    )

    fun UI_TEXT_MESSAGE(status: MessageFlowStatus = MessageFlowStatus.Sent) = TEXT_MESSAGE().let { textMessage ->
        mockMessageWithText.copy(
            conversationId = textMessage.conversationId,
            header = mockHeader.copy(
                username = UIText.DynamicString(SELF_USER.name ?: ""),
                messageId = textMessage.id,
                messageStatus = mockHeader.messageStatus.copy(flowStatus = status)
            )
        )
    }

}
