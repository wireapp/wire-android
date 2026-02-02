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

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.BuildConfig
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.SyncConversationCodeUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.CanCreatePasswordProtectedLinksUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.GenerateGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.ObserveGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkResult
import com.wire.kalium.logic.feature.conversation.guestroomlink.RevokeGuestRoomLinkUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.guestroomlink.ObserveGuestRoomLinkFeatureFlagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
@Suppress("LongParameterList", "TooManyFunctions")
class EditGuestAccessViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val updateConversationAccessRole: UpdateConversationAccessRoleUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val generateGuestRoomLink: GenerateGuestRoomLinkUseCase,
    private val revokeGuestRoomLink: RevokeGuestRoomLinkUseCase,
    private val observeGuestRoomLink: ObserveGuestRoomLinkUseCase,
    private val observeGuestRoomLinkFeatureFlag: ObserveGuestRoomLinkFeatureFlagUseCase,
    private val canCreatePasswordProtectedLinks: CanCreatePasswordProtectedLinksUseCase,
    private val syncConversationCode: SyncConversationCodeUseCase,
    private val getDefaultProtocol: GetDefaultProtocolUseCase,
    private val selfUser: ObserveSelfUserUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editGuestAccessNavArgs: EditGuestAccessNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = editGuestAccessNavArgs.conversationId
    private val accessParams = editGuestAccessNavArgs.editGuessAccessParams

    var editGuestAccessState by mutableStateOf(
        EditGuestAccessState(
            isGuestAccessAllowed = accessParams.isGuestAccessAllowed,
            isServicesAccessAllowed = accessParams.isServicesAllowed,
            isUpdatingGuestAccessAllowed = accessParams.isUpdatingGuestAccessAllowed
        )
    )

    init {
        observeConversationDetails()
        startObservingGuestRoomLink()
        observeGuestRoomLinkFeature()
        checkIfUserCanCreatePasswordProtectedLinks()
    }

    private val syncCodeMutex = Mutex()

    private var isGuestCodeUpdated = false
    private fun updateConversationCodeOnce() {
        viewModelScope.launch {
            syncCodeMutex.withLock {
                if (!isGuestCodeUpdated) {
                    syncConversationCode(conversationId)
                    isGuestCodeUpdated = true
                }
            }
        }
    }

    private fun checkIfUserCanCreatePasswordProtectedLinks() {
        viewModelScope.launch {
            val canCreatePasswordProtectedLinks = when {
                !BuildConfig.IS_PASSWORD_PROTECTED_GUEST_LINK_ENABLED -> false
                else -> canCreatePasswordProtectedLinks()
            }
            editGuestAccessState = editGuestAccessState.copy(isPasswordProtectedLinksAllowed = canCreatePasswordProtectedLinks)
        }
    }

    private fun observeGuestRoomLinkFeature() {
        viewModelScope.launch {
            observeGuestRoomLinkFeatureFlag().collect {
                it.isGuestRoomLinkEnabled?.let { isEnabled ->
                    editGuestAccessState = editGuestAccessState.copy(isGuestRoomLinkFeatureEnabled = isEnabled)
                }
            }
        }
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            val conversationDetailsFlow = observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                .map { it.conversationDetails }
                .distinctUntilChanged()
                .flowOn(dispatcher.io())
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            val isSelfAdminFlow = observeConversationMembers(conversationId)
                .map { it.isSelfAnAdmin }
                .distinctUntilChanged()

            val isMLSTeam = getDefaultProtocol() == SupportedProtocol.MLS

            // TODO(refactor): Move all this logic to a UseCase
            combine(
                conversationDetailsFlow,
                isSelfAdminFlow,
                selfUser()
            ) { conversationDetails, isSelfAnAdmin, selfUser ->
                Triple(isSelfAnAdmin, conversationDetails, selfUser)
            }.collect { (isSelfAnAdmin, conversationDetails, selfUser) ->
                val isTeamAdmin = selfUser.userType.isTeamAdmin()
                val isSelfInConversationTeam = selfUser.teamId == conversationDetails.conversation.teamId
                val isSelfChannelTeamAdmin =
                    (conversationDetails is ConversationDetails.Group.Channel && isTeamAdmin && isSelfInConversationTeam)
                val canSelfPerformAdminActions = isSelfAnAdmin || isSelfChannelTeamAdmin
                val isGuestAllowed =
                    conversationDetails.conversation.isGuestAllowed() || conversationDetails.conversation.isNonTeamMemberAllowed()
                val isMLSConversation = conversationDetails.conversation.protocol is Conversation.ProtocolInfo.MLS

                editGuestAccessState = editGuestAccessState.copy(
                    isGuestAccessAllowed = isGuestAllowed,
                    isServicesAccessAllowed = conversationDetails.conversation.isServicesAllowed() && !isMLSTeam && !isMLSConversation,
                    isUpdatingGuestAccessAllowed = canSelfPerformAdminActions
                )

                if (isGuestAllowed) updateConversationCodeOnce()
            }
        }
    }

    private fun updateState(newState: EditGuestAccessState) {
        editGuestAccessState = newState
    }

    fun updateGuestAccess(shouldEnableGuestAccess: Boolean) {
        updateState(editGuestAccessState.copy(isUpdatingGuestAccess = true, isGuestAccessAllowed = shouldEnableGuestAccess))
        when (shouldEnableGuestAccess) {
            true -> updateGuestRemoteRequest(true)
            false -> updateState(editGuestAccessState.copy(shouldShowGuestAccessChangeConfirmationDialog = true))
        }
    }

    private fun updateGuestRemoteRequest(shouldEnableGuestAccess: Boolean) {

        val newAccessRoles = Conversation
            .accessRolesFor(
                guestAllowed = shouldEnableGuestAccess,
                servicesAllowed = editGuestAccessState.isServicesAccessAllowed,
                nonTeamMembersAllowed = shouldEnableGuestAccess // Guest access controls non-team member access
            )
        val newAccess = Conversation
            .accessFor(shouldEnableGuestAccess)

        viewModelScope.launch {
            withContext(dispatcher.io()) {
                updateConversationAccessRole(
                    conversationId = conversationId,
                    accessRoles = newAccessRoles,
                    access = newAccess
                )
            }.also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                        editGuestAccessState.copy(
                            isGuestAccessAllowed = !shouldEnableGuestAccess
                        )
                    )

                    is UpdateConversationAccessRoleUseCase.Result.Success -> updateState(
                        editGuestAccessState.copy(
                            isGuestAccessAllowed = shouldEnableGuestAccess
                        )
                    )
                }
                updateState(editGuestAccessState.copy(isUpdatingGuestAccess = false))
            }
        }
    }

    fun onRequestGuestRoomLink() {
        viewModelScope.launch {
            safeCreateGuestLink(null)
        }
    }

    private suspend fun safeCreateGuestLink(password: String?) {
        editGuestAccessState = editGuestAccessState.copy(isGeneratingGuestRoomLink = true)
        generateGuestRoomLink(conversationId, password).also {
            editGuestAccessState = editGuestAccessState.copy(isGeneratingGuestRoomLink = false)
            if (it is GenerateGuestRoomLinkResult.Failure) {
                editGuestAccessState = editGuestAccessState.copy(isFailedToGenerateGuestRoomLink = true)
            }
        }
    }

    fun onGenerateGuestRoomFailureDialogDismiss() {
        editGuestAccessState = editGuestAccessState.copy(isFailedToGenerateGuestRoomLink = false)
    }

    fun onGuestDialogDismiss() {
        updateState(
            editGuestAccessState.copy(
                shouldShowGuestAccessChangeConfirmationDialog = false,
                isUpdatingGuestAccess = false,
                isGuestAccessAllowed = !editGuestAccessState.isGuestAccessAllowed
            )
        )
    }

    fun onGuestDialogConfirm() {
        updateState(editGuestAccessState.copy(shouldShowGuestAccessChangeConfirmationDialog = false, isUpdatingGuestAccess = true))
        updateGuestRemoteRequest(false)
    }

    fun onRevokeGuestRoomLink() {
        editGuestAccessState = editGuestAccessState.copy(shouldShowRevokeLinkConfirmationDialog = true)
    }

    fun onRevokeDialogDismiss() {
        editGuestAccessState = editGuestAccessState.copy(shouldShowRevokeLinkConfirmationDialog = false)
    }

    fun removeGuestLink() {
        viewModelScope.launch {
            updateState(editGuestAccessState.copy(shouldShowRevokeLinkConfirmationDialog = false, isRevokingLink = true))
            revokeGuestRoomLink(conversationId).also {
                if (it is RevokeGuestRoomLinkResult.Failure) {
                    updateState(editGuestAccessState.copy(isFailedToRevokeGuestRoomLink = true))
                }
            }
            updateState(editGuestAccessState.copy(isRevokingLink = false))
        }
    }

    private fun startObservingGuestRoomLink() {
        viewModelScope.launch {
            observeGuestRoomLink(conversationId).collect { linkResult ->
                when (linkResult) {
                    is ObserveGuestRoomLinkResult.Failure -> appLogger.w("Failed to observe guest room link: ${linkResult.failure}")
                    is ObserveGuestRoomLinkResult.Success -> {
                        editGuestAccessState =
                            editGuestAccessState.copy(
                                link = linkResult.link?.link,
                                isLinkPasswordProtected = linkResult.link?.isPasswordProtected ?: false
                            )
                    }
                }
            }
        }
    }

    fun onRevokeGuestRoomFailureDialogDismiss() {
        editGuestAccessState = editGuestAccessState.copy(isFailedToRevokeGuestRoomLink = false)
    }

    fun shouldDisableGenerateGuestLinkButton() = !editGuestAccessState.isGuestRoomLinkFeatureEnabled ||
            !editGuestAccessState.isGuestAccessAllowed ||
            editGuestAccessState.isGeneratingGuestRoomLink
}
