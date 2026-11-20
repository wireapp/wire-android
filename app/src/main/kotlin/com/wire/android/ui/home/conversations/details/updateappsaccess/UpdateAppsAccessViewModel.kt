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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.util.AppsUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.type.isTeamAdmin
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.apps.ChangeAccessForAppsInConversationUseCase
import com.wire.kalium.logic.feature.featureConfig.AppsAllowedResult
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.HasWhitelistedServicesUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
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

@WireAssistedViewModelBinding(ConversationDetailsManualViewModelFactoryGroup::class)
class UpdateAppsAccessViewModel @AssistedInject constructor(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val selfUser: ObserveSelfUserUseCase,
    private val changeAccessForAppsInConversation: ChangeAccessForAppsInConversationUseCase,
    @Assisted navigationArgs: UpdateAppsAccessNavArgs,
    private val hasWhitelistedServices: HasWhitelistedServicesUseCase,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(navigationArgs: UpdateAppsAccessNavArgs): UpdateAppsAccessViewModel
    }

    private val conversationId: QualifiedID = navigationArgs.conversationId
    private var currentConversation: Conversation? = null
    private var canSelfManageApps = false

    private class WhitelistRequest

    private val whitelistRequest = MutableStateFlow<WhitelistRequest?>(null)
    private val hasLegacyBots = MutableStateFlow(false)
    private val currentAccessParams = navigationArgs.updateAppsAccessParams
    val shouldUseNewAppsUi: Boolean = currentAccessParams.shouldUseNewAppsUi

    var updateAppsAccessState by mutableStateOf(
        UpdateAppsAccessState(
            isAppAccessAllowed = false,
            isUpdatingAppAccessAllowed = false,
            isLoadingAppsOption = false,
            shouldShowDisableAppsConfirmationDialog = false
        )
    )

    init {
        viewModelScope.launch {
            whitelistRequest.collectLatest { request ->
                if (request == null) return@collectLatest
                val available = hasWhitelistedServices()
                if (whitelistRequest.value === request) {
                    hasLegacyBots.value = available
                }
            }
        }
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
                selfUser(),
                hasLegacyBots,
            ) { isAppsAllowedResult, conversationDetails, isSelfAnAdmin, selfUser, legacyAppsAvailable ->
                AppsAccessInputs(isAppsAllowedResult, conversationDetails, isSelfAnAdmin, selfUser, legacyAppsAvailable)
            }.collect(::onAppsAccessInputs)
        }
    }

    @Suppress("DestructuringDeclarationWithTooManyEntries")
    private fun onAppsAccessInputs(inputs: AppsAccessInputs) {
        val (isAppsAllowedResult, conversationDetails, isSelfAnAdmin, selfUser, legacyAppsAvailable) = inputs
        currentConversation = conversationDetails.conversation
        val isTeamAdmin = selfUser.userType.isTeamAdmin()
        val isSelfInConversationTeam = selfUser.teamId == conversationDetails.conversation.teamId
        val isSelfChannelTeamAdmin =
            (conversationDetails is ConversationDetails.Group.Channel && isTeamAdmin && isSelfInConversationTeam)
        val canSelfPerformAdminActions = isSelfAnAdmin || isSelfChannelTeamAdmin

        val isProteus = conversationDetails.conversation.protocol is Conversation.ProtocolInfo.Proteus
        canSelfManageApps = canSelfPerformAdminActions &&
            (!isProteus || (selfUser.teamId != null && isSelfInConversationTeam))
        checkLegacyBots(isProteus && canSelfManageApps)
        val isAppAccessAllowed = if (isProteus) {
            conversationDetails.conversation.isServicesAllowed()
        } else {
            conversationDetails.conversation.isServicesAllowed() &&
                AppsUtil.isAppsAllowed(isAppsAllowedResult, conversationDetails.conversation.protocol)
        }
        val isUpdatingAppAccessAllowed = if (isProteus) {
            canSelfManageApps && (isAppAccessAllowed || legacyAppsAvailable)
        } else {
            canSelfPerformAdminActions &&
                AppsUtil.isAppsAllowed(isAppsAllowedResult, conversationDetails.conversation.protocol)
        }
        updateObservedAccessState(isAppAccessAllowed, isUpdatingAppAccessAllowed)
    }

    private fun updateObservedAccessState(isAppAccessAllowed: Boolean, isUpdatingAppAccessAllowed: Boolean) {
        val wasConfirming = updateAppsAccessState.shouldShowDisableAppsConfirmationDialog
        val keepConfirmation = wasConfirming && isUpdatingAppAccessAllowed
        updateAppsAccessState = updateAppsAccessState.copy(
            isUpdatingAppAccessAllowed = isUpdatingAppAccessAllowed,
            isAppAccessAllowed = if (keepConfirmation) updateAppsAccessState.isAppAccessAllowed else isAppAccessAllowed,
            shouldShowDisableAppsConfirmationDialog = keepConfirmation,
            isLoadingAppsOption = updateAppsAccessState.isLoadingAppsOption && (!wasConfirming || keepConfirmation),
        )
    }

    private fun checkLegacyBots(canCheckWhitelist: Boolean) {
        if (!canCheckWhitelist) {
            whitelistRequest.value = null
            hasLegacyBots.value = false
        } else if (whitelistRequest.value == null) {
            whitelistRequest.value = WhitelistRequest()
        }
    }

    private data class AppsAccessInputs(
        val appsAllowedResult: AppsAllowedResult,
        val conversationDetails: ConversationDetails,
        val isSelfAnAdmin: Boolean,
        val selfUser: SelfUser,
        val legacyAppsAvailable: Boolean,
    )

    fun onAppsAccessUpdate(shouldEnableAppsAccess: Boolean) {
        if (updateAppsAccessState.isLoadingAppsOption || !isAppsAccessChangeAllowed(shouldEnableAppsAccess)) return
        updateState(updateAppsAccessState.copy(isLoadingAppsOption = true, isAppAccessAllowed = shouldEnableAppsAccess))
        when (shouldEnableAppsAccess) {
            true -> updateAppsAccessRemotely(true)
            false -> updateState(updateAppsAccessState.copy(shouldShowDisableAppsConfirmationDialog = true))
        }
    }

    private fun isAppsAccessChangeAllowed(enable: Boolean): Boolean {
        val conversation = currentConversation ?: return false
        return if (conversation.protocol is Conversation.ProtocolInfo.Proteus) {
            canSelfManageApps && if (enable) {
                hasLegacyBots.value
            } else {
                conversation.isServicesAllowed()
            }
        } else {
            updateAppsAccessState.isUpdatingAppAccessAllowed
        }
    }

    private fun updateAppsAccessRemotely(shouldEnableAppsAccess: Boolean) {
        viewModelScope.launch {
            val conversation = currentConversation ?: return@launch
            val result = withContext(dispatcher.io()) {
                updateConversationAccess(
                    conversation = conversation,
                    enableServices = shouldEnableAppsAccess
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

                is UpdateConversationAccessRoleUseCase.Result.Success -> {
                    val latestConversation = currentConversation
                    if (latestConversation?.id == conversation.id && latestConversation.protocol is Conversation.ProtocolInfo.Proteus) {
                        currentConversation = latestConversation.copy(
                            accessRole = if (shouldEnableAppsAccess) {
                                (latestConversation.accessRole + Conversation.AccessRole.SERVICE).distinct()
                            } else {
                                latestConversation.accessRole - Conversation.AccessRole.SERVICE
                            }
                        )
                    }
                    updateState(
                        updateAppsAccessState.copy(
                            isAppAccessAllowed = shouldEnableAppsAccess,
                            isUpdatingAppAccessAllowed = if (conversation.protocol is Conversation.ProtocolInfo.Proteus) {
                                isAppsAccessChangeAllowed(!shouldEnableAppsAccess)
                            } else {
                                updateAppsAccessState.isUpdatingAppAccessAllowed
                            },
                            hasErrorOnUpdateAppAccess = false,
                            isLoadingAppsOption = false,
                        )
                    )
                }
            }
        }
    }

    private suspend fun updateConversationAccess(
        conversation: Conversation,
        enableServices: Boolean,
    ): UpdateConversationAccessRoleUseCase.Result {
        val accessRoles = conversation.accessRole.toSet().let {
            if (enableServices) it + Conversation.AccessRole.SERVICE else it - Conversation.AccessRole.SERVICE
        }
        return changeAccessForAppsInConversation(
            conversationId = conversationId,
            accessRoles = accessRoles,
            access = conversation.access.toSet()
        )
    }

    fun onServiceDialogConfirm() {
        if (!updateAppsAccessState.shouldShowDisableAppsConfirmationDialog || !isAppsAccessChangeAllowed(false)) return
        updateState(updateAppsAccessState.copy(shouldShowDisableAppsConfirmationDialog = false, isLoadingAppsOption = true))
        updateAppsAccessRemotely(false)
    }

    fun onAppsDialogDismiss() {
        updateState(
            updateAppsAccessState.copy(
                isLoadingAppsOption = false,
                shouldShowDisableAppsConfirmationDialog = false,
                isAppAccessAllowed = if (currentConversation?.protocol is Conversation.ProtocolInfo.Proteus) {
                    currentConversation?.isServicesAllowed() == true
                } else {
                    !updateAppsAccessState.isAppAccessAllowed
                }
            )
        )
    }

    private fun updateState(newState: UpdateAppsAccessState) {
        updateAppsAccessState = newState
    }
}
