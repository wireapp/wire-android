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

import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.ui.common.ActionsViewModel
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsManager
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsManagerImpl
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAddPermissionType
import com.wire.android.ui.home.newconversation.channelaccess.toUiEnum
import com.wire.android.util.AppsUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.featureConfig.Status
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.type.isExternal
import com.wire.kalium.logic.data.user.type.isFederated
import com.wire.kalium.logic.data.user.type.isRegularTeamMember
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateReceiptModeResult
import com.wire.kalium.logic.feature.conversation.MigrateConversationToMLSUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.debug.GetFeatureConfigResult
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import com.wire.kalium.network.exceptions.KaliumException
import com.wire.kalium.util.DebugKaliumApi
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
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
import com.wire.android.di.metro.WireAssistedViewModelBinding
import com.wire.android.ui.home.conversations.ConversationDetailsManualViewModelFactoryGroup

@OptIn(DebugKaliumApi::class)
@Suppress("TooManyFunctions", "LongParameterList")
@WireAssistedViewModelBinding(ConversationDetailsManualViewModelFactoryGroup::class)
class GroupConversationDetailsViewModel @AssistedInject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val observeSelfUserWithTeam: ObserveSelfUserWithTeamUseCase,
    private val updateConversationReceiptMode: UpdateConversationReceiptModeUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    @Assisted navigationArgs: GroupConversationDetailsNavArgs,
    private val isMLSEnabled: IsMLSEnabledUseCase,
    refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
    private val getFeatureConfig: GetFeatureConfigUseCase,
    private val migrateConversationToMLS: MigrateConversationToMLSUseCase,
) : ActionsViewModel<GroupConversationDetailsViewAction>(),
    GroupConversationParticipantsManager by GroupConversationParticipantsManagerImpl(
        conversationId = navigationArgs.conversationId,
        observeConversationMembers = observeConversationMembers,
        refreshUsersWithoutMetadata = refreshUsersWithoutMetadata
    ) {
    @AssistedFactory
    interface Factory {
        fun create(navigationArgs: GroupConversationDetailsNavArgs): GroupConversationDetailsViewModel
    }

    val conversationId: QualifiedID = navigationArgs.conversationId

    private val _groupOptionsState = MutableStateFlow(GroupConversationOptionsState(conversationId))
    val groupOptionsState: StateFlow<GroupConversationOptionsState> = _groupOptionsState

    private val _isFetchingInitialData: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isFetchingInitialData: MutableStateFlow<Boolean> = _isFetchingInitialData
    private val isMlsMigrationEnabled = MutableStateFlow(false)
    private var protocolTapCount = 0

    init {
        loadMlsMigrationFeatureFlag()
        observeConversationDetails()
    }

    private fun loadMlsMigrationFeatureFlag() {
        viewModelScope.launch {
            isMlsMigrationEnabled.value = when (val result = withContext(dispatcher.io()) { getFeatureConfig() }) {
                is GetFeatureConfigResult.Success -> {
                    val migrationConfig = result.featureConfigModel.mlsMigrationModel
                    migrationConfig?.status == Status.ENABLED
                }

                is GetFeatureConfigResult.Failure -> {
                    appLogger.w(
                        "[$TAG] Failed to load the manual MLS migration feature config " +
                                "(failureType=${result.coreFailure::class.simpleName})"
                    )
                    false
                }
            }
        }
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
            val selfWithTeamFlow = observeSelfUserWithTeam()
                .flowOn(dispatcher.io())
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)
            val appsAllowedResultFlow = observeIsAppsAllowedForUsage()
                .flowOn(dispatcher.io())
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            combine(
                groupDetailsFlow,
                selfWithTeamFlow,
                appsAllowedResultFlow,
                observeSelfDeletionTimerSettingsForConversation(conversationId, considerSelfUserSettings = false),
                isMlsMigrationEnabled,
            ) { groupDetails, (selfUser, selfTeam), appsAllowedResult, selfDeletionTimer, isMlsMigrationEnabled ->
                val selfType = selfUser.userType
                val isSelfInTeamThatOwnsConversation = selfTeam?.id != null && selfTeam.id == groupDetails.conversation.teamId?.value
                val isSelfExternalMember = selfUser.userType.isExternal()
                val isRegularTeamMember = selfType.isRegularTeamMember()
                val isChannel = groupDetails is ConversationDetails.Group.Channel
                val isSelfTeamAdmin = selfUser.userType.isTeamAdmin()
                val canPerformChannelAdminTasks = isChannel && isSelfInTeamThatOwnsConversation && isSelfTeamAdmin
                val isRegularGroupAdmin = groupDetails.selfRole == Conversation.Member.Role.Admin
                val canSelfPerformAdminTasks = (isRegularGroupAdmin) || (canPerformChannelAdminTasks)
                val isConversationInSelfTeam = groupDetails.conversation.teamId?.let { it == selfUser.teamId } == true
                val canManuallyMigrateToMLS = isMlsMigrationEnabled &&
                        groupDetails.conversation.protocol.isProteusOrMixed() &&
                        isConversationInSelfTeam
                val channelPermissionType = groupDetails.getChannelPermissionType()
                val channelAccessType = groupDetails.getChannelAccessType()
                val isExternalOrFederated =
                    selfType.isExternal() || selfType.isFederated()
                val canSelfAddParticipants =
                    when {
                        !isChannel -> isRegularGroupAdmin && !isExternalOrFederated

                        groupOptionsState.value.isSelfTeamAdmin -> true

                        canSelfPerformAdminTasks -> true

                        channelPermissionType == ChannelAddPermissionType.EVERYONE &&
                                isSelfInTeamThatOwnsConversation &&
                                isRegularTeamMember ->
                            true

                        else -> false
                    }

                val shouldUseNewAppsUi = computeShouldUseNewAppsUi(groupDetails, appsAllowedResult)
                val isUpdatingAppsAllowedForConversation =
                    computeAppsAllowedStatus(canSelfPerformAdminTasks, isSelfInTeamThatOwnsConversation, groupDetails, appsAllowedResult)

                _isFetchingInitialData.value = false

                val mlsEnabled = isMLSEnabled()
                val wireCellFeatureEnabled = isWireCellsEnabled()

                if (!canManuallyMigrateToMLS) {
                    protocolTapCount = 0
                }

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
                        isAppsAllowed = groupDetails.conversation.isServicesAllowed(),
                        shouldUseNewAppsUi = shouldUseNewAppsUi,
                        isUpdatingAppsAllowed = isUpdatingAppsAllowedForConversation,
                        isUpdatingReadReceiptAllowed = canSelfPerformAdminTasks && groupDetails.conversation.isTeamGroup(),
                        isUpdatingSelfDeletingAllowed = canSelfPerformAdminTasks,
                        mlsEnabled = mlsEnabled,
                        isReadReceiptAllowed = if (groupOptionsState.value.loadingReadReceiptOption) {
                            groupOptionsState.value.isReadReceiptAllowed
                        } else {
                            groupDetails.conversation.receiptMode == Conversation.ReceiptMode.ENABLED
                        },
                        selfDeletionTimer = selfDeletionTimer,
                        isChannel = isChannel,
                        isSelfTeamAdmin = isSelfTeamAdmin,
                        channelAddPermissionType = channelPermissionType,
                        channelAccessType = channelAccessType,
                        loadingWireCellState = false,
                        isWireCellEnabled = groupDetails.wireCell != null,
                        isWireCellFeatureEnabled = wireCellFeatureEnabled,
                        isSelfPartOfATeam = selfTeam != null,
                        canSelfAddParticipants = canSelfAddParticipants,
                        canManuallyMigrateToMLS = canManuallyMigrateToMLS,
                        shouldShowMlsMigrationDialog = groupOptionsState.value.shouldShowMlsMigrationDialog &&
                                canManuallyMigrateToMLS,
                    )
                )
            }.collect {}
        }
    }

    /**
     * Determine apps visibility based on feature flag and team settings
     * Or just should be protocol based in case of current logic
     */
    private fun computeAppsAllowedStatus(
        canSelfPerformAdminTasks: Boolean,
        isSelfInTeamThatOwnsConversation: Boolean,
        groupDetails: ConversationDetails.Group,
        appsAllowedResult: AppsAllowedResult
    ) = canSelfPerformAdminTasks &&
            isSelfInTeamThatOwnsConversation &&
            isServicesSupportedForConversation(groupDetails.conversation.protocol, appsAllowedResult)

    private fun isServicesSupportedForConversation(
        protocolInfo: Conversation.ProtocolInfo,
        appsAllowedResult: AppsAllowedResult
    ) = appsAllowedResult is AppsAllowedResult.Enabled &&
            when (protocolInfo) {
                is Conversation.ProtocolInfo.MLS -> AppsUtil.isAppsAllowed(appsAllowedResult, protocolInfo)
                is Conversation.ProtocolInfo.Proteus -> true
                is Conversation.ProtocolInfo.Mixed -> AppsUtil.isAppsAllowed(appsAllowedResult, protocolInfo)
            }

    private fun computeShouldUseNewAppsUi(
        groupDetails: ConversationDetails.Group,
        appsAllowedResult: AppsAllowedResult
    ) = AppsUtil.isAppsAllowed(appsAllowedResult, groupDetails.conversation.protocol)

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

    fun onReadReceiptUpdate(enableReadReceipt: Boolean) {
        appLogger.i("[$TAG][onReadReceiptUpdate] - enableReadReceipt: $enableReadReceipt")
        updateState(groupOptionsState.value.copy(loadingReadReceiptOption = true, isReadReceiptAllowed = enableReadReceipt))
        updateReadReceiptRemoteRequest(enableReadReceipt)
    }

    fun onProtocolTapped() {
        val state = groupOptionsState.value
        if (!state.canManuallyMigrateToMLS) {
            protocolTapCount = 0
            return
        }

        protocolTapCount += 1
        if (protocolTapCount == MANUAL_MIGRATION_TAP_COUNT) {
            protocolTapCount = 0
            appLogger.i("[$TAG] Manual MLS migration confirmation shown")
            updateState(groupOptionsState.value.copy(shouldShowMlsMigrationDialog = true))
        }
    }

    fun onMlsMigrationDialogDismissed() {
        if (groupOptionsState.value.isMigratingToMLS) {
            return
        }
        protocolTapCount = 0
        appLogger.i("[$TAG] Manual MLS migration confirmation dismissed")
        updateState(groupOptionsState.value.copy(shouldShowMlsMigrationDialog = false))
    }

    fun onMlsMigrationConfirmed() {
        if (!groupOptionsState.value.canManuallyMigrateToMLS || groupOptionsState.value.isMigratingToMLS) {
            return
        }

        appLogger.i(
            "[$TAG] Manual MLS migration started " +
                    "(protocol=${groupOptionsState.value.protocolInfo.debugName()})"
        )
        updateState(groupOptionsState.value.copy(isMigratingToMLS = true))
        viewModelScope.launch {
            when (val result = migrateConversationToMLS(conversationId)) {
                MigrateConversationToMLSUseCase.Result.Success -> {
                    appLogger.i("[$TAG] Manual MLS migration succeeded")
                    updateState(
                        groupOptionsState.value.copy(
                            shouldShowMlsMigrationDialog = false,
                            isMigratingToMLS = false,
                        )
                    )
                    sendAction(GroupConversationDetailsViewAction.Message(UIText.StringResource(R.string.mls_migration_success)))
                }

                is MigrateConversationToMLSUseCase.Result.Failure -> {
                    val backendError = result.cause.backendErrorDetails()
                    val backendErrorLog = backendError?.let {
                        ", httpCode=${it.httpCode}, backendLabel=${it.label}"
                    }.orEmpty()
                    appLogger.e(
                        "[$TAG] Manual MLS migration failed " +
                                "(failureType=${result.cause::class.simpleName}$backendErrorLog)"
                    )
                    updateState(
                        groupOptionsState.value.copy(
                            shouldShowMlsMigrationDialog = false,
                            isMigratingToMLS = false,
                        )
                    )
                    sendAction(GroupConversationDetailsViewAction.Message(result.cause.mlsMigrationFailureText()))
                }
            }
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

    fun updateState(newState: GroupConversationOptionsState) {
        _groupOptionsState.value = newState
    }

    companion object {
        const val TAG = "GroupConversationDetailsViewModel"
        private const val MANUAL_MIGRATION_TAP_COUNT = 5
    }
}

private fun Conversation.ProtocolInfo.isProteusOrMixed(): Boolean =
    this is Conversation.ProtocolInfo.Proteus || this is Conversation.ProtocolInfo.Mixed

private fun Conversation.ProtocolInfo.debugName(): String = when (this) {
    is Conversation.ProtocolInfo.MLS -> "MLS"
    is Conversation.ProtocolInfo.Mixed -> "Mixed"
    Conversation.ProtocolInfo.Proteus -> "Proteus"
}

private fun CoreFailure.mlsMigrationFailureText(): UIText = backendErrorDetails()?.let {
    UIText.StringResource(R.string.mls_migration_failure_with_backend_error, it.httpCode, it.label)
} ?: UIText.StringResource(R.string.mls_migration_failure)

private fun CoreFailure.backendErrorDetails(): BackendErrorDetails? =
    (this as? NetworkFailure.ServerMiscommunication)?.kaliumException?.let { exception ->
        when (exception) {
            is KaliumException.RedirectError -> exception.errorResponse
            is KaliumException.InvalidRequestError -> exception.errorResponse
            is KaliumException.ServerError -> exception.errorResponse
            else -> null
        }
    }?.let { BackendErrorDetails(it.code, it.label) }

private data class BackendErrorDetails(val httpCode: Int, val label: String)

sealed interface GroupConversationDetailsViewAction {
    data class Message(val text: UIText) : GroupConversationDetailsViewAction
}
