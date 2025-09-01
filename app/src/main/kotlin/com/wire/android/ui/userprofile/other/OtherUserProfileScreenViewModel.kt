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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset
import com.wire.android.model.SnackBarMessage
import com.wire.android.ui.authentication.devices.model.Device
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.common.visbility.VisibilityState
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.navArgs
import com.wire.android.ui.userprofile.common.UsernameMapper.fromOtherUser
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.ChangeGroupRoleError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.RemoveConversationMemberError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.RemoveConversationMemberSuccess
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.common.functional.getOrNull
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.client.FetchUsersClientsFromRemoteUseCase
import com.wire.kalium.logic.feature.client.ObserveClientsByUserIdUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.GetMLSClientIdentityUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.IsOtherUserE2EIVerifiedUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.IsE2EIEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    private val observeClientList: ObserveClientsByUserIdUseCase,
    private val fetchUsersClients: FetchUsersClientsFromRemoteUseCase,
    private val getUserE2eiCertificateStatus: IsOtherUserE2EIVerifiedUseCase,
    private val isOneToOneConversationCreated: IsOneToOneConversationCreatedUseCase,
    private val mlsClientIdentity: GetMLSClientIdentityUseCase,
    private val isE2EIEnabled: IsE2EIEnabledUseCase,
    savedStateHandle: SavedStateHandle
) : ActionsViewModel<OtherUserProfileViewAction>(), OtherUserProfileEventsHandler {

    private val otherUserProfileNavArgs: OtherUserProfileNavArgs = savedStateHandle.navArgs()
    private val userId: QualifiedID = otherUserProfileNavArgs.userId
    private val groupConversationId: QualifiedID? = otherUserProfileNavArgs.groupConversationId

    var state: OtherUserProfileState by mutableStateOf(
        OtherUserProfileState(
            userId = userId,
            groupConversationId = groupConversationId,
            isDataLoading = true,
            isAvatarLoading = true
        )
    )
    val removeConversationMemberDialogState: VisibilityState<RemoveConversationMemberState> by mutableStateOf(VisibilityState())

    init {
        observeUserInfoAndUpdateViewState()
        persistClients()
        getMLSVerificationStatus()
        getIfConversationExist()
        getE2EIStatus()
    }

    private fun getIfConversationExist() {
        viewModelScope.launch {
            val isOneToOneConversationCreated = isOneToOneConversationCreated(userId)
            state = state.copy(isConversationStarted = isOneToOneConversationCreated)
        }
    }

    private fun getMLSVerificationStatus() {
        viewModelScope.launch {
            val isMLSVerified = getUserE2eiCertificateStatus(userId)
            state = state.copy(isMLSVerified = isMLSVerified)
        }
    }

    private fun getE2EIStatus() = viewModelScope.launch {
        val isE2EIEnabled = isE2EIEnabled()
        state = state.copy(isE2EIEnabled = isE2EIEnabled)
    }

    override fun observeClientList() {
        viewModelScope.launch(dispatchers.io()) {
            observeClientList(userId)
                .collect { result ->
                    when (result) {
                        is ObserveClientsByUserIdUseCase.Result.Failure -> {
                            state = state.copy(otherUserDevices = emptyList())
                        }

                        is ObserveClientsByUserIdUseCase.Result.Success -> {
                            val devices = result.clients.map { client ->
                                async {
                                    Device(
                                        client = client,
                                        mlsClientIdentity = mlsClientIdentity(client.id).getOrNull()
                                    )
                                }
                            }.awaitAll()

                            state = state.copy(otherUserDevices = devices)
                        }
                    }
                }
        }
    }

    private fun persistClients() {
        viewModelScope.launch(dispatchers.io()) {
            fetchUsersClients(listOf(userId))
        }
    }

    private fun observeUserInfoAndUpdateViewState() {
        viewModelScope.launch {
            combine(
                observeUserInfo(userId),
                observeGroupInfo(),
                ::Pair
            )
                .flowOn(dispatchers.io())
                .collect { (userResult, groupInfo) ->
                    when (userResult) {
                        is GetUserInfoResult.Failure -> {
                            appLogger.e("Couldn't not find the user with provided id: ${userId.toLogString()}")
                            updateUserInfoStateForError()
                        }

                        is GetUserInfoResult.Success -> {
                            updateUserInfoState(userResult, groupInfo)
                        }
                    }
                }
        }
    }

    private suspend fun observeGroupInfo(): Flow<OtherUserProfileGroupState?> {
        return groupConversationId?.let {
            observeConversationRoleForUser(it, userId)
                .map { conversationRoleData ->
                    conversationRoleData.userRole?.let { userRole ->
                        OtherUserProfileGroupState(
                            groupName = conversationRoleData.conversationName,
                            role = userRole,
                            isSelfAdmin = conversationRoleData.selfRole is Conversation.Member.Role.Admin,
                            conversationId = conversationRoleData.conversationId,
                        )
                    }
                }
        } ?: flowOf(null)
    }

    fun onChangeMemberRole(role: Conversation.Member.Role) {
        viewModelScope.launch {
            if (groupConversationId != null) {
                updateMemberRole(groupConversationId, userId, role).also {
                    if (it is UpdateConversationMemberRoleResult.Failure) {
                        onMessage(ChangeGroupRoleError)
                    }
                }
            }
        }
    }

    override fun onRemoveConversationMember(state: RemoveConversationMemberState) {
        viewModelScope.launch {
            removeConversationMemberDialogState.update { it.copy(loading = true) }
            val response = withContext(dispatchers.io()) {
                removeMemberFromConversation(
                    state.conversationId,
                    userId
                )
            }
            when (response) {
                is RemoveMemberFromConversationUseCase.Result.Failure -> onMessage(RemoveConversationMemberError)
                is RemoveMemberFromConversationUseCase.Result.Success -> onMessage(RemoveConversationMemberSuccess(state.userName))
            }
            removeConversationMemberDialogState.dismiss()
        }
    }

    private fun updateUserInfoStateForError() {
        state = state.copy(
            isDataLoading = false,
            isAvatarLoading = false,
            errorLoadingUser = ErrorLoadingUser.USER_NOT_FOUND
        )
    }

    private fun updateUserInfoState(
        userResult: GetUserInfoResult.Success,
        groupInfo: OtherUserProfileGroupState?,
    ) {
        val otherUser = userResult.otherUser
        val userAvatarAsset = otherUser.completePicture
            ?.let { pic -> ImageAsset.UserAvatarAsset(pic) }

        state = state.copy(
            isDataLoading = false,
            isAvatarLoading = false,
            userAvatarAsset = userAvatarAsset,
            fullName = otherUser.name.orEmpty(),
            userName = fromOtherUser(otherUser),
            teamName = userResult.team?.name.orEmpty(),
            email = otherUser.email.orEmpty(),
            phone = otherUser.phone.orEmpty(),
            connectionState = otherUser.connectionStatus,
            membership = userTypeMapper.toMembership(otherUser.userType),
            groupState = groupInfo,
            botService = otherUser.botService,
            blockingState = otherUser.BlockState,
            isProteusVerified = otherUser.isProteusVerified,
            isUnderLegalHold = otherUser.isUnderLegalHold,
            expiresAt = otherUser.expiresAt,
            accentId = otherUser.accentId,
            isDeletedUser = otherUser.deleted,
            activeOneOnOneConversationId = otherUser.activeOneOnOneConversationId
        )
    }

    private fun onMessage(message: SnackBarMessage) = sendAction(OtherUserProfileViewAction.Message(message))
}

sealed interface OtherUserProfileViewAction {
    data class Message(val message: SnackBarMessage) : OtherUserProfileViewAction
}
