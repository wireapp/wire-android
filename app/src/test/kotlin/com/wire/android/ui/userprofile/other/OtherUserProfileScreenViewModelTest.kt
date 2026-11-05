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

package com.wire.android.ui.userprofile.other

import app.cash.turbine.test
import com.wire.android.assertIs
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.ContactStatus
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.feature.conversation.CheckOneToOneConversationIsReadyUseCase
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class OtherUserProfileScreenViewModelTest {

    @Test
    fun `cached profile renders while status lookup is pending`() = runTest {
        val arrangement = OtherUserProfileViewModelArrangement()
        val response = CompletableDeferred<Map<UserId, ContactStatus?>>()
        coEvery { arrangement.refreshContactStatuses(setOf(USER_ID)) } coAnswers { response.await() }
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(OTHER_USER.name, viewModel.state.fullName)
        assertEquals(false, viewModel.state.isDataLoading)
        assertEquals(false, viewModel.state.isOnboardingPending)
        response.complete(mapOf(USER_ID to ContactStatus.NON_CONTACTABLE))
        advanceUntilIdle()
        assertEquals(true, viewModel.state.isOnboardingPending)
        coVerify(exactly = 1) { arrangement.refreshContactStatuses(setOf(USER_ID)) }
    }

    @Test
    fun `profile loading failure is independent of status lookup`() = runTest {
        val arrangement = OtherUserProfileViewModelArrangement().withUserInfo(GetUserInfoResult.Failure)
        val response = CompletableDeferred<Map<UserId, ContactStatus?>>()
        coEvery { arrangement.refreshContactStatuses(any()) } coAnswers { response.await() }
        val (_, viewModel) = arrangement.arrange()
        advanceUntilIdle()
        assertEquals(ErrorLoadingUser.USER_NOT_FOUND, viewModel.state.errorLoadingUser)
        assertEquals(false, viewModel.state.isDataLoading)
        response.complete(emptyMap())
        advanceUntilIdle()
        assertEquals(ErrorLoadingUser.USER_NOT_FOUND, viewModel.state.errorLoadingUser)
        coVerify(exactly = 1) { arrangement.observeUserInfo(USER_ID) }
    }

    @Test
    fun `obsolete status response cannot overwrite a foreground refresh`() = runTest {
        val arrangement = OtherUserProfileViewModelArrangement()
        val oldResponse = CompletableDeferred<Map<UserId, ContactStatus?>>()
        coEvery { arrangement.refreshContactStatuses(any()) } coAnswers {
            withContext(NonCancellable) { oldResponse.await() }
        }
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        viewModel.onBackground()
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(USER_ID to ContactStatus.CONTACTABLE)
        viewModel.onForeground()
        advanceUntilIdle()
        oldResponse.complete(mapOf(USER_ID to ContactStatus.NON_CONTACTABLE))
        advanceUntilIdle()
        assertEquals(ContactStatus.CONTACTABLE, viewModel.state.contactStatus)
        coVerify(exactly = 2) { arrangement.refreshContactStatuses(setOf(USER_ID)) }
    }

    @Test
    fun `disabled feature ignores pending data and keeps ordinary missing user loading`() = runTest {
        val arrangement = OtherUserProfileViewModelArrangement().apply { pendingConnectionsEnabled = false }
            .withUserInfo(GetUserInfoResult.Success(OTHER_USER.copy(contactStatus = ContactStatus.NON_CONTACTABLE), null))
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        viewModel.onBackground()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.isOnboardingPending)
        assertEquals(OTHER_USER.name, viewModel.state.fullName)
        coVerify(exactly = 0) { arrangement.refreshContactStatuses(any()) }
        coVerify(exactly = 1) { arrangement.observeUserInfo(USER_ID) }
    }

    @Test
    fun `failed refresh clears pending while retaining cached profile`() = runTest {
        val arrangement = OtherUserProfileViewModelArrangement()
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(USER_ID to ContactStatus.NON_CONTACTABLE)
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        val name = viewModel.state.fullName
        assertEquals(true, viewModel.state.isOnboardingPending)
        coEvery { arrangement.refreshContactStatuses(any()) } returns emptyMap()
        viewModel.onBackground()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.isOnboardingPending)
        assertEquals(name, viewModel.state.fullName)
        assertEquals(null, viewModel.state.errorLoadingUser)
    }

    @Test
    fun `profile does not restore a pending status from observed user data`() = runTest {
        val arrangement = OtherUserProfileViewModelArrangement().withUserInfo(
            GetUserInfoResult.Success(OTHER_USER.copy(contactStatus = ContactStatus.NON_CONTACTABLE), null)
        )
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(USER_ID to null)
        val (_, viewModel) = arrangement.arrange()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.isOnboardingPending)
    }

    @Test
    fun `pending status comes from refresh and foreground clears stale status`() = runTest {
        val arrangement = OtherUserProfileViewModelArrangement()
        val updates = MutableStateFlow<GetUserInfoResult>(GetUserInfoResult.Success(OTHER_USER, null))
        coEvery { arrangement.observeUserInfo(any()) } returns updates
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(USER_ID to ContactStatus.NON_CONTACTABLE)
        val (_, viewModel) = arrangement.arrange()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(true, viewModel.state.isOnboardingPending)
        updates.value = GetUserInfoResult.Success(OTHER_USER.copy(name = "Updated name"), null)
        advanceUntilIdle()
        assertEquals(true, viewModel.state.isOnboardingPending)
        coVerify(exactly = 1) { arrangement.refreshContactStatuses(setOf(USER_ID)) }
        coEvery { arrangement.refreshContactStatuses(any()) } returns mapOf(USER_ID to ContactStatus.CONTACTABLE)
        viewModel.onBackground()
        viewModel.onForeground()
        advanceUntilIdle()
        assertEquals(false, viewModel.state.isOnboardingPending)
        coVerify(exactly = 2) { arrangement.refreshContactStatuses(setOf(USER_ID)) }
        coVerify(exactly = 1) { arrangement.observeUserInfo(USER_ID) }
    }

    @Test
    fun `given a group conversationId, when loading the data, then return group state`() =
        runTest {
            // given
            val expected = OtherUserProfileGroupState("some_name", Member.Role.Member, false, CONVERSATION_ID)
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withConversationIdInSavedState(CONVERSATION_ID)
                .withGetOneToOneConversation(GetOneToOneConversationDetailsUseCase.Result.Success(CONVERSATION_ONE_ONE))
                .arrange()

            // when
            val groupState = viewModel.state.groupState

            // then
            coVerify {
                arrangement.observeConversationRoleForUserUseCase(CONVERSATION_ID, USER_ID)
            }
            assertEquals(groupState, expected)
            assertEquals(viewModel.state.groupConversationId, CONVERSATION_ID)
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
        coVerify(exactly = 0) {
            arrangement.observeConversationRoleForUserUseCase(any(), any())
        }
        assertEquals(groupState, null)
    }

    @Test
    fun `given one-to-one conversation is ready, when loading profile, then conversation is started`() = runTest {
        val (_, viewModel) = OtherUserProfileViewModelArrangement()
            .withConversationReadiness(CheckOneToOneConversationIsReadyUseCase.Result.Ready(CONVERSATION))
            .arrange()

        assertEquals(true, viewModel.state.isConversationStarted)
    }

    @Test
    fun `given one-to-one conversation is not ready, when loading profile, then conversation is not started`() = runTest {
        val (_, viewModel) = OtherUserProfileViewModelArrangement()
            .withConversationReadiness(CheckOneToOneConversationIsReadyUseCase.Result.NotReady)
            .arrange()

        assertEquals(false, viewModel.state.isConversationStarted)
    }

    @Test
    fun `given one-to-one readiness check fails, when loading profile, then conversation is not started`() = runTest {
        val (_, viewModel) = OtherUserProfileViewModelArrangement()
            .withConversationReadiness(
                CheckOneToOneConversationIsReadyUseCase.Result.Failure(CoreFailure.Unknown(null))
            )
            .arrange()

        assertEquals(false, viewModel.state.isConversationStarted)
    }

    @Test
    fun `given a group conversationId, when changing the role, then the request should be configured correctly`() =
        runTest {
            // given
            val (arrangement, viewModel) = OtherUserProfileViewModelArrangement()
                .withUpdateConversationMemberRole(UpdateConversationMemberRoleResult.Success)
                .arrange()
            val newRole = Member.Role.Admin
            viewModel.actions.test {

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
            viewModel.actions.test {

                // when
                expectNoEvents()
                viewModel.onChangeMemberRole(newRole)

                // then
                coVerify {
                    arrangement.updateConversationMemberRoleUseCase(CONVERSATION_ID, USER_ID, newRole)
                }
                assertIs<OtherUserProfileViewAction.Message>(awaitItem()).also {
                    assertIs<OtherUserProfileInfoMessageType.ChangeGroupRoleError>(it.message)
                }
            }
        }

    @Test
    fun `given legal hold enabled, then isUnderLegalHold is true`() = runTest {
        // given
        val (_, viewModel) = OtherUserProfileViewModelArrangement()
            .withUserInfo(
                GetUserInfoResult.Success(
                    otherUser = OTHER_USER.copy(
                        connectionStatus = ConnectionState.NOT_CONNECTED,
                        isUnderLegalHold = true
                    ),
                    team = TEAM
                )
            )
            .arrange()
        // then
        assertEquals(true, viewModel.state.isUnderLegalHold)
    }

    @Test
    fun `given legal hold disabled, then isUnderLegalHold is false`() = runTest {
        // given
        val (_, viewModel) = OtherUserProfileViewModelArrangement()
            .withUserInfo(
                GetUserInfoResult.Success(
                    otherUser = OTHER_USER.copy(
                        connectionStatus = ConnectionState.NOT_CONNECTED,
                        isUnderLegalHold = false
                    ),
                    team = TEAM
                )
            )
            .arrange()
        // then
        assertEquals(false, viewModel.state.isUnderLegalHold)
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
            userType = UserTypeInfo.Regular(UserType.INTERNAL),
            availabilityStatus = UserAvailabilityStatus.AVAILABLE,
            botService = null,
            deleted = false,
            defederated = false,
            isProteusVerified = false,
            supportedProtocols = setOf(SupportedProtocol.PROTEUS)
        )
        val TEAM = Team("some_id", "name", "icon")

        val CONVERSATION = Conversation(
            id = CONVERSATION_ID,
            name = "some_name",
            type = Conversation.Type.OneOnOne,
            teamId = null,
            protocol = Conversation.ProtocolInfo.Proteus,
            mutedStatus = MutedConversationStatus.AllAllowed,
            removedBy = null,
            lastNotificationDate = null,
            lastModifiedDate = null,
            lastReadDate = Instant.parse("2022-04-04T16:11:28.388Z"),
            access = listOf(Conversation.Access.INVITE),
            accessRole = listOf(Conversation.AccessRole.NON_TEAM_MEMBER),
            creatorId = null,
            receiptMode = Conversation.ReceiptMode.ENABLED,
            messageTimer = null,
            userMessageTimer = null,
            archived = false,
            archivedDateTime = null,
            mlsVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            proteusVerificationStatus = Conversation.VerificationStatus.NOT_VERIFIED,
            legalHoldStatus = Conversation.LegalHoldStatus.DISABLED
        )

        val CONVERSATION_ONE_ONE = ConversationDetails.OneOne(
            CONVERSATION,
            OTHER_USER,
            UserTypeInfo.Regular(UserType.EXTERNAL),
        )

        val CONVERSATION_ROLE_DATA = ConversationRoleData(
            "some_name",
            Member.Role.Member,
            Member.Role.Member,
            CONVERSATION_ID
        )
    }
}
