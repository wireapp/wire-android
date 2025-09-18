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
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.conversation.ConversationQueryConfig
import com.wire.kalium.logic.data.conversation.FolderType
import com.wire.kalium.logic.feature.conversation.GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase
import com.wire.kalium.logic.feature.conversation.folder.GetFavoriteFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveConversationsFromFolderUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf
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
            useCase(
                searchQuery = searchQuery,
                fromArchive = fromArchive,
                newActivitiesOnTop = newActivitiesOnTop,
                onlyInteractionEnabled = onlyInteractionEnabled,
                useStrictMlsFilter = true
            )
        }
        // Then
        coVerify {
            arrangement.useCase(
                queryConfig = eq(arrangement.queryConfig),
                pagingConfig = any(),
                startingOffset = any(),
                strictMlsFilter = any()
            )
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
        val (arrangement, useCase) = Arrangement().withPaginatedResult(conversationsList).withSelfUser().arrange()
        // When
        val result = with(arrangement.queryConfig) {
            useCase(
                searchQuery = searchQuery,
                fromArchive = fromArchive,
                newActivitiesOnTop = newActivitiesOnTop,
                onlyInteractionEnabled = onlyInteractionEnabled,
                useStrictMlsFilter = true,
            ).asSnapshot()
        }
        // Then
        result.forEachIndexed { index, conversationItem ->
            assertEquals(conversationsList[index].conversationDetails.conversation.id, conversationItem.conversationId)
            assertEquals(arrangement.queryConfig.searchQuery, conversationItem.searchQuery)
        }
    }

    @Test
    fun givenFavoritesFilter_whenGettingConversations_thenObserveConversationsFromFolder() = runTest(dispatcherProvider.main()) {
        // Given
        val favoriteFolderId = "folder_id"
        val folderResult = GetFavoriteFolderUseCase.Result.Success(
            folder = ConversationFolder(id = favoriteFolderId, name = "", FolderType.FAVORITE)
        )
        val conversationsList = listOf(
            ConversationDetailsWithEvents(TestConversationDetails.CONVERSATION_ONE_ONE)
        )

        val (arrangement, useCase) = Arrangement().withFavoriteFolderResult(folderResult).withFolderConversationsResult(conversationsList)
            .withSelfUser().arrange()

        // When
        useCase(
            searchQuery = "",
            fromArchive = false,
            newActivitiesOnTop = false,
            onlyInteractionEnabled = false,
            conversationFilter = ConversationFilter.Favorites,
            useStrictMlsFilter = true,
        ).asSnapshot()

        // Then
        coVerify(exactly = 1) { arrangement.getFavoriteFolderUseCase.invoke() }
        coVerify(exactly = 1) { arrangement.observeConversationsFromFolderUseCase.invoke(favoriteFolderId) }
        coVerify(exactly = 0) { arrangement.useCase(any(), any(), any(), any()) }
    }

    @Test
    fun givenGroupConversation_whenConversationFromTheSameTeam_thenReturnDataWithProperlySameTeamSet() =
        runTest(dispatcherProvider.main()) {
            // Given
            val conversationsList = listOf(
                ConversationDetailsWithEvents(
                    TestConversationDetails.GROUP.copy(
                        conversation = TestConversationDetails.GROUP.conversation.copy(
                            teamId = TestUser.SELF_USER.teamId
                        )
                    )
                )
            )
            val (arrangement, useCase) = Arrangement().withPaginatedResult(conversationsList).withSelfUser().arrange()
            // When
            val result = with(arrangement.queryConfig) {
                useCase(
                    searchQuery = searchQuery,
                    fromArchive = fromArchive,
                    newActivitiesOnTop = newActivitiesOnTop,
                    onlyInteractionEnabled = onlyInteractionEnabled,
                    useStrictMlsFilter = true
                ).asSnapshot()
            }
            // Then
            val conversation = result.first()
            assertInstanceOf<ConversationItem.Group>(conversation)
            assertEquals(true, conversation.isFromTheSameTeam)
        }

    @Test
    fun givenGroupConversation_whenConversationNotFromTheSameTeam_thenReturnDataWithProperlySameTeamSet() =
        runTest(dispatcherProvider.main()) {
            // Given
            val conversationsList = listOf(ConversationDetailsWithEvents(TestConversationDetails.GROUP))
            val (arrangement, useCase) = Arrangement().withPaginatedResult(conversationsList).withSelfUser().arrange()
            // When
            val result = with(arrangement.queryConfig) {
                useCase(
                    searchQuery = searchQuery,
                    fromArchive = fromArchive,
                    newActivitiesOnTop = newActivitiesOnTop,
                    onlyInteractionEnabled = onlyInteractionEnabled,
                    useStrictMlsFilter = true
                ).asSnapshot()
            }
            // Then
            val conversation = result.first()
            assertInstanceOf<ConversationItem.Group>(conversation)
            assertEquals(false, conversation.isFromTheSameTeam)
        }

    inner class Arrangement {

        @MockK
        lateinit var useCase: GetPaginatedFlowOfConversationDetailsWithEventsBySearchQueryUseCase

        @MockK
        lateinit var getFavoriteFolderUseCase: GetFavoriteFolderUseCase

        @MockK
        lateinit var observeConversationsFromFolderUseCase: ObserveConversationsFromFolderUseCase

        @MockK
        lateinit var userTypeMapper: UserTypeMapper

        @MockK
        lateinit var getSelfUser: GetSelfUserUseCase

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
                useCase.invoke(any(), any(), any(), any())
            } returns flowOf(PagingData.from(conversations))
        }

        fun withFavoriteFolderResult(result: GetFavoriteFolderUseCase.Result) = apply {
            coEvery { getFavoriteFolderUseCase.invoke() } returns result
        }

        fun withFolderConversationsResult(conversations: List<ConversationDetailsWithEvents>) = apply {
            coEvery {
                observeConversationsFromFolderUseCase.invoke(any())
            } returns flowOf(conversations)
        }

        fun withSelfUser() = apply {
            coEvery { getSelfUser() } returns TestUser.SELF_USER
        }

        fun arrange() = this to GetConversationsFromSearchUseCase(
            useCase, getFavoriteFolderUseCase, observeConversationsFromFolderUseCase, userTypeMapper, dispatcherProvider, getSelfUser
        )
    }
}
