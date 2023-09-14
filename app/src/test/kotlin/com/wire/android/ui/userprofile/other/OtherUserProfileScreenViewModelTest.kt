/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.userprofile.other

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.NavigationTestExtension
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import io.mockk.Called
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
@ExtendWith(NavigationTestExtension::class)
class OtherUserProfileScreenViewModelTest {

    @Test
    fun `given a group conversationId, when loading the data, then return group state`() =
        runTest {
            // given
            val expected = OtherUserProfileGroupState("some_name", Member.Role.Member, false, CONVERSATION_ID)
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withConversationIdInSavedState(CONVERSATION_ID)
                .withGetOneToOneConversation(GetOneToOneConversationUseCase.Result.Success(CONVERSATION))
                .arrange()

            // when
            val groupState = viewModel.state.groupState

            // then
            coVerify {
                arrangement.observeConversationRoleForUserUseCase(CONVERSATION_ID, USER_ID)
            }
            assertEquals(groupState, expected)
            assertEquals(viewModel.state.conversationId, CONVERSATION_ID)
        }

    @Test
    fun `given no conversationId, when loading the data, then return null group state`() = runTest {
        // given
        val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
            .withConversationIdInSavedState(null)
            .arrange()

        // when
        val groupState = viewModel.state.groupState

        // then
        coVerify {
            arrangement.observeConversationRoleForUserUseCase(any(), any()) wasNot Called
        }
        assertEquals(groupState, null)
    }

    @Test
    fun `given a group conversationId, when changing the role, then the request should be configured correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withUpdateConversationMemberRole(UpdateConversationMemberRoleResult.Success)
                .arrange()
            val newRole = Member.Role.Admin
            viewModel.infoMessage.test {

                // when
                expectNoEvents()
                viewModel.onChangeMemberRole(newRole)

                // then
                coVerify {
                    arrangement.updateConversationMemberRoleUseCase(CONVERSATION_ID, USER_ID, newRole)
                }
                expectNoEvents()
            }
        }

    @Test
    fun `given a group conversationId and a failed response when changing the role, then show info message`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withUpdateConversationMemberRole(UpdateConversationMemberRoleResult.Failure)
                .arrange()
            val newRole = Member.Role.Admin
            viewModel.infoMessage.test {

                // when
                expectNoEvents()
                viewModel.onChangeMemberRole(newRole)

                // then
                coVerify {
                    arrangement.updateConversationMemberRoleUseCase(CONVERSATION_ID, USER_ID, newRole)
                }
                assertEquals(OtherUserProfileInfoMessageType.ChangeGroupRoleError.uiText, awaitItem())
            }
        }

    @Test
    fun `given a userId, when blocking user fails, then show error message and dismiss BlockDialog`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withBlockUserResult(BlockUserResult.Failure(Unknown(RuntimeException("some error"))))
                .arrange()
            viewModel.infoMessage.test {

                // when
                expectNoEvents()
                viewModel.onBlockUser(
                    BlockUserDialogState(
                        userName = "some name",
                        userId = USER_ID
                    )
                )
                // then
                coVerify { arrangement.blockUser(eq(USER_ID)) }
                assertEquals(OtherUserProfileInfoMessageType.BlockingUserOperationError.uiText, awaitItem())
                assertEquals(false, viewModel.requestInProgress)
            }
        }

    @Test
    fun `given a userId, when blocking user is succeed, then show Success message and dismiss BlockDialog`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withBlockUserResult(BlockUserResult.Success)
                .arrange()
            viewModel.infoMessage.test {
                val userName = "some name"

                // when
                expectNoEvents()
                viewModel.onBlockUser(
                    BlockUserDialogState(
                        userName = userName,
                        userId = USER_ID
                    )
                )

                // then
                coVerify { arrangement.blockUser(eq(USER_ID)) }
                assertEquals(
                    (awaitItem() as UIText.StringResource).resId,
                    (OtherUserProfileInfoMessageType.BlockingUserOperationSuccess(userName).uiText as UIText.StringResource).resId
                )
                assertEquals(false, viewModel.requestInProgress)
            }
        }

    @Test
    fun `given not connected user, then direct conversation is not found`() = runTest {
        // given
        val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
            .withUserInfo(
                GetUserInfoResult.Success(OTHER_USER.copy(connectionStatus = ConnectionState.NOT_CONNECTED), TEAM)
            )
            .withGetOneToOneConversation(GetOneToOneConversationUseCase.Result.Failure)
            .arrange()

        // then
        assertEquals(null, viewModel.state.conversationSheetContent)
    }

    companion object {
        val USER_ID = UserId("some_value", "some_domain")
        val CONVERSATION_ID = ConversationId("some_value", "some_domain")
        val OTHER_USER = OtherUser(
            USER_ID,
            name = "some_name",
            handle = "some_handle",
            email = "some_email",
            phone = "some_phone",
            accentId = 1,
            teamId = TeamId("some_team"),
            connectionStatus = ConnectionState.NOT_CONNECTED,
            previewPicture = null,
            completePicture = null,
            userType = UserType.INTERNAL,
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            botService = null,
            deleted = false,
            defederated = false
        )
        val TEAM = Team("some_id", "name", "icon")
        val CONVERSATION = Conversation(
            id = CONVERSATION_ID,
            name = "some_name",
            type = Conversation.Type.ONE_ON_ONE,
            teamId = null,
            protocol = Conversation.ProtocolInfo.Proteus,
            mutedStatus = MutedConversationStatus.AllAllowed,
            removedBy = null,
            lastNotificationDate = null,
            lastModifiedDate = null,
            lastReadDate = "2022-04-04T16:11:28.388Z",
            access = listOf(Conversation.Access.INVITE),
            accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER),
            creatorId = null,
            receiptMode = Conversation.ReceiptMode.ENABLED,
            messageTimer = null,
            userMessageTimer = null,
            archived = false,
            archivedDateTime = null
        )
        val CONVERSATION_ROLE_DATA = ConversationRoleData(
            "some_name",
            Member.Role.Member,
            Member.Role.Member,
            CONVERSATION_ID
        )
    }
}
