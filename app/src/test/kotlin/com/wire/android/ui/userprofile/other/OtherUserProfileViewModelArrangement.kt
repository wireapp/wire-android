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
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.e2ei.MLSClientIdentity
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
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
    lateinit var getOneToOneConversation: GetOneToOneConversationDetailsUseCase

    @MockK
    lateinit var observeUserInfo: ObserveUserInfoUseCase

    @MockK
    lateinit var observeConversationRoleForUserUseCase: ObserveConversationRoleForUserUseCase

    @MockK
    lateinit var userTypeMapper: UserTypeMapper

    @MockK
    lateinit var updateConversationMemberRoleUseCase: UpdateConversationMemberRoleUseCase

    @MockK
    lateinit var removeMemberFromConversationUseCase: RemoveMemberFromConversationUseCase

    @MockK
    lateinit var observeSelfUser: ObserveSelfUserUseCase

    @MockK
    lateinit var observeClientList: ObserveClientsByUserIdUseCase

    @MockK
    lateinit var fetchUsersClientsFromRemote: FetchUsersClientsFromRemoteUseCase

    @MockK
    lateinit var getUserE2eiCertificateStatus: IsOtherUserE2EIVerifiedUseCase

    @MockK
    lateinit var isOneToOneConversationCreated: IsOneToOneConversationCreatedUseCase

    @MockK
    lateinit var mlsClientIdentity: GetMLSClientIdentityUseCase

    @MockK
    lateinit var mlsIdentity: MLSClientIdentity

    @MockK
    lateinit var isE2EIEnabled: IsE2EIEnabledUseCase

    private val viewModel by lazy {
        OtherUserProfileScreenViewModel(
            TestDispatcherProvider(),
            observeUserInfo,
            userTypeMapper,
            observeConversationRoleForUserUseCase,
            removeMemberFromConversationUseCase,
            updateConversationMemberRoleUseCase,
            observeClientList,
            fetchUsersClientsFromRemote,
            getUserE2eiCertificateStatus,
            isOneToOneConversationCreated,
            mlsClientIdentity,
            isE2EIEnabled,
            savedStateHandle,
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()

        every { savedStateHandle.navArgs<OtherUserProfileNavArgs>() } returns OtherUserProfileNavArgs(
            groupConversationId = CONVERSATION_ID,
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
        every { userTypeMapper.toMembership(any()) } returns Membership.None
        coEvery { getOneToOneConversation(USER_ID) } returns flowOf(
            GetOneToOneConversationDetailsUseCase.Result.Success(OtherUserProfileScreenViewModelTest.CONVERSATION_ONE_ONE)
        )
        coEvery { getUserE2eiCertificateStatus.invoke(any()) } returns true
        coEvery { mlsClientIdentity.invoke(any()) } returns mlsIdentity.right()
        coEvery { isOneToOneConversationCreated.invoke(any()) } returns true
        coEvery { isE2EIEnabled.invoke() } returns true
    }

    suspend fun withUpdateConversationMemberRole(result: UpdateConversationMemberRoleResult) = apply {
        coEvery { updateConversationMemberRoleUseCase(any(), any(), any()) } returns result
    }

    fun withConversationIdInSavedState(conversationId: ConversationId?) = apply {
        every { savedStateHandle.navArgs<OtherUserProfileNavArgs>() } returns OtherUserProfileNavArgs(
            userId = USER_ID,
            groupConversationId = conversationId
        )
    }

    fun withGetOneToOneConversation(result: GetOneToOneConversationDetailsUseCase.Result) = apply {
        coEvery { getOneToOneConversation(USER_ID) } returns flowOf(result)
    }

    suspend fun withUserInfo(result: GetUserInfoResult) = apply {
        coEvery { observeUserInfo(any()) } returns flowOf(result)
    }

    fun arrange() = this to viewModel
}
