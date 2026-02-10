/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.details.updateappsaccess

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.util.debug.FeatureVisibilityFlags
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class UpdateAppsAccessViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val selfUser: ObserveSelfUserUseCase,
    private val changeAccessForAppsInConversation: ChangeAccessForAppsInConversationUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val updateAppsAccessNavArgs: UpdateAppsAccessNavArgs = savedStateHandle.navArgs()
    private val conversationId: QualifiedID = updateAppsAccessNavArgs.conversationId
    private val currentAccessParams = updateAppsAccessNavArgs.updateAppsAccessParams

    var updateAppsAccessState by mutableStateOf(
        UpdateAppsAccessState(
            isAppAccessAllowed = false,
            isUpdatingAppAccessAllowed = false,
            isLoadingAppsOption = false,
            shouldShowDisableAppsConfirmationDialog = false
        )
    )

    init {
        observeConversationDetails()
    }

    @Suppress("DestructuringDeclarationWithTooManyEntries")
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

            // TODO(refactor): Move all this logic to a UseCase in kalium if possible.
            combine(
                observeIsAppsAllowedForUsage(),
                conversationDetailsFlow,
                isSelfAdminFlow,
                selfUser()
            ) { isTeamAllowedToUseApps, conversationDetails, isSelfAnAdmin, selfUser ->
                CombineFour(isTeamAllowedToUseApps, conversationDetails, isSelfAnAdmin, selfUser)
            }.collect { (isTeamAllowedToUseApps, conversationDetails, isSelfAnAdmin, selfUser) ->
                val isTeamAdmin = selfUser.userType.isTeamAdmin()
                val isSelfInConversationTeam = selfUser.teamId == conversationDetails.conversation.teamId
                val isSelfChannelTeamAdmin =
                    (conversationDetails is ConversationDetails.Group.Channel && isTeamAdmin && isSelfInConversationTeam)
                val canSelfPerformAdminActions = isSelfAnAdmin || isSelfChannelTeamAdmin

                // WPB-21835: Apps availability logic controlled by feature flag
                val isMLSConversation = conversationDetails.conversation.protocol is Conversation.ProtocolInfo.MLS
                val isAppAccessAllowed = computeAppsEnabledStatus(conversationDetails, isMLSConversation, isTeamAllowedToUseApps)
                val isUpdatingAppAccessAllowed =
                    computeAppsAllowedStatus(canSelfPerformAdminActions, isMLSConversation, isTeamAllowedToUseApps)

                updateAppsAccessState = updateAppsAccessState.copy(
                    isAppAccessAllowed = isAppAccessAllowed,
                    isUpdatingAppAccessAllowed = isUpdatingAppAccessAllowed,
                    isLoadingAppsOption = false,
                    shouldShowDisableAppsConfirmationDialog = false
                )
            }
        }
    }

    /**
     * Determine apps visibility based on feature flag and team settings
     * Or just should be protocol based in case of current logic
     */
    private fun computeAppsEnabledStatus(
        conversationDetails: ConversationDetails,
        isMLSConversation: Boolean,
        isTeamAllowedToUseApps: Boolean
    ) = if (FeatureVisibilityFlags.AppsBasedOnProtocol) {
        // New logic: based on protocol (apps disabled for MLS)o
        conversationDetails.conversation.isServicesAllowed() && !isMLSConversation
    } else {
        // Old logic: based on team settings and feature flags
        conversationDetails.conversation.isServicesAllowed() && isTeamAllowedToUseApps
    }

    /**
     * Determine apps visibility based on feature flag and team settings
     * Or just should be protocol based in case of current logic
     */
    private fun computeAppsAllowedStatus(
        canSelfPerformAdminActions: Boolean,
        isMLSConversation: Boolean,
        isTeamAllowedToUseApps: Boolean
    ) = if (FeatureVisibilityFlags.AppsBasedOnProtocol) {
        // New logic: based on protocol
        canSelfPerformAdminActions && !isMLSConversation
    } else {
        // Old logic: based on permissions and team settings
        canSelfPerformAdminActions && isTeamAllowedToUseApps
    }

    private data class CombineFour(
        val isAppsUsageAllowed: Boolean,
        val conversationDetails: ConversationDetails,
        val isSelfAnAdmin: Boolean,
        val selfUser: SelfUser
    )

    fun onAppsAccessUpdate(shouldEnableAppsAccess: Boolean) {
        updateState(updateAppsAccessState.copy(isLoadingAppsOption = true, isAppAccessAllowed = shouldEnableAppsAccess))
        when (shouldEnableAppsAccess) {
            true -> updateAppsAccessRemotely(true)
            false -> updateState(updateAppsAccessState.copy(shouldShowDisableAppsConfirmationDialog = true))
        }
    }

    private fun updateAppsAccessRemotely(shouldEnableAppsAccess: Boolean) {
        viewModelScope.launch {
            val result = withContext(dispatcher.io()) {
                updateConversationAccess(
                    enableGuestAndNonTeamMember = currentAccessParams.isGuestAllowed,
                    enableServices = shouldEnableAppsAccess,
                    conversationId = conversationId
                )
            }

            when (result) {
                is UpdateConversationAccessRoleUseCase.Result.Failure ->
                    updateState(
                        updateAppsAccessState.copy(
                            isAppAccessAllowed = !shouldEnableAppsAccess,
                            hasErrorOnUpdateAppAccess = true,
                            isLoadingAppsOption = false
                        )
                    )

                is UpdateConversationAccessRoleUseCase.Result.Success -> updateState(
                    updateAppsAccessState.copy(
                        isAppAccessAllowed = shouldEnableAppsAccess,
                        hasErrorOnUpdateAppAccess = false,
                        isLoadingAppsOption = false,
                    )
                )
            }
        }
    }

    private suspend fun updateConversationAccess(
        enableGuestAndNonTeamMember: Boolean,
        enableServices: Boolean,
        conversationId: ConversationId
    ): UpdateConversationAccessRoleUseCase.Result {

        val accessRoles = Conversation
            .accessRolesFor(
                guestAllowed = enableGuestAndNonTeamMember,
                servicesAllowed = enableServices,
                nonTeamMembersAllowed = enableGuestAndNonTeamMember
            )

        val access = Conversation.accessFor(guestsAllowed = enableGuestAndNonTeamMember)

        return changeAccessForAppsInConversation(
            conversationId = conversationId,
            accessRoles = accessRoles,
            access = access
        )
    }

    fun onServiceDialogConfirm() {
        updateState(updateAppsAccessState.copy(shouldShowDisableAppsConfirmationDialog = false, isLoadingAppsOption = true))
        updateAppsAccessRemotely(false)
    }

    fun onAppsDialogDismiss() {
        updateState(
            updateAppsAccessState.copy(
                isLoadingAppsOption = false,
                shouldShowDisableAppsConfirmationDialog = false,
                isAppAccessAllowed = !updateAppsAccessState.isAppAccessAllowed
            )
        )
    }

    private fun updateState(newState: UpdateAppsAccessState) {
        updateAppsAccessState = newState
    }
}
