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
import com.wire.android.framework.TestConversationDetails
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
import com.wire.kalium.logic.data.conversation.ConversationQueryConfig
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class GetConversationsFromSearchUseCaseTest {
    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun givenSearchQuery_whenGettingPaginatedList_thenCallUseCaseWithProperParams() = runTest(dispatcherProvider.main()) {
        // Given
        val (arrangement, useCase) = Arrangement().arrange()
        // When
        with(arrangement.queryConfig) {
            useCase(searchQuery, fromArchive, newActivitiesOnTop, onlyInteractionEnabled)
        }
        // Then
        coVerify {
            arrangement.useCase(queryConfig = eq(arrangement.queryConfig), pagingConfig = any(), startingOffset = any())
        }
    }

    @Test
    fun givenConversations_whenGettingPaginatedList_thenReturnCorrectValues() = runTest(dispatcherProvider.main()) {
        // Given
        val conversationsList = listOf(
            ConversationDetailsWithEvents(TestConversationDetails.CONNECTION),
            ConversationDetailsWithEvents(TestConversationDetails.CONVERSATION_ONE_ONE),
            ConversationDetailsWithEvents(TestConversationDetails.GROUP),
        )
        val (arrangement, useCase) = Arrangement()
            .withPaginatedResult(conversationsList)
            .arrange()
        // When
        val result = with(arrangement.queryConfig) {
            useCase(searchQuery, fromArchive, newActivitiesOnTop, onlyInteractionEnabled).asSnapshot()
        }
        // Then
        result.forEachIndexed { index, conversationItem ->
            assertEquals(conversationsList[index].conversationDetails.conversation.id, conversationItem.conversationId)
            assertEquals(arrangement.queryConfig.searchQuery, conversationItem.searchQuery)
        }
    }

    inner class Arrangement {

        @MockK
        lateinit var useCase: GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase

        @MockK
        lateinit var userTypeMapper: UserTypeMapper

        val queryConfig = ConversationQueryConfig(
            searchQuery = "search",
            fromArchive = false,
            newActivitiesOnTop = true,
            onlyInteractionEnabled = false,
        )

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { userTypeMapper.toMembership(any()) } returns Membership.Standard
            withPaginatedResult(emptyList())
        }

        fun withPaginatedResult(conversations: List<ConversationDetailsWithEvents> = emptyList()) = apply {
            coEvery {
                useCase.invoke(any(), any(), any())
            } returns flowOf(PagingData.from(conversations))
        }

        fun arrange() = this to GetConversationsFromSearchUseCase(useCase, userTypeMapper, dispatcherProvider)
    }
}
