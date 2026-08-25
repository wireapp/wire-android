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

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.framework.TestUser
import com.wire.android.navigation.HomeDestination
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.user.ObserveIsMeetingsEnabledUseCase
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
@OptIn(ExperimentalCoroutinesApi::class)
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
            viewModel.drawerItems()
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
            viewModel.drawerItems()
                .filterIsInstance<DrawerUiItem.DynamicExternalNavigationItem>()
                .first().url
        )
    }

    @Test
    fun `given cell disabled, when starts checking, then do not show cells drawer item`() =
        runTest {
            // Given
            val (arrangement, viewModel) = Arrangement()
                .withWireCellsEnabled(false)
                .arrange()

            // When
            arrangement.unreadArchivedConversationsCountChannel.send(0L)
            advanceUntilIdle()

            // Then
            assertFalse(
                viewModel.hasRegularItem(HomeDestination.Cells)
            )
        }

    @Test
    fun `given cell enabled and no cell conversation, when starts checking, then show Cell drawer item`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withWireCellsEnabled(true)
            .arrange()

        // When
        arrangement.unreadArchivedConversationsCountChannel.send(0L)
        advanceUntilIdle()

        // Then
        assertTrue(
            viewModel.hasRegularItem(HomeDestination.Cells)
        )
    }

    @Test
    fun `given meetings disabled, when starts observing, then do not show Meetings drawer item`() =
        runTest {
            // Given
            val (arrangement, viewModel) = Arrangement()
                .withWireMeetingsEnabled(false)
                .arrange()

            // When
            arrangement.unreadArchivedConversationsCountChannel.send(0L)
            advanceUntilIdle()

            // Then
            assertFalse(viewModel.hasRegularItem(HomeDestination.Meetings))
        }

    @Test
    fun `given meetings enabled, when starts observing, then show Meetings drawer item`() = runTest {
        // Given
        val (arrangement, viewModel) = Arrangement()
            .withWireMeetingsEnabled(true)
            .arrange()

        // When
        arrangement.unreadArchivedConversationsCountChannel.send(0L)
        advanceUntilIdle()

        // Then
        assertTrue(viewModel.hasRegularItem(HomeDestination.Meetings))
    }

    private fun HomeDrawerViewModel.drawerItems(): List<DrawerUiItem> =
        listOf(drawerState.items.first, drawerState.items.second).flatten()

    private fun HomeDrawerViewModel.hasRegularItem(destination: HomeDestination): Boolean =
        drawerItems()
            .filterIsInstance<DrawerUiItem.RegularItem>()
            .any { it.destination == destination }

    private class Arrangement {

        @MockK
        lateinit var observeArchivedUnreadConversationsCount: ObserveArchivedUnreadConversationsCountUseCase

        @MockK
        lateinit var isWireCellsEnabled: IsWireCellsEnabledUseCase

        @MockK
        lateinit var observeSelfUserUseCase: ObserveSelfUserUseCase

        @MockK
        lateinit var observeIsWireMeetingsEnabled: ObserveIsMeetingsEnabledUseCase

        @MockK
        lateinit var getTeamUrlUseCase: GetTeamUrlUseCase

        val unreadArchivedConversationsCountChannel = Channel<Long>(capacity = Channel.UNLIMITED)

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeArchivedUnreadConversationsCount() } returns unreadArchivedConversationsCountChannel.consumeAsFlow()
            coEvery { isWireCellsEnabled() } returns false
            coEvery { observeIsWireMeetingsEnabled() } returns flowOf(false)
            withSelfUserType()
            coEvery { getTeamUrlUseCase() } returns TEAM_URL
        }

        fun withSelfUserType(type: UserType = UserType.INTERNAL) = apply {
            coEvery { observeSelfUserUseCase() } returns flowOf(TestUser.SELF_USER.copy(userType = UserTypeInfo.Regular(type)))
        }

        fun withWireCellsEnabled(enabled: Boolean) = apply {
            coEvery { isWireCellsEnabled() } returns enabled
        }

        fun withWireMeetingsEnabled(enabled: Boolean) = apply {
            coEvery { observeIsWireMeetingsEnabled() } returns flowOf(enabled)
        }

        fun arrange() = this to HomeDrawerViewModel(
            observeArchivedUnreadConversationsCount = lazyOf(observeArchivedUnreadConversationsCount),
            observeSelfUser = observeSelfUserUseCase,
            getTeamUrl = getTeamUrlUseCase,
            isWireCellsEnabled = isWireCellsEnabled,
            observeIsWireMeetingsEnabled = observeIsWireMeetingsEnabled
        )

        companion object {
            const val TEAM_URL = "some-url"
        }
    }
}
