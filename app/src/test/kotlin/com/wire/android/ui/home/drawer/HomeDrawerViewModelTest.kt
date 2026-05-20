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
package com.wire.android.ui.home.drawer

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.framework.TestConversation
import com.wire.android.framework.TestConversationDetails
import com.wire.android.framework.TestUser
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(NavigationTestExtension::class)
class HomeDrawerViewModelTest {

    @Test
    fun `given archivedUnreadConversationsCount, when starts observing, then returns correct integer value`() = runTest {
        // Given
        val unreadCount = 10L
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        // When
        arrangement.unreadArchivedConversationsCountChannel.send(unreadCount)
        advanceUntilIdle()

        // Then
        assertEquals(
            unreadCount,
            listOf(
                viewModel.drawerState.items.first,
                viewModel.drawerState.items.second
            ).flatten()
                .filterIsInstance<DrawerUiItem.UnreadCounterItem>()
                .first().unreadCount
        )
    }

    @Test
    fun `given userIsAdmin, when starts observing, then set team url`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withSelfUserType(UserType.ADMIN)
            .arrange()

        // When
        arrangement.unreadArchivedConversationsCountChannel.send(0L)
        advanceUntilIdle()

        // Then
        assertEquals(
            Arrangement.TEAM_URL,
            listOf(
                viewModel.drawerState.items.first,
                viewModel.drawerState.items.second
            ).flatten()
                .filterIsInstance<DrawerUiItem.DynamicExternalNavigationItem>()
                .first().url
        )
    }

    @Test
    fun `given wireCellsEnabledGlobally, when starts observing, then show cells drawer item`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withWireCellsEnabled(true)
            .arrange()

        // When
        arrangement.unreadArchivedConversationsCountChannel.send(0L)
        advanceUntilIdle()

        // Then
        assertTrue(
            listOf(
                viewModel.drawerState.items.first,
                viewModel.drawerState.items.second
            ).flatten()
                .filterIsInstance<DrawerUiItem.RegularItem>()
                .any { it.destination.toString().contains("Cells") }
        )
    }

    @Test
    fun `given userInConversationWithCellsEnabled, when starts observing, then show cells drawer item`() = runTest {
        // Given
        val conversationWithCells = ConversationDetails.Group.Regular(
            TestConversation.GROUP(),
            isSelfUserMember = true,
            selfRole = com.wire.kalium.logic.data.conversation.Conversation.Member.Role.Member,
            wireCell = "2024-01-01T00:00:00.000Z",
        )
        val (arrangement, viewModel) = Arrangement()
            .withWireCellsEnabled(false)
            .withConversations(listOf(conversationWithCells))
            .arrange()

        // When
        arrangement.unreadArchivedConversationsCountChannel.send(0L)
        advanceUntilIdle()

        // Then
        assertTrue(
            listOf(
                viewModel.drawerState.items.first,
                viewModel.drawerState.items.second
            ).flatten()
                .filterIsInstance<DrawerUiItem.RegularItem>()
                .any { it.destination.toString().contains("Cells") }
        )
    }

    @Test
    fun `given noCellsEnabled_whenStartsObserving_thenDontShowCellsDrawerItem`() = runTest {
        // Given
        val conversationWithoutCells = TestConversationDetails.GROUP
        val (arrangement, viewModel) = Arrangement()
            .withWireCellsEnabled(false)
            .withConversations(listOf(conversationWithoutCells))
            .arrange()

        // When
        arrangement.unreadArchivedConversationsCountChannel.send(0L)
        advanceUntilIdle()

        // Then
        assertFalse(
            listOf(
                viewModel.drawerState.items.first,
                viewModel.drawerState.items.second
            ).flatten()
                .filterIsInstance<DrawerUiItem.RegularItem>()
                .any { it.destination.toString().contains("Cells") }
        )
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var observeArchivedUnreadConversationsCount: ObserveArchivedUnreadConversationsCountUseCase

        @MockK
        lateinit var isWireCellsEnabled: IsWireCellsEnabledUseCase

        @MockK
        lateinit var observeSelfUserUseCase: ObserveSelfUserUseCase

        @MockK
        lateinit var getTeamUrlUseCase: GetTeamUrlUseCase

        @MockK
        lateinit var observeConversationListDetailsUseCase: ObserveConversationListDetailsUseCase

        val unreadArchivedConversationsCountChannel = Channel<Long>(capacity = Channel.UNLIMITED)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeArchivedUnreadConversationsCount() } returns unreadArchivedConversationsCountChannel.consumeAsFlow()
            coEvery { isWireCellsEnabled() } returns false
            coEvery { observeConversationListDetailsUseCase(fromArchive = false) } returns flowOf(emptyList())
            withSelfUserType()
            coEvery { getTeamUrlUseCase() } returns TEAM_URL
        }

        fun withSelfUserType(type: UserType = UserType.INTERNAL) = apply {
            coEvery { observeSelfUserUseCase() } returns flowOf(TestUser.SELF_USER.copy(userType = UserTypeInfo.Regular(type)))
        }

        fun withWireCellsEnabled(enabled: Boolean) = apply {
            coEvery { isWireCellsEnabled() } returns enabled
        }

        fun withConversations(conversations: List<ConversationDetails>) = apply {
            coEvery { observeConversationListDetailsUseCase(fromArchive = false) } returns flowOf(conversations)
        }

        fun arrange() = this to HomeDrawerViewModel(
            savedStateHandle = savedStateHandle,
            observeArchivedUnreadConversationsCount = { observeArchivedUnreadConversationsCount },
            observeSelfUser = observeSelfUserUseCase,
            getTeamUrl = getTeamUrlUseCase,
            isWireCellsEnabled = isWireCellsEnabled,
            observeConversationListDetails = { observeConversationListDetailsUseCase },
        )

        companion object {
            const val TEAM_URL = "some-url"
        }
    }
}
