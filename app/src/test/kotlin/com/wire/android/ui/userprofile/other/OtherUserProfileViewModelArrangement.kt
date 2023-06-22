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

import androidx.lifecycle.SavedStateHandle
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestUser
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.client.PersistOtherUserClientsUseCase
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetOtherUserSecurityClassificationLabelUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
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
    lateinit var navigationManager: NavigationManager

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
    lateinit var qualifiedIdMapper: QualifiedIdMapper

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
    lateinit var persistOtherUserClientsUseCase: PersistOtherUserClientsUseCase

    @MockK
    lateinit var clearConversationContent: ClearConversationContentUseCase

    @MockK
    lateinit var getOtherUserSecurityClassificationLabel: GetOtherUserSecurityClassificationLabelUseCase

    private val viewModel by lazy {
        OtherUserProfileScreenViewModel(
            navigationManager,
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
            persistOtherUserClientsUseCase,
            clearConversationContent,
            getOtherUserSecurityClassificationLabel,
            savedStateHandle,
            qualifiedIdMapper
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockUri()
        every { savedStateHandle.get<String>(EXTRA_USER_ID) } returns OtherUserProfileScreenViewModelTest.CONVERSATION_ID.toString()
        every { savedStateHandle.get<String>(EXTRA_CONVERSATION_ID) } returns
                OtherUserProfileScreenViewModelTest.CONVERSATION_ID.toString()
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
        coEvery {
            qualifiedIdMapper.fromStringToQualifiedID("some_value@some_domain")
        } returns QualifiedID("some_value", "some_domain")
        coEvery { getOneToOneConversation(OtherUserProfileScreenViewModelTest.USER_ID) } returns flowOf(
            GetOneToOneConversationUseCase.Result.Success(OtherUserProfileScreenViewModelTest.CONVERSATION)
        )
        coEvery { navigationManager.navigate(command = any()) } returns Unit
        coEvery { getOtherUserSecurityClassificationLabel(any()) } returns SecurityClassificationType.NONE
    }

    suspend fun withBlockUserResult(result: BlockUserResult) = apply {
        coEvery { blockUser(any()) } returns result
    }

    suspend fun withUpdateConversationMemberRole(result: UpdateConversationMemberRoleResult) = apply {
        coEvery { updateConversationMemberRoleUseCase(any(), any(), any()) } returns result
    }

    fun withConversationIdInSavedState(conversationIdString: String?) = apply {
        every { savedStateHandle.get<String>(eq(EXTRA_CONVERSATION_ID)) } returns conversationIdString
    }

    fun withGetOneToOneConversation(result: GetOneToOneConversationUseCase.Result) = apply {
        coEvery { getOneToOneConversation(OtherUserProfileScreenViewModelTest.USER_ID) } returns flowOf(result)
    }

    suspend fun withUserInfo(result: GetUserInfoResult) = apply {
        coEvery { observeUserInfo(any()) } returns flowOf(result)
    }

    fun arrange() = this to viewModel
}
