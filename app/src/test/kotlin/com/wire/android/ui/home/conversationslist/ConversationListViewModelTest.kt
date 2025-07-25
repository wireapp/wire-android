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
@file:Suppress("MaxLineLength", "MaximumLineLength")

package com.wire.android.ui.home.conversationslist

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestConversationItem
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.media.audiomessage.PlayingAudioMessage
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversationslist.model.ConversationItem
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.kalium.logic.data.conversation.ConversationDetailsWithEvents
import com.wire.kalium.logic.data.conversation.ConversationFilter
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.legalhold.LegalHoldStateForSelfUser
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
// TODO write more tests
class ConversationListViewModelTest {

    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given initial empty search query, when collecting conversations, then call use case with proper params`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN).arrange()

            // When
            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.test {
                // Then
                coVerify(exactly = 1) {
                    arrangement.getConversationsPaginated("", false, true, false)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given updated non-empty search query, when collecting conversations, then call use case with proper params`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val searchQueryText = "search"
            val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN).arrange()

            // When
            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.test {
                conversationListViewModel.searchQueryChanged(searchQueryText)
                advanceUntilIdle()

                // Then
                coVerify(exactly = 1) {
                    arrangement.getConversationsPaginated(searchQueryText, false, true, false)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given updated non-empty search query, when collecting archived, then call use case with proper params`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val searchQueryText = "search"
            val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.ARCHIVE).arrange()

            // When
            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.test {
                conversationListViewModel.searchQueryChanged(searchQueryText)
                advanceUntilIdle()

                // Then
                coVerify(exactly = 1) {
                    arrangement.getConversationsPaginated(searchQueryText, true, false, false)
                }
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given self user is under legal hold, when collecting conversations, then hide LH indicators`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val conversations = listOf(
                TestConversationItem.CONNECTION.copy(conversationId = ConversationId("conn_1", ""), showLegalHoldIndicator = true),
                TestConversationItem.CONNECTION.copy(conversationId = ConversationId("conn_2", ""), showLegalHoldIndicator = false),
                TestConversationItem.PRIVATE.copy(conversationId = ConversationId("private_1", ""), showLegalHoldIndicator = true),
                TestConversationItem.PRIVATE.copy(conversationId = ConversationId("private_2", ""), showLegalHoldIndicator = false),
                TestConversationItem.GROUP.copy(conversationId = ConversationId("group_1", ""), showLegalHoldIndicator = true),
                TestConversationItem.GROUP.copy(conversationId = ConversationId("group_2", ""), showLegalHoldIndicator = false),
            ).associateBy { it.conversationId }
            val (_, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN)
                .withConversationsPaginated(conversations.values.toList())
                .withSelfUserLegalHoldState(LegalHoldStateForSelfUser.Enabled)
                .arrange()
            advanceUntilIdle()

            // When
            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.asSnapshot()
                .filterIsInstance<ConversationItem>()
                .forEach {
                    // Then
                    assertEquals(false, it.showLegalHoldIndicator) // self user is under LH so hide LH indicators next to conversations
                }
        }

    @Test
    fun `given self user is not under legal hold, when collecting conversations, then show LH indicator when conversation is under LH`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val conversations = listOf(
                TestConversationItem.CONNECTION.copy(conversationId = ConversationId("conn_1", ""), showLegalHoldIndicator = true),
                TestConversationItem.CONNECTION.copy(conversationId = ConversationId("conn_2", ""), showLegalHoldIndicator = false),
                TestConversationItem.PRIVATE.copy(conversationId = ConversationId("private_1", ""), showLegalHoldIndicator = true),
                TestConversationItem.PRIVATE.copy(conversationId = ConversationId("private_2", ""), showLegalHoldIndicator = false),
                TestConversationItem.GROUP.copy(conversationId = ConversationId("group_1", ""), showLegalHoldIndicator = true),
                TestConversationItem.GROUP.copy(conversationId = ConversationId("group_2", ""), showLegalHoldIndicator = false),
            ).associateBy { it.conversationId }
            val (_, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN)
                .withConversationsPaginated(conversations.values.toList())
                .withSelfUserLegalHoldState(LegalHoldStateForSelfUser.Disabled)
                .arrange()
            advanceUntilIdle()

            // When
            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.asSnapshot()
                .filterIsInstance<ConversationItem>()
                .forEach {
                    // Then
                    val expected = conversations[it.conversationId]!!.showLegalHoldIndicator // show indicator when conversation is under LH
                    assertEquals(expected, it.showLegalHoldIndicator)
                }
        }

    @Test
    fun `given cached PagingData, when self user legal hold changes, then should call paginated use case again`() =
        runTest(dispatcherProvider.main()) {
            // given
            val conversations = listOf(
                TestConversationItem.CONNECTION.copy(conversationId = ConversationId("conn_1", "")),
                TestConversationItem.PRIVATE.copy(conversationId = ConversationId("private_1", "")),
                TestConversationItem.GROUP.copy(conversationId = ConversationId("group_1", "")),
            ).associateBy { it.conversationId }
            val selfUserLegalHoldStateFlow = MutableSharedFlow<LegalHoldStateForSelfUser>()
            val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN)
                .withConversationsPaginated(conversations.values.toList())
                .withSelfUserLegalHoldStateFlow(selfUserLegalHoldStateFlow)
                .arrange()
            advanceUntilIdle()

            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.test {
                // initial legal hold state
                selfUserLegalHoldStateFlow.emit(LegalHoldStateForSelfUser.Disabled)
                advanceUntilIdle()

                // use case is called initially
                coVerify(exactly = 1) {
                    arrangement.getConversationsPaginated(any(), any(), any(), any(), any())
                }

                // when legal hold state is changed
                selfUserLegalHoldStateFlow.emit(LegalHoldStateForSelfUser.Enabled)
                advanceUntilIdle()

                // then use case should be called again (in total 2 executions) to create new PagingData
                coVerify(exactly = 2) {
                    arrangement.getConversationsPaginated(any(), any(), any(), any(), any())
                }

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given cached PagingData, when observing twice, then paginated use case should not be called again`() =
        runTest(dispatcherProvider.main()) {
            // given
            val conversations = listOf(
                TestConversationItem.CONNECTION.copy(conversationId = ConversationId("conn_1", "")),
                TestConversationItem.PRIVATE.copy(conversationId = ConversationId("private_1", "")),
                TestConversationItem.GROUP.copy(conversationId = ConversationId("group_1", "")),
            ).associateBy { it.conversationId }
            val (arrangement, conversationListViewModel) = Arrangement(conversationsSource = ConversationsSource.MAIN)
                .withConversationsPaginated(conversations.values.toList())
                .withSelfUserLegalHoldState(LegalHoldStateForSelfUser.Disabled)
                .arrange()
            advanceUntilIdle()

            // flow is collected first time
            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.first()

            // use case is called initially
            coVerify(exactly = 1) {
                arrangement.getConversationsPaginated(any(), any(), any(), any(), any())
            }

            // flow is collected second time
            (conversationListViewModel.conversationListState as ConversationListState.Paginated).conversations.first()

            // use case should NOT be called again, there should be still only one call
            coVerify(exactly = 1) {
                arrangement.getConversationsPaginated(any(), any(), any(), any(), any())
            }
        }

    inner class Arrangement(val conversationsSource: ConversationsSource = ConversationsSource.MAIN) {
        @MockK
        lateinit var getConversationsPaginated: GetConversationsFromSearchUseCase

        @MockK
        private lateinit var refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase

        @MockK
        private lateinit var refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase

        @MockK
        private lateinit var observeConversationListDetailsWithEventsUseCase:
                ObserveConversationListDetailsWithEventsUseCase

        @MockK
        private lateinit var observeLegalHoldStateForSelfUserUseCase: ObserveLegalHoldStateForSelfUserUseCase

        @MockK
        private lateinit var getSelfUser: GetSelfUserUseCase

        @MockK
        lateinit var audioMessagePlayer: ConversationAudioMessagePlayer

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            withConversationsPaginated(listOf(TestConversationItem.CONNECTION, TestConversationItem.PRIVATE, TestConversationItem.GROUP))
            withSelfUserLegalHoldState(LegalHoldStateForSelfUser.Disabled)
            coEvery { observeConversationListDetailsWithEventsUseCase.invoke(false, ConversationFilter.All) } returns flowOf(
                listOf(
                    TestConversationDetails.CONNECTION,
                    TestConversationDetails.CONVERSATION_ONE_ONE,
                    TestConversationDetails.GROUP
                ).map {
                    ConversationDetailsWithEvents(
                        conversationDetails = it
                    )
                }
            )
            every { audioMessagePlayer.playingAudioMessageFlow } returns flowOf(PlayingAudioMessage.None)
            mockUri()
        }

        fun withConversationsPaginated(items: List<ConversationItem>) = apply {
            coEvery {
                getConversationsPaginated.invoke(any(), any(), any(), any())
            } returns flowOf(
                PagingData.from(
                    data = items,
                    sourceLoadStates = LoadStates(
                        prepend = LoadState.NotLoading(true),
                        append = LoadState.NotLoading(true),
                        refresh = LoadState.NotLoading(true),
                    ),
                )
            )
        }

        fun withSelfUserLegalHoldState(legalHoldStateForSelfUser: LegalHoldStateForSelfUser) = apply {
            coEvery { observeLegalHoldStateForSelfUserUseCase() } returns flowOf(legalHoldStateForSelfUser)
        }

        fun withSelfUserLegalHoldStateFlow(legalHoldStateForSelfUserFlow: Flow<LegalHoldStateForSelfUser>) = apply {
            coEvery { observeLegalHoldStateForSelfUserUseCase() } returns legalHoldStateForSelfUserFlow
        }

        fun arrange() = this to ConversationListViewModelImpl(
            conversationsSource = conversationsSource,
            dispatcher = dispatcherProvider,
            getConversationsPaginated = getConversationsPaginated,
            refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
            refreshConversationsWithoutMetadata = refreshConversationsWithoutMetadata,
            currentAccount = TestUser.SELF_USER_ID,
            observeConversationListDetailsWithEvents = observeConversationListDetailsWithEventsUseCase,
            observeLegalHoldStateForSelfUser = observeLegalHoldStateForSelfUserUseCase,
            userTypeMapper = UserTypeMapper(),
            getSelfUser = getSelfUser,
            usePagination = true,
            audioMessagePlayer = audioMessagePlayer,
        )
    }
}
