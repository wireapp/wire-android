package com.wire.android.ui.userprofile.other

import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.userprofile.common.UsernameMapper
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreFailure.Unknown
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.PlainId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
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
class OtherUserProfileScreenViewModelTest {

    @Test
    fun `given a userId, when sending a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withSendConnectionRequest(SendConnectionRequestResult.Success)
                .arrange()
            viewModel.infoMessage.test {

                // when
                expectNoEvents()
                viewModel.onSendConnectionRequest()

                // then
                coVerify { arrangement.sendConnectionRequest(eq(USER_ID)) }
                assertEquals(ConnectionState.SENT, viewModel.state.connectionState)
                assertEquals(InfoMessageType.SuccessConnectionSentRequest.uiText, awaitItem())
            }
        }

    @Test
    fun `given a userId, when sending a connection request a fails, then returns a Failure result and show error message`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withSendConnectionRequest(SendConnectionRequestResult.Failure(Unknown(RuntimeException("some error"))))
                .arrange()
            viewModel.infoMessage.test {

                // when
                expectNoEvents()
                viewModel.onSendConnectionRequest()

                // then
                coVerify {
                    arrangement.sendConnectionRequest(eq(USER_ID))
                    arrangement.navigationManager wasNot Called
                }
                assertEquals(InfoMessageType.ConnectionRequestError.uiText, awaitItem())
            }
        }

    @Test
    fun `given a userId, when ignoring a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withIgnoreConnectionRequest(IgnoreConnectionRequestUseCaseResult.Success)
                .arrange()

            // when
            viewModel.onIgnoreConnectionRequest()

