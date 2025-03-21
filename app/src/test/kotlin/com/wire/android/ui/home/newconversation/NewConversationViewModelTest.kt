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

import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationOptions
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import io.mockk.coVerify
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.fail
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
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
            arrangement.createRegularGroup(
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
                .arrange()

            viewModel.onAllowServicesStatusChanged(false)
            viewModel.onAllowGuestStatusChanged(true)
            viewModel.createGroup(arrangement.onGroupCreated)
            advanceUntilIdle()

            viewModel.createGroupState.error.shouldBeNull()

            coVerify {
                arrangement.createRegularGroup(
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
            .arrange()

        // when
        val result = viewModel.newGroupState.groupProtocol
        val result2 = viewModel.groupOptionsState

        // then
        assertEquals(ConversationOptions.Protocol.MLS, result)
        assertEquals(false, result2.isAllowServicesEnabled)
        assertEquals(false, result2.isAllowServicesPossible)
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

    @Test
    fun `given group name, when creating group, then do not show NameEmptyError until name is entered and cleared`() = runTest {
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .arrange()
        viewModel.newGroupNameTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
        advanceUntilIdle()
        assertEquals(GroupMetadataState.NewGroupError.None, viewModel.newGroupState.error)

        viewModel.newGroupNameTextState.setTextAndPlaceCursorAtEnd("name")
        advanceUntilIdle()
        assertEquals(GroupMetadataState.NewGroupError.None, viewModel.newGroupState.error)

        viewModel.newGroupNameTextState.clearText()
        advanceUntilIdle()
        assertEquals(GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError, viewModel.newGroupState.error)
    }

    @Test
    fun `given conversation is created, when guest are selected and guests are disabled, then set the correct state`() = runTest {

        val usersSelected = persistentSetOf(
            Contact(
                "id",
                "domain",
                "name",
                "handle",
                UserAvatarData(),
                label = "label",
                connectionState = ConnectionState.ACCEPTED,
                membership = Membership.Guest
            )
        )

        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .arrange()

        viewModel.newGroupState = viewModel.newGroupState.copy(
            selectedUsers = usersSelected
        )

        viewModel.groupOptionsState = viewModel.groupOptionsState.copy(isAllowGuestEnabled = false)

        viewModel.createGroup { _ -> fail("group should not be created") }

        assertTrue(viewModel.groupOptionsState.showAllowGuestsDialog)

        coVerify(exactly = 0) {
            arrangement.createRegularGroup(any(), any(), any())
        }
    }

    @Test
    fun `given conversation is created, when federated users are selected and guests are disabled, then set the correct state`() = runTest {

        val usersSelected = persistentSetOf(
            Contact(
                "id",
                "domain",
                "name",
                "handle",
                UserAvatarData(),
                label = "label",
                connectionState = ConnectionState.ACCEPTED,
                membership = Membership.Federated
            )
        )

        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .arrange()

        viewModel.newGroupState = viewModel.newGroupState.copy(
            selectedUsers = usersSelected
        )

        viewModel.groupOptionsState = viewModel.groupOptionsState.copy(isAllowGuestEnabled = false)

        viewModel.createGroup { _ -> fail("group should not be created") }

        assertTrue(viewModel.groupOptionsState.showAllowGuestsDialog)

        coVerify(exactly = 0) {
            arrangement.createRegularGroup(any(), any(), any())
        }
    }

    @Test
    fun `given valid data, when createChannel is called, then it creates the channel and invokes onCreated`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withCreateChannelSuccess()
            .arrange()
        var isInvoked = false

        // When
        viewModel.createChannel(onCreated = { isInvoked = true })

        // Then
        assertEquals(true, isInvoked)
    }

    @Test
    fun `given createChannel fails when createChannel is called then it does not invoke onCreated`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withCreateChannelFailure()
            .arrange()
        var isInvoked = false

        // When
        viewModel.createChannel(onCreated = { isInvoked = true })

        // Then
        assertEquals(false, isInvoked)
    }
}
