/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.mapper.MessageMapper
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.message.MessagePagingStart
import com.wire.kalium.logic.feature.message.GetPaginatedFlowOfMessagesByConversationUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class GetMessagesForConversationUseCaseTest {

    @Test
    fun givenPagingStart_whenGettingMessages_thenForwardsAnchorWithoutPlaceholders() = runTest {
        val (arrangement, useCase) = Arrangement().arrange()
        val pagingStart = MessagePagingStart.AroundFirstUnread(itemsBefore = 29)

        useCase(arrangement.conversationId, pagingStart)

        coVerify(exactly = 1) {
            arrangement.getMessages(
                conversationId = arrangement.conversationId,
                visibility = any(),
                pagingStart = pagingStart,
                pagingConfig = match { config ->
                    config.pageSize == 20 &&
                            config.initialLoadSize == 60 &&
                            config.prefetchDistance == 30 &&
                            !config.enablePlaceholders
                }
            )
        }
    }

    private class Arrangement {
        val conversationId = ConversationId("conversation", "domain")

        @MockK
        lateinit var getMessages: GetPaginatedFlowOfMessagesByConversationUseCase

        @MockK
        lateinit var getUsersForMessage: GetUsersForMessageUseCase

        @MockK
        lateinit var messageMapper: MessageMapper

        init {
            MockKAnnotations.init(this)
            coEvery { getMessages(any(), any(), any(), any()) } returns flowOf(PagingData.empty())
        }

        fun arrange(): Pair<Arrangement, GetMessagesForConversationUseCase> = this to
                GetMessagesForConversationUseCase(
                    getMessages = getMessages,
                    getUsersForMessage = getUsersForMessage,
                    messageMapper = messageMapper,
                    dispatchers = TestDispatcherProvider(),
                )
    }
}
