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

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.assertions.shouldBeInstanceOf
import com.wire.android.assertions.shouldNotBeEqualTo
import com.wire.android.assertions.shouldNotBeInstanceOf
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.common.groupname.GroupMetadataState
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.home.newconversation.NewConversationViewModelArrangement.Companion.CONVERSATION
import com.wire.android.ui.home.newconversation.common.CreateGroupState
import com.wire.android.ui.home.newconversation.model.Contact
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.CreateConversationParam
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.channels.ChannelCreationPermission
import io.mockk.coVerify
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.createGroupState shouldBeEqualTo CreateGroupState.Error.LackingConnection
    }

    @Test
    fun `given unknown failure, when creating group, then should update options state with unknown error`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withUnknownFailureOnCreatingGroup()
            .arrange()

        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.createGroupState shouldBeEqualTo CreateGroupState.Error.Unknown
    }

    @Test
    fun `given no failure, when creating group, then options state should have no error`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .arrange()

        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.createGroupState shouldNotBeInstanceOf CreateGroupState.Error::class
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
            viewModel.createGroupState shouldBeEqualTo CreateGroupState.Default
        }

    @Test
    fun `given self is not a team member, when creating group, then the group is created with the correct values`() = runTest {
        val (arrangement, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = false)
            .arrange()

        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.createGroupState shouldBeEqualTo CreateGroupState.Created(CONVERSATION.id)

        coVerify {
            arrangement.createRegularGroup(
                viewModel.newGroupNameTextState.text.toString(),
                viewModel.newGroupState.selectedUsers.map { contact -> UserId(contact.id, contact.domain) },
                CreateConversationParam(
                    access = Conversation.defaultGroupAccess,
                    accessRole = Conversation.defaultGroupAccessRoles,
                    readReceiptsEnabled = false,
                    wireCellEnabled = false,
                    protocol = CreateConversationParam.Protocol.PROTEUS,
                    creatorClientId = null
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
            viewModel.createGroup()
            advanceUntilIdle()

            viewModel.createGroupState shouldBeEqualTo CreateGroupState.Created(CONVERSATION.id)

            coVerify {
                arrangement.createRegularGroup(
                    viewModel.newGroupNameTextState.text.toString(),
                    viewModel.newGroupState.selectedUsers.map { contact -> UserId(contact.id, contact.domain) },
                    CreateConversationParam(
                        access = setOf(Conversation.Access.INVITE, Conversation.Access.CODE),
                        accessRole = setOf(
                            Conversation.AccessRole.TEAM_MEMBER,
                            Conversation.AccessRole.NON_TEAM_MEMBER,
                            Conversation.AccessRole.GUEST
                        ),
                        readReceiptsEnabled = true,
                        wireCellEnabled = false,
                        protocol = CreateConversationParam.Protocol.PROTEUS,
                        creatorClientId = null
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
        assertEquals(CreateConversationParam.Protocol.MLS, result)
        assertEquals(false, result2.isAllowAppsEnabled)
        assertEquals(false, result2.isTeamAllowedToUseApps)
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

        launch {
            viewModel.observeGroupNameChanges()
        }.also {
            viewModel.newGroupNameTextState.setTextAndPlaceCursorAtEnd(String.EMPTY)
            advanceUntilIdle()
            assertEquals(GroupMetadataState.NewGroupError.None, viewModel.newGroupState.error)

            viewModel.newGroupNameTextState.setTextAndPlaceCursorAtEnd("name")
            advanceUntilIdle()
            assertEquals(GroupMetadataState.NewGroupError.None, viewModel.newGroupState.error)

            viewModel.newGroupNameTextState.clearText()
            advanceUntilIdle()
            assertEquals(GroupMetadataState.NewGroupError.TextFieldError.GroupNameEmptyError, viewModel.newGroupState.error)
            it.cancel()
        }
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

        viewModel.createGroup()

        viewModel.createGroupState shouldBeEqualTo CreateGroupState.Default
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

        viewModel.createGroup()

        viewModel.createGroupState shouldBeEqualTo CreateGroupState.Default
        assertTrue(viewModel.groupOptionsState.showAllowGuestsDialog)

        coVerify(exactly = 0) {
            arrangement.createRegularGroup(any(), any(), any())
        }
    }

    @Test
    fun `given createChannel success, when creating channel, then state should be changed to Created`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withCreateChannelSuccess()
            .arrange()

        // When
        viewModel.createChannel()

        // Then
        viewModel.createGroupState shouldBeEqualTo CreateGroupState.Created(CONVERSATION.id)
    }

    @Test
    fun `given createChannel failure, when creating channel, then state is not changed to Created but to Error`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withCreateChannelFailure()
            .arrange()

        // When
        viewModel.createChannel()

        // Then
        viewModel.createGroupState shouldNotBeEqualTo CreateGroupState.Created(CONVERSATION.id)
        viewModel.createGroupState shouldBeInstanceOf CreateGroupState.Error::class
    }

    @Test
    fun `given user is allowed to create channel, when initializing viewModel, then state should reflect that`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withChannelCreationPermissionReturning(flowOf(ChannelCreationPermission.Allowed(false)))
            .arrange()

        assertTrue(viewModel.isChannelCreationPossible)
    }

    @Test
    fun `given user is NOT allowed to create channel, when initializing viewModel, then state should reflect that`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withChannelCreationPermissionReturning(flowOf(ChannelCreationPermission.Forbidden))
            .arrange()

        assertFalse(viewModel.isChannelCreationPossible)
    }

    @Test
    fun `given apps are not allowed, when initializing, then state should ignore the feature flag and based on protocol`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withAppsAllowedResult(false)
            .arrange()

        assertTrue(viewModel.groupOptionsState.isTeamAllowedToUseApps)
        assertTrue(viewModel.groupOptionsState.isAllowAppsEnabled)
    }

    @Test
    fun `given apps are allowed, when initializing viewModel, then state should reflect that`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withAppsAllowedResult(true)
            .arrange()

        assertTrue(viewModel.groupOptionsState.isTeamAllowedToUseApps)
        assertTrue(viewModel.groupOptionsState.isAllowAppsEnabled)
    }

    @Test
    fun `given state is reset, when reset called, then state should reflect that`() = runTest {
        // Given
        val (_, viewModel) = NewConversationViewModelArrangement()
            .withGetSelfUser(isTeamMember = true)
            .withAppsAllowedResult(true)
            .arrange()

        // dirty state
        viewModel.newGroupNameTextState = TextFieldState("Test Group")
        viewModel.groupOptionsState = viewModel.groupOptionsState.copy(
            isTeamAllowedToUseApps = true,
            isAllowAppsEnabled = false
        )

        // When
        viewModel.resetState()

        assertTrue(viewModel.groupOptionsState.isTeamAllowedToUseApps)
        assertTrue(viewModel.groupOptionsState.isAllowAppsEnabled)
        assertEquals("", viewModel.newGroupNameTextState.text)
    }
}
