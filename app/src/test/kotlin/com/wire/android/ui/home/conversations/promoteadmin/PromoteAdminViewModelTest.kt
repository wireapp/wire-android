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
package com.wire.android.ui.home.conversations.promoteadmin

import androidx.lifecycle.SavedStateHandle
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.mapper.testOtherUser
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveEligibleMembersForConversationAdminRoleUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, NavigationTestExtension::class)
class PromoteAdminViewModelTest {

    @Test
    fun givenNoUserSelected_whenScreenLoads_thenButtonIsDisabled() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        assertFalse(viewModel.state.value.isButtonEnabled)
        assertNull(viewModel.state.value.selectedUserId)
    }

    @Test
    fun givenUserSelected_whenSameUserSelectedAgain_thenSelectionCleared() = runTest {
        val userId = UserId("user1", "wire.com")
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onUserSelected(userId)
        assertTrue(viewModel.state.value.isButtonEnabled)

        viewModel.onUserSelected(userId)
        assertFalse(viewModel.state.value.isButtonEnabled)
        assertNull(viewModel.state.value.selectedUserId)
    }

    @Test
    fun givenUserSelected_whenDifferentUserSelected_thenOnlyNewUserSelected() = runTest {
        val userId1 = UserId("user1", "wire.com")
        val userId2 = UserId("user2", "wire.com")
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onUserSelected(userId1)
        assertEquals(userId1, viewModel.state.value.selectedUserId)

        viewModel.onUserSelected(userId2)
        assertEquals(userId2, viewModel.state.value.selectedUserId)
    }

    @Test
    fun givenSearchQuery_whenFiltering_thenOnlyMatchingMembersReturned() = runTest {
        val (_, viewModel) = Arrangement()
            .withEligibleMembers(listOf(member(0, "Alice", "alice"), member(1, "Bob", "bob")))
            .arrange()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("ali")
        advanceUntilIdle()

        val filtered = viewModel.state.value.filteredMembers
        assertEquals(1, filtered.size)
        assertEquals("Alice", filtered.first().name)
    }

    @Test
    fun givenEmptySearchQuery_whenFiltering_thenAllMembersReturned() = runTest {
        val (_, viewModel) = Arrangement()
            .withEligibleMembers(listOf(member(0, "Alice", "alice"), member(1, "Bob", "bob")))
            .arrange()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.filteredMembers.size)
    }

    @Test
    fun givenSearchQueryMatchesHandle_whenFiltering_thenMemberReturned() = runTest {
        val (_, viewModel) = Arrangement()
            .withEligibleMembers(listOf(member(0, "Alice", "alicewonder"), member(1, "Bob", "bobby")))
            .arrange()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("wonder")
        advanceUntilIdle()

        val filtered = viewModel.state.value.filteredMembers
        assertEquals(1, filtered.size)
        assertEquals("Alice", filtered.first().name)
    }

    @Test
    fun givenSearchQueryStartsWithAt_whenFiltering_thenLeadingAtIsStrippedAndHandleMatched() = runTest {
        val (_, viewModel) = Arrangement()
            .withEligibleMembers(listOf(member(0, "Alice", "alicewonder"), member(1, "Bob", "bobby")))
            .arrange()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("@alice")
        advanceUntilIdle()

        val filtered = viewModel.state.value.filteredMembers
        assertEquals(1, filtered.size)
        assertEquals("Alice", filtered.first().name)
    }

    @Test
    fun givenSearchQueryMatchesNeitherNameNorHandle_whenFiltering_thenResultIsEmpty() = runTest {
        val (_, viewModel) = Arrangement()
            .withEligibleMembers(listOf(member(0, "Alice", "alicewonder"), member(1, "Bob", "bobby")))
            .arrange()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("charlie")
        advanceUntilIdle()

        assertTrue(viewModel.state.value.filteredMembers.isEmpty())
    }

    private fun member(index: Int, name: String, handle: String): MemberDetails =
        MemberDetails(
            testOtherUser(index).copy(name = name, handle = handle),
            Conversation.Member.Role.Member,
        )

    @Test
    fun givenNoEligibleMembers_whenViewModelCreated_thenAllMembersIsEmpty() = runTest {
        val (_, viewModel) = Arrangement()
            .withEligibleMembers(emptyList())
            .arrange()

        advanceUntilIdle()

        assertTrue(viewModel.state.value.filteredMembers.isEmpty())
    }

    private inner class Arrangement {
        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var observeEligibleMembers: ObserveEligibleMembersForConversationAdminRoleUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { savedStateHandle.navArgs<PromoteAdminNavArgs>() } returns
                PromoteAdminNavArgs(ConversationId("conv1", "wire.com"))
            coEvery { observeEligibleMembers(any()) } returns flowOf(emptyList())
        }

        fun withEligibleMembers(members: List<MemberDetails>) = apply {
            coEvery { observeEligibleMembers(any()) } returns flowOf(members)
        }

        fun arrange() = this to PromoteAdminViewModel(savedStateHandle, observeEligibleMembers)
    }
}