            // then
            coVerify { arrangement.ignoreConnectionRequest(eq(USER_ID)) }
            assertEquals(ConnectionState.NOT_CONNECTED, viewModel.state.connectionState)
        }

    @Test
    fun `given a userId, when canceling a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withCancelConnectionRequest(CancelConnectionRequestUseCaseResult.Success)
                .arrange()
            viewModel.infoMessage.test {

                // when
                expectNoEvents()
                viewModel.onCancelConnectionRequest()

                // then
                coVerify { arrangement.cancelConnectionRequest(eq(USER_ID)) }
                assertEquals(ConnectionState.NOT_CONNECTED, viewModel.state.connectionState)
                assertEquals(InfoMessageType.SuccessConnectionCancelRequest.uiText, awaitItem())
            }
        }

    @Test
    fun `given a userId, when accepting a connection request, then returns a Success result and update view state`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withAcceptConnectionRequest(AcceptConnectionRequestUseCaseResult.Success)
                .arrange()
            viewModel.infoMessage.test {

                // when
                expectNoEvents()
                viewModel.onAcceptConnectionRequest()

                // then
                coVerify { arrangement.acceptConnectionRequest(eq(USER_ID)) }
                assertEquals(ConnectionState.ACCEPTED, viewModel.state.connectionState)
                assertEquals(InfoMessageType.SuccessConnectionAcceptRequest.uiText, awaitItem())
            }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation, then returns a Success result with the conversation`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withGetOneToOneConversation(CreateConversationResult.Success(CONVERSATION))
                .arrange()

            // when
            viewModel.onOpenConversation()

            // then
            coVerify {
                arrangement.getOrCreateOneToOneConversation(USER_ID)
                arrangement.navigationManager.navigate(any())
            }
        }

    @Test
    fun `given a conversationId, when trying to open the conversation and fails, then returns a Failure result and update error state`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withGetOneToOneConversation(CreateConversationResult.Failure(Unknown(RuntimeException("some error"))))
                .arrange()

            // when
            viewModel.onOpenConversation()

            // then
            coVerify {
                arrangement.getOrCreateOneToOneConversation(USER_ID)
                arrangement.navigationManager wasNot Called
            }
        }

    @Test
    fun `given a navigation case, when going back requested, then should delegate call to manager navigateBack`() = runTest {
        val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
            .arrange()
        viewModel.navigateBack()

        coVerify(exactly = 1) { arrangement.navigationManager.navigateBack() }
    }

    @Test
    fun `given a group conversationId, when loading the data, then return group state`() =
        runTest {
            // given
            val expected = OtherUserProfileGroupState("some_name", Member.Role.Member, false, CONVERSATION_ID)
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withConversationIdInSavedState(CONVERSATION_ID.toString())
                .withGetOneToOneConversation(CreateConversationResult.Success(CONVERSATION))
                .arrange()

            // when
            val groupState = viewModel.state.groupState

            // then
            coVerify {
                arrangement.observeConversationRoleForUserUseCase(CONVERSATION_ID, USER_ID)
                arrangement.navigationManager wasNot Called
            }
            assertEquals(groupState, expected)
        }

    @Test
    fun `given no conversationId, when loading the data, then return null group state`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withConversationIdInSavedState(null)
                .arrange()

            // when
            val groupState = viewModel.state.groupState

            // then
            coVerify {
                arrangement.observeConversationRoleForUserUseCase(any(), any()) wasNot Called
                arrangement.navigationManager wasNot Called
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
                    arrangement.navigationManager wasNot Called
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
                    arrangement.navigationManager wasNot Called
                }
                assertEquals(InfoMessageType.ChangeGroupRoleError.uiText, awaitItem())
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
                viewModel.onBlockUser(USER_ID, "some name")

                // then
                coVerify { arrangement.blockUser(eq(USER_ID)) }
                assertEquals(InfoMessageType.BlockingUserOperationError.uiText, awaitItem())
                assertEquals(null, viewModel.blockUserDialogState)
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
                viewModel.onBlockUser(USER_ID, userName)

                // then
                coVerify { arrangement.blockUser(eq(USER_ID)) }
                assertEquals(
                    (awaitItem() as UIText.StringResource).resId,
                    (InfoMessageType.BlockingUserOperationSuccess(userName).uiText as UIText.StringResource).resId
                )
                assertEquals(null, viewModel.blockUserDialogState)
            }
        }

    @Test
    fun `given not connected user, then direct conversation is not requested`() = runTest {
        // given
        val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
            .arrange()

        // then
        coVerify {
            arrangement.getOrCreateOneToOneConversation wasNot Called
        }
    }

    @Test
    fun `given connected user, then direct conversation data is requested`() = runTest {
        // given
        val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
            .withUserInfo(
                GetUserInfoResult.Success(OTHER_USER.copy(connectionStatus = ConnectionState.ACCEPTED), TEAM)
            )
            .arrange()

        // then
        coVerify {
            arrangement.getOrCreateOneToOneConversation(USER_ID)
        }
    }

    @Test
    fun `given connected user AND direct conversation data error, then the other data is displayed`() = runTest {
        // given
        val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
            .withUserInfo(
                GetUserInfoResult.Success(OTHER_USER.copy(connectionStatus = ConnectionState.ACCEPTED), TEAM)
            )
            .withGetOneToOneConversation(CreateConversationResult.Failure(Unknown(RuntimeException("some error"))))
            .arrange()

        // then
        coVerify {
            arrangement.getOrCreateOneToOneConversation(USER_ID)
        }

        assertEquals(false, viewModel.state.isDataLoading)
        assertEquals(OTHER_USER.name.orEmpty(), viewModel.state.fullName)
        assertEquals(UsernameMapper.mapUserLabel(OTHER_USER), viewModel.state.userName)
        assertEquals(TEAM.name.orEmpty(), viewModel.state.teamName)
        assertEquals(OTHER_USER.email.orEmpty(), viewModel.state.email)
        assertEquals(OTHER_USER.phone.orEmpty(), viewModel.state.phone)
        assertEquals(ConnectionState.ACCEPTED, viewModel.state.connectionState)
        assertEquals(OTHER_USER.botService, viewModel.state.botService)
    }

    companion object {
        val USER_ID = UserId("some_value", "some_domain")
        val CONVERSATION_ID = ConversationId("some_value", "some_domain")
        val OTHER_USER = OtherUser(
            USER_ID,
            "some_name",
            "some_handle",
            "some_email",
            "some_phone",
            1,
            TeamId("some_team"),
            ConnectionState.NOT_CONNECTED,
            null,
            null,
            UserType.INTERNAL,
            UserAvailabilityStatus.AVAILABLE,
            null,
            false
        )
        val TEAM = Team("some_id", null)
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
            creatorId = PlainId("")
        )
        val CONVERSATION_ROLE_DATA = ConversationRoleData(
            "some_name",
            Member.Role.Member,
            Member.Role.Member,
            CONVERSATION_ID
        )
    }
}
