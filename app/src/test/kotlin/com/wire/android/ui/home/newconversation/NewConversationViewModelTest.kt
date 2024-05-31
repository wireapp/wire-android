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

@file:Suppress("MaxLineLength")

package com.wire.android.ui.home.newconversation

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class NewConversationViewModelTest {

    @Test
    fun `given sync failure, when creating group, then should update options state with connectivity error`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withSyncFailureOnCreatingGroup()
            .arrange()

        viewModel.createGroup(arrangement.onGroupCreated)
        advanceUntilIdle()

        viewModel.createGroupState.error shouldBeEqualTo CreateGroupState.Error.LackingConnection
    }

    @Test
    fun `given unknown failure, when creating group, then should update options state with unknown error`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withUnknownFailureOnCreatingGroup()
            .arrange()

        viewModel.createGroup(arrangement.onGroupCreated)
        advanceUntilIdle()

        viewModel.createGroupState.error shouldBeEqualTo CreateGroupState.Error.Unknown
    }

    @Test
    fun `given no failure, when creating group, then options state should have no error`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .arrange()

        viewModel.createGroup(arrangement.onGroupCreated)
        advanceUntilIdle()

        viewModel.createGroupState.error.shouldBeNull()
    }

    @Test
    fun `given create group conflicted backends error, when clicked on dismiss, then error should be cleaned`() =
        runTest {
            val (_, viewModel) = NewConversationViewModelArrangement()
                .withGetSelfUser(isTeamMember = true)
                .withConflictingBackendsFailure()
                .arrange()

            viewModel.onCreateGroupErrorDismiss()
            advanceUntilIdle()
            viewModel.createGroupState.error.shouldBeNull()
        }

    @Test
    fun `given self is not a team member, when creating group, then the group is created with the correct values`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = false)
            .arrange()

        viewModel.createGroup(arrangement.onGroupCreated)
        advanceUntilIdle()

        viewModel.createGroupState.error.shouldBeNull()

        coVerify {
            arrangement.createGroupConversation(
                viewModel.newGroupNameTextState.text.toString(),
                viewModel.newGroupState.selectedUsers.map { contact -> UserId(contact.id, contact.domain) },
                ConversationOptions(
                    Conversation.defaultGroupAccess,
                    Conversation.defaultGroupAccessRoles,
                    false,
                    ConversationOptions.Protocol.PROTEUS,
                    null
                )
            )
        }
    }

    @Test
    fun `given self is team member and guests are enabled, when creating group, then the group is created with the correct values`() =
        runTest {
            val (arrangement, viewModel) = NewConversationViewModelArrangement()
                .withGetSelfUser(isTeamMember = true)
                .withServicesEnabled(false)
                .withGuestEnabled(true)
                .arrange()

            viewModel.createGroup(arrangement.onGroupCreated)
            advanceUntilIdle()

            viewModel.createGroupState.error.shouldBeNull()

            coVerify {
                arrangement.createGroupConversation(
                    viewModel.newGroupNameTextState.text.toString(),
                    viewModel.newGroupState.selectedUsers.map { contact -> UserId(contact.id, contact.domain) },
                    ConversationOptions(
                        setOf(Conversation.Access.INVITE, Conversation.Access.CODE),
                        setOf(Conversation.AccessRole.TEAM_MEMBER, Conversation.AccessRole.NON_TEAM_MEMBER, Conversation.AccessRole.GUEST),
                        true,
                        ConversationOptions.Protocol.PROTEUS,
                        null
                    )
                )
            }
        }

    @Test
    fun `given team settings is MLS default protocol, when getting default protocol, then result is MLS`() = runTest {
        // given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withDefaultProtocol(SupportedProtocol.MLS)
            .withGetSelfUser(isTeamMember = true)
            .withServicesEnabled(false)
            .withGuestEnabled(true)
            .arrange()

        // when
        val result = viewModel.newGroupState.groupProtocol

        // then
        assertEquals(
            ConversationOptions.Protocol.MLS,
            result
        )
    }

    @Test
    fun `given self is external team member, when creating group, then creating group should not be allowed`() = runTest {
            // given
            val (_, viewModel) = NewConversationViewModelArrangement()
                .withGetSelfUser(isTeamMember = true, userType = UserType.EXTERNAL)
                .arrange()
            advanceUntilIdle()
            // when
            val result = viewModel.newGroupState.isGroupCreatingAllowed
            // then
            assertEquals(false, result)
        }
    @Test
    fun `given self is internal team member, when creating group, then creating group should be allowed`() = runTest {
        // given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true, userType = UserType.INTERNAL)
            .arrange()
        advanceUntilIdle()
        // when
        val result = viewModel.newGroupState.isGroupCreatingAllowed
        // then
        assertEquals(true, result)
    }
}
