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

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.ui.navArgs
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModelTest.Companion.CONVERSATION_ID
import com.wire.android.ui.userprofile.other.OtherUserProfileScreenViewModelTest.Companion.USER_ID
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ArchiveStatusUpdateResult
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.e2ei.CertificateStatus
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserE2eiCertificateStatusResult
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserE2eiCertificateStatusUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetUserE2eiCertificatesUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf

internal class OtherUserProfileViewModelArrangement {

    @MockK
    lateinit var savedStateHandle: SavedStateHandle

    @MockK
    lateinit var getOneToOneConversation: GetOneToOneConversationUseCase

    @MockK
    lateinit var observeUserInfo: ObserveUserInfoUseCase

    @MockK
    lateinit var wireSessionImageLoader: WireSessionImageLoader

    @MockK
    lateinit var observeConversationRoleForUserUseCase: ObserveConversationRoleForUserUseCase

    @MockK
    lateinit var userTypeMapper: UserTypeMapper

    @MockK
    lateinit var updateConversationMemberRoleUseCase: UpdateConversationMemberRoleUseCase

    @MockK
    lateinit var removeMemberFromConversationUseCase: RemoveMemberFromConversationUseCase

    @MockK
    lateinit var observeSelfUser: GetSelfUserUseCase

    @MockK
    lateinit var blockUser: BlockUserUseCase

    @MockK
    lateinit var unblockUser: UnblockUserUseCase

    @MockK
    lateinit var updateConversationMutedStatus: UpdateConversationMutedStatusUseCase

    @MockK
    lateinit var observeClientList: ObserveClientsByUserIdUseCase

    @MockK
    lateinit var fetchUsersClientsFromRemote: FetchUsersClientsFromRemoteUseCase

    @MockK
    lateinit var clearConversationContent: ClearConversationContentUseCase

    @MockK
    lateinit var updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase

    @MockK
    lateinit var getUserE2eiCertificateStatus: GetUserE2eiCertificateStatusUseCase

    @MockK
    lateinit var getUserE2eiCertificates: GetUserE2eiCertificatesUseCase

    @MockK
    lateinit var isOneToOneConversationCreated: IsOneToOneConversationCreatedUseCase

    private val viewModel by lazy {
        OtherUserProfileScreenViewModel(
            TestDispatcherProvider(),
            updateConversationMutedStatus,
            blockUser,
            unblockUser,
            getOneToOneConversation,
            observeUserInfo,
            userTypeMapper,
            wireSessionImageLoader,
            observeConversationRoleForUserUseCase,
            removeMemberFromConversationUseCase,
            updateConversationMemberRoleUseCase,
            observeClientList,
            fetchUsersClientsFromRemote,
            clearConversationContent,
            updateConversationArchivedStatus,
            getUserE2eiCertificateStatus,
            getUserE2eiCertificates,
            isOneToOneConversationCreated,
            savedStateHandle,
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()

        every { savedStateHandle.navArgs<OtherUserProfileNavArgs>() } returns OtherUserProfileNavArgs(
            conversationId = CONVERSATION_ID,
            userId = USER_ID
        )

        coEvery {
            observeConversationRoleForUserUseCase.invoke(any(), any())
        } returns flowOf(OtherUserProfileScreenViewModelTest.CONVERSATION_ROLE_DATA)
        coEvery { observeUserInfo(any()) } returns flowOf(
            GetUserInfoResult.Success(
                OtherUserProfileScreenViewModelTest.OTHER_USER,
                OtherUserProfileScreenViewModelTest.TEAM
            )
        )
        coEvery { observeSelfUser() } returns flowOf(TestUser.SELF_USER)
        coEvery { updateConversationArchivedStatus(any(), any(), any()) } returns ArchiveStatusUpdateResult.Success
        every { userTypeMapper.toMembership(any()) } returns Membership.None
        coEvery { getOneToOneConversation(USER_ID) } returns flowOf(
            GetOneToOneConversationUseCase.Result.Success(OtherUserProfileScreenViewModelTest.CONVERSATION)
        )
        coEvery { getUserE2eiCertificateStatus.invoke(any()) } returns GetUserE2eiCertificateStatusResult.Success(CertificateStatus.VALID)
        coEvery { getUserE2eiCertificates.invoke(any()) } returns mapOf()
        coEvery { isOneToOneConversationCreated.invoke(any()) } returns true
    }

    suspend fun withBlockUserResult(result: BlockUserResult) = apply {
        coEvery { blockUser(any()) } returns result
    }

    suspend fun withUpdateConversationMemberRole(result: UpdateConversationMemberRoleResult) = apply {
        coEvery { updateConversationMemberRoleUseCase(any(), any(), any()) } returns result
    }

    fun withConversationIdInSavedState(conversationId: ConversationId?) = apply {
        every { savedStateHandle.navArgs<OtherUserProfileNavArgs>() } returns OtherUserProfileNavArgs(
            userId = USER_ID,
            conversationId = conversationId
        )
    }

    fun withGetOneToOneConversation(result: GetOneToOneConversationUseCase.Result) = apply {
        coEvery { getOneToOneConversation(USER_ID) } returns flowOf(result)
    }

    suspend fun withUserInfo(result: GetUserInfoResult) = apply {
        coEvery { observeUserInfo(any()) } returns flowOf(result)
    }

    fun arrange() = this to viewModel
}
