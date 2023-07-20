/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.mapper

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestMessage
import com.wire.android.ui.home.conversations.model.MessageBody
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageContentMapperTest {

    @Test
    fun givenMessagesWithDifferentVisibilities_whenMappingToUIMessageContent_thenCorrectValuesShouldBeReturned() = runTest {
        // Given
        val (_, mapper) = Arrangement().arrange()
        val visibleMessage = TestMessage.TEXT_MESSAGE.copy(visibility = Message.Visibility.VISIBLE)
        val deletedMessage = TestMessage.TEXT_MESSAGE.copy(
            visibility = Message.Visibility.DELETED,
            content = MessageContent.Text("")
        )
        val hiddenMessage = TestMessage.TEXT_MESSAGE.copy(
            visibility = Message.Visibility.HIDDEN,
            content = MessageContent.Text("")
        )
        // When
        val resultContentVisible = mapper.fromMessage(visibleMessage, listOf())
        val resultContentDeleted = mapper.fromMessage(deletedMessage, listOf())
        val resultContentHidden = mapper.fromMessage(hiddenMessage, listOf())
        // Then
        assertTrue(resultContentVisible != null)
        assertTrue(resultContentDeleted == UIMessageContent.Deleted)
        assertTrue(resultContentHidden == null)
    }

    private class Arrangement {

        @MockK
        lateinit var regularMessageMapper: RegularMessageMapper

        @MockK
        lateinit var systemMessageContentMapper: SystemMessageContentMapper

        private val messageContentMapper by lazy {
            MessageContentMapper(regularMessageMapper, systemMessageContentMapper)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { regularMessageMapper.mapMessage(any(), any(), any()) } returns UIMessageContent.TextMessage(
                MessageBody(UIText.DynamicString("some message text"))
            )
            coEvery { systemMessageContentMapper.mapMessage(any(), any()) } returns UIMessageContent.SystemMessage.HistoryLost
        }

        fun arrange() = this to messageContentMapper
    }
}
