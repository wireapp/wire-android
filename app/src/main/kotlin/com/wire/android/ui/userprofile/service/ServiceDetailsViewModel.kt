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
package com.wire.android.ui.userprofile.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.CurrentAccount
import com.wire.android.model.ImageAsset
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.AppsUtil
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.service.ServiceDetails
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.app.GetAppByIdUseCase
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberResult
import com.wire.kalium.logic.feature.app.ObserveIsAppMemberUseCase
import com.wire.kalium.logic.feature.conversation.AddMemberToConversationUseCase
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.IsOneToOneConversationCreatedUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberResult
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class ServiceDetailsViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    @CurrentAccount private val selfUserId: UserId,
    private val getServiceById: GetServiceByIdUseCase,
    private val getAppById: GetAppByIdUseCase,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeIsServiceMember: ObserveIsServiceMemberUseCase,
    private val observeIsAppMember: ObserveIsAppMemberUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val addServiceToConversation: AddServiceToConversationUseCase,
    private val addMemberToConversation: AddMemberToConversationUseCase,
    private val isOneToOneConversationCreated: IsOneToOneConversationCreatedUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serviceDetailsNavArgs: ServiceDetailsNavArgs = savedStateHandle.navArgs()

    private val serviceId: ServiceId = serviceDetailsNavArgs.id.serviceId
    private val userId: UserId = serviceDetailsNavArgs.id.userId
    private val conversationId: QualifiedID? = serviceDetailsNavArgs.conversationId

    var serviceDetailsState by mutableStateOf(ServiceDetailsState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    private val _openConversationEvent = MutableSharedFlow<ConversationId?>()
    val openConversationEvent = _openConversationEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            getIfConversationExist()

            val appsAllowedResult = observeIsAppsAllowedForUsage().firstOrNull()

            val conversationProtocolInfo = conversationId?.let {
                observeConversationDetails(it)
                    .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                    .map { result -> result.conversationDetails.conversation.protocol }
                    .firstOrNull()
            }

            val isAppsEnabled = AppsUtil.isAppsAllowed(
                appsAllowedResult = appsAllowedResult,
                conversationProtocol = conversationProtocolInfo
            )

            serviceDetailsState = serviceDetailsState.copy(
                serviceId = serviceId,
                conversationId = conversationId,
                isDataLoading = true,
                isAvatarLoading = true,
                isAppsEnabled = isAppsEnabled
            )

            when (val id = serviceDetailsNavArgs.id) {
                is ServiceDetailsNavArgs.Id.AppId -> {
                    val details = getAppDetailsAndUpdateViewState(id.appId)
                    if (details != null && isAppsEnabled) {
                        observeIsAppConversationMember(id.appId)
                    }
                }

                is ServiceDetailsNavArgs.Id.BotServiceId -> {
                    getServiceDetailsAndUpdateViewState()?.let {
                        observeIsServiceConversationMember()
                    }
                }
            }
        }
    }

    fun onAddService() {
        viewModelScope.launch {
            val responseMessage = when (val id = serviceDetailsNavArgs.id) {
                is ServiceDetailsNavArgs.Id.AppId -> {
                    val response = addMemberToConversation.invoke(
                        conversationId = requireNotNull(conversationId),
                        userIdList = listOf(id.appId)
                    )

                    when (response) {
                        is AddMemberToConversationUseCase.Result.Failure -> ServiceDetailsInfoMessageType.ErrorAddService
                        is AddMemberToConversationUseCase.Result.Success -> ServiceDetailsInfoMessageType.SuccessAddService
                    }
                }
                is ServiceDetailsNavArgs.Id.BotServiceId -> {
                    val response = withContext(dispatchers.io()) {
                        addServiceToConversation.invoke(
                            conversationId = requireNotNull(conversationId),
                            serviceId = id.serviceId
                        )
                    }

                    when (response) {
                        is AddServiceToConversationUseCase.Result.Failure -> ServiceDetailsInfoMessageType.ErrorAddService
                        is AddServiceToConversationUseCase.Result.Success -> ServiceDetailsInfoMessageType.SuccessAddService
                    }
                }
            }

            _infoMessage.emit(responseMessage.uiText)
        }
    }

    fun onRemoveService() {
        viewModelScope.launch {
            serviceDetailsState.serviceMemberId?.let { serviceMemberId ->
                val response = withContext(dispatchers.io()) {
                    removeMemberFromConversation(
                        conversationId = requireNotNull(conversationId),
                        userIdToRemove = serviceMemberId
                    )
                }

                val responseMessage = when (response) {
                    is RemoveMemberFromConversationUseCase.Result.Failure -> ServiceDetailsInfoMessageType.ErrorRemoveService
                    is RemoveMemberFromConversationUseCase.Result.Success -> ServiceDetailsInfoMessageType.SuccessRemoveService
                }

                _infoMessage.emit(responseMessage.uiText)
            }
        }
    }

    fun onOpenConversation() {
        viewModelScope.launch {
            val result = withContext(dispatchers.io()) {
                getOrCreateOneToOneConversation(userId)
            }

            when (result) {
                is CreateConversationResult.Failure -> {
                    appLogger.d("Couldn't retrieve or create the conversation")
                    _infoMessage.emit(ServiceDetailsInfoMessageType.ErrorStartOrOpenConversation.uiText)
                }
                is CreateConversationResult.Success -> _openConversationEvent.emit(result.conversation.id)
            }
        }
    }

    private suspend fun getServiceDetailsAndUpdateViewState(): ServiceDetails? =
        getServiceById(serviceId = serviceId).also { service ->
            if (service != null) {
                val serviceAvatarAsset = service.completeAssetId?.let { asset ->
                    ImageAsset.UserAvatarAsset(asset)
                }

                serviceDetailsState = serviceDetailsState.copy(
                    isDataLoading = false,
                    isAvatarLoading = false,
                    serviceAvatarAsset = serviceAvatarAsset,
                    serviceDetails = service
                )
            } else {
                serviceNotFound()
            }
        }

    private suspend fun getAppDetailsAndUpdateViewState(appId: UserId): ServiceDetails? =
        getAppById(appId = appId).also { app ->
            if (app != null) {
                val appAvatarAsset = app.completeAssetId?.let { asset ->
                    ImageAsset.UserAvatarAsset(asset)
                } ?: app.previewAssetId?.let { asset ->
                    ImageAsset.UserAvatarAsset(asset)
                }

                serviceDetailsState = serviceDetailsState.copy(
                    isDataLoading = false,
                    isAvatarLoading = false,
                    serviceAvatarAsset = appAvatarAsset,
                    serviceDetails = app
                )
            } else {
                serviceNotFound()
            }
        }

    private suspend fun observeGroupInfo(): Flow<ServiceDetailsGroupState> =
        conversationId?.let {
            observeConversationRoleForUser(conversationId, selfUserId)
                .map { conversationRoleData ->
                    ServiceDetailsGroupState(
                        role = conversationRoleData.userRole,
                        isSelfAdmin = conversationRoleData.selfRole is Conversation.Member.Role.Admin
                    )
                }
        } ?: flowOf()

    private suspend fun observeIsServiceConversationMember() {
        conversationId?.let {
            observeIsServiceMember(
                serviceId = serviceId,
                conversationId = conversationId
            )
                .combine(observeGroupInfo(), ::Pair)
                .flowOn(dispatchers.io())
                .collect { (serviceMemberResult: ObserveIsServiceMemberResult, groupInfo: ServiceDetailsGroupState) ->
                    val memberId =
                        (serviceMemberResult as? ObserveIsServiceMemberResult.Success)?.userId
                    updateViewStateButton(
                        serviceMemberId = memberId,
                        groupInfo = groupInfo
                    )
                }
        }
    }

    private suspend fun observeIsAppConversationMember(appId: UserId) {
        conversationId?.let {
            observeIsAppMember(
                appId = appId,
                conversationId = conversationId
            )
                .combine(observeGroupInfo(), ::Pair)
                .flowOn(dispatchers.io())
                .collect { (appMemberResult: ObserveIsAppMemberResult, groupInfo: ServiceDetailsGroupState) ->
                    val memberId = (appMemberResult as? ObserveIsAppMemberResult.Success)?.userId
                    updateViewStateButton(
                        serviceMemberId = memberId,
                        groupInfo
                    )
                }
        }
    }

    private fun getIfConversationExist() {
        viewModelScope.launch {
            if (conversationId == null) {
                val isOneToOneConversationCreated = isOneToOneConversationCreated(userId)
                serviceDetailsState = serviceDetailsState.copy(
                    isConversationStarted = isOneToOneConversationCreated
                )
            }
        }
    }

    private fun serviceNotFound() {
        serviceDetailsState = serviceDetailsState.copy(
            serviceDetails = null,
            buttonState = ServiceDetailsButtonState.HIDDEN
        )
    }

    private fun updateViewStateButton(
        serviceMemberId: UserId?,
        groupInfo: ServiceDetailsGroupState
    ) {
        val buttonState = if (conversationId != null && groupInfo.isSelfAdmin) {
            if (serviceMemberId != null) {
                ServiceDetailsButtonState.REMOVE
            } else {
                ServiceDetailsButtonState.ADD
            }
        } else {
            ServiceDetailsButtonState.HIDDEN
        }

        serviceDetailsState = serviceDetailsState.copy(
            buttonState = buttonState,
            serviceMemberId = serviceMemberId
        )
    }

    private companion object {
        const val TAG = "ServiceDetailsViewModel"
    }
}
