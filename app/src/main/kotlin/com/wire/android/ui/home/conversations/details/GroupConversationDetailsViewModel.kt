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

package com.wire.android.ui.home.conversations.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.ui.common.ActionsManager
import com.wire.android.ui.common.ActionsManagerImpl
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.android.ui.home.newconversation.channelaccess.toUiEnum
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.functional.getOrNull
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.type.isExternal
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateReceiptModeResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.team.GetUpdatedSelfTeamUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class GroupConversationDetailsViewModel @Inject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val updateConversationAccessRole: UpdateConversationAccessRoleUseCase,
    private val getSelfTeam: GetUpdatedSelfTeamUseCase,
    private val getSelfUser: GetSelfUserUseCase,
    private val updateConversationReceiptMode: UpdateConversationReceiptModeUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    savedStateHandle: SavedStateHandle,
    private val isMLSEnabled: IsMLSEnabledUseCase,
    refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
) : GroupConversationParticipantsViewModel(savedStateHandle, observeConversationMembers, refreshUsersWithoutMetadata),
    ActionsManager<GroupConversationDetailsViewAction> by ActionsManagerImpl() {

    private val groupConversationDetailsNavArgs: GroupConversationDetailsNavArgs = savedStateHandle.navArgs()
    val conversationId: QualifiedID = groupConversationDetailsNavArgs.conversationId

    private val _groupOptionsState = MutableStateFlow(GroupConversationOptionsState(conversationId))
    val groupOptionsState: StateFlow<GroupConversationOptionsState> = _groupOptionsState

    private val _isFetchingInitialData: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isFetchingInitialData: MutableStateFlow<Boolean> = _isFetchingInitialData

    init {
        observeConversationDetails()
    }

    private suspend fun groupDetailsFlow(): Flow<ConversationDetails.Group> = observeConversationDetails(conversationId)
        .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
        .map { it.conversationDetails }
        .filterIsInstance<ConversationDetails.Group>()
        .distinctUntilChanged()
        .flowOn(dispatcher.io())

    /**
     * TODO(refactor): move business logic to Kalium/Logic or similar
     *                 this shouldn't be defined in the ViewModel
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun observeConversationDetails() {
        viewModelScope.launch {
            val groupDetailsFlow = groupDetailsFlow()
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            val selfTeam = getSelfTeam().getOrNull()
            val selfUser = getSelfUser()

            combine(
                observeIsAppsAllowedForUsage(),
                groupDetailsFlow,
                observeSelfDeletionTimerSettingsForConversation(conversationId, considerSelfUserSettings = false),
            ) { isAppsUsageAllowed, groupDetails, selfDeletionTimer ->
                val isSelfInTeamThatOwnsConversation = selfTeam?.id != null && selfTeam.id == groupDetails.conversation.teamId?.value
                val isSelfExternalMember = selfUser?.userType?.isExternal() == true
                val isChannel = groupDetails is ConversationDetails.Group.Channel
                val isSelfTeamAdmin = selfUser?.userType?.isTeamAdmin() == true
                val canPerformChannelAdminTasks = isChannel && isSelfInTeamThatOwnsConversation && isSelfTeamAdmin
                val isRegularGroupAdmin = groupDetails.selfRole == Conversation.Member.Role.Admin
                val canSelfPerformAdminTasks = (isRegularGroupAdmin) || (canPerformChannelAdminTasks)
                val channelPermissionType = groupDetails.getChannelPermissionType()
                val channelAccessType = groupDetails.getChannelAccessType()

                _isFetchingInitialData.value = false

                updateState(
                    groupOptionsState.value.copy(
                        groupName = groupDetails.conversation.name.orEmpty(),
                        protocolInfo = groupDetails.conversation.protocol,
                        proteusVerificationStatus = groupDetails.conversation.proteusVerificationStatus,
                        mlsVerificationStatus = groupDetails.conversation.mlsVerificationStatus,
                        legalHoldStatus = groupDetails.conversation.legalHoldStatus,
                        areAccessOptionsAvailable = groupDetails.conversation.isTeamGroup(),
                        isGuestAllowed = groupDetails.conversation.isGuestAllowed() || groupDetails.conversation.isNonTeamMemberAllowed(),
                        isUpdatingNameAllowed = canSelfPerformAdminTasks && !isSelfExternalMember,
                        isUpdatingGuestAllowed = canSelfPerformAdminTasks && isSelfInTeamThatOwnsConversation,
                        isUpdatingChannelAccessAllowed = canSelfPerformAdminTasks && isSelfInTeamThatOwnsConversation,
                        isServicesAllowed = groupDetails.conversation.isServicesAllowed() && isAppsUsageAllowed,
                        isUpdatingServicesAllowed = canSelfPerformAdminTasks && isSelfInTeamThatOwnsConversation,
                        isUpdatingReadReceiptAllowed = canSelfPerformAdminTasks && groupDetails.conversation.isTeamGroup(),
                        isUpdatingSelfDeletingAllowed = canSelfPerformAdminTasks,
                        mlsEnabled = isMLSEnabled(),
                        isReadReceiptAllowed = groupDetails.conversation.receiptMode == Conversation.ReceiptMode.ENABLED,
                        selfDeletionTimer = selfDeletionTimer,
                        isChannel = isChannel,
                        isSelfTeamAdmin = isSelfTeamAdmin,
                        channelAddPermissionType = channelPermissionType,
                        channelAccessType = channelAccessType,
                        loadingWireCellState = false,
                        isWireCellEnabled = groupDetails.wireCell != null,
                        isWireCellFeatureEnabled = isWireCellsEnabled(),
                    )
                )
            }.collect {}
        }
    }

    fun shouldShowAddParticipantButton(): Boolean {
        val isSelfAdmin = groupParticipantsState.data.isSelfAnAdmin
        val isSelfGuest = groupParticipantsState.data.isSelfGuest
        val isSelfExternalMember = groupParticipantsState.data.isSelfExternalMember

        return when {
            groupOptionsState.value.isChannel -> {
                val isEveryoneAllowed = groupOptionsState.value.channelAddPermissionType == ChannelAddPermissionType.EVERYONE
                isEveryoneAllowed && !isSelfGuest || isSelfAdmin && !isSelfGuest || groupOptionsState.value.isSelfTeamAdmin
            }

            else -> {
                isSelfAdmin && !isSelfExternalMember
            }
        }
    }

    private fun ConversationDetails.getChannelPermissionType(): ChannelAddPermissionType? = if (this is ConversationDetails.Group.Channel) {
        this.permission.toUiEnum()
    } else {
        null
    }

    private fun ConversationDetails.getChannelAccessType(): ChannelAccessType? = if (this is ConversationDetails.Group.Channel) {
        this.access.toUiEnum()
    } else {
        null
    }

    fun updateChannelAccess(channelAccessType: ChannelAccessType) {
        updateState(groupOptionsState.value.copy(channelAccessType = channelAccessType))
    }

    fun updateChannelAddPermission(channelAddPermissionType: ChannelAddPermissionType) {
        updateState(groupOptionsState.value.copy(channelAddPermissionType = channelAddPermissionType))
    }

    // todo (ym) move to update apps access view model
    fun onServicesUpdate(enableServices: Boolean) {
        updateState(groupOptionsState.value.copy(loadingServicesOption = true, isServicesAllowed = enableServices))
        when (enableServices) {
            true -> updateServicesRemoteRequest(enableServices)
            false -> updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = true))
        }
    }

    fun onReadReceiptUpdate(enableReadReceipt: Boolean) {
        appLogger.i("[$TAG][onReadReceiptUpdate] - enableReadReceipt: $enableReadReceipt")
        updateState(groupOptionsState.value.copy(loadingReadReceiptOption = true, isReadReceiptAllowed = enableReadReceipt))
        updateReadReceiptRemoteRequest(enableReadReceipt)
    }

    fun onServiceDialogDismiss() {
        updateState(
            groupOptionsState.value.copy(
                loadingServicesOption = false,
                changeServiceOptionConfirmationRequired = false,
                isServicesAllowed = !groupOptionsState.value.isServicesAllowed
            )
        )
    }

    fun onServiceDialogConfirm() {
        updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = false, loadingServicesOption = true))
        updateServicesRemoteRequest(false)
    }

    private fun updateServicesRemoteRequest(enableServices: Boolean) {
        viewModelScope.launch {
            val result = withContext(dispatcher.io()) {
                updateConversationAccess(
                    enableGuestAndNonTeamMember = groupOptionsState.value.isGuestAllowed,
                    enableServices = enableServices,
                    conversationId = conversationId
                )
            }

            when (result) {
                is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                    groupOptionsState.value.copy(
                        isServicesAllowed = !enableServices,
                        error = GroupConversationOptionsState.Error.UpdateServicesError(result.cause)
                    )
                )

                is UpdateConversationAccessRoleUseCase.Result.Success -> Unit
            }

            updateState(groupOptionsState.value.copy(loadingServicesOption = false))
        }
    }

    private fun updateReadReceiptRemoteRequest(enableReadReceipt: Boolean) {
        viewModelScope.launch {
            val result = withContext(dispatcher.io()) {
                updateConversationReceiptMode(
                    conversationId = conversationId,
                    receiptMode = when (enableReadReceipt) {
                        true -> Conversation.ReceiptMode.ENABLED
                        else -> Conversation.ReceiptMode.DISABLED
                    }
                )
            }

            when (result) {
                is ConversationUpdateReceiptModeResult.Failure -> updateState(
                    groupOptionsState.value.copy(
                        isReadReceiptAllowed = !enableReadReceipt,
                        error = GroupConversationOptionsState.Error.UpdateReadReceiptError(result.cause)
                    )
                )

                ConversationUpdateReceiptModeResult.Success -> Unit
            }

            updateState(groupOptionsState.value.copy(loadingReadReceiptOption = false))
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

        val access = Conversation
            .accessFor(
                guestsAllowed = enableGuestAndNonTeamMember
            )

        return updateConversationAccessRole(
            conversationId = conversationId,
            accessRoles = accessRoles,
            access = access
        )
    }

    fun updateState(newState: GroupConversationOptionsState) {
        _groupOptionsState.value = newState
    }

    private fun onMessage(text: UIText) = sendAction(GroupConversationDetailsViewAction.Message(text))

    companion object {
        const val TAG = "GroupConversationDetailsViewModel"
    }
}

sealed interface GroupConversationDetailsViewAction {
    data class Message(val text: UIText) : GroupConversationDetailsViewAction
}
