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
import com.wire.android.ui.navArgs
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.service.ServiceDetails
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.AddServiceToConversationUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberResult
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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
    private val observeIsServiceMember: ObserveIsServiceMemberUseCase,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val addServiceToConversation: AddServiceToConversationUseCase,
    serviceDetailsMapper: ServiceDetailsMapper,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val serviceDetailsNavArgs: ServiceDetailsNavArgs = savedStateHandle.navArgs()
    private val serviceId: ServiceId = serviceDetailsMapper.fromBotServiceToServiceId(serviceDetailsNavArgs.botService)
    private val conversationId: QualifiedID = serviceDetailsNavArgs.conversationId

    var serviceDetailsState by mutableStateOf(ServiceDetailsState())
    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            serviceDetailsState = serviceDetailsState.copy(
                serviceId = serviceId,
                conversationId = conversationId,
                isDataLoading = true,
                isAvatarLoading = true
            )

            getServiceDetailsAndUpdateViewState()?.let {
                observeIsServiceConversationMember()
            }
        }
    }

    fun addService() {
        viewModelScope.launch {
            val response = withContext(dispatchers.io()) {
                addServiceToConversation.invoke(
                    conversationId = conversationId,
                    serviceId = serviceId
                )
            }

            val responseMessage = when (response) {
                is AddServiceToConversationUseCase.Result.Failure -> ServiceDetailsInfoMessageType.ErrorAddService
                is AddServiceToConversationUseCase.Result.Success -> ServiceDetailsInfoMessageType.SuccessAddService
            }

            _infoMessage.emit(responseMessage.uiText)
        }
    }

    fun removeService() {
        viewModelScope.launch {
            serviceDetailsState.serviceMemberId?.let { serviceMemberId ->
                val response = withContext(dispatchers.io()) {
                    removeMemberFromConversation(
                        conversationId = conversationId,
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

    private suspend fun observeGroupInfo(): Flow<ServiceDetailsGroupState> {
        return observeConversationRoleForUser(conversationId, selfUserId)
            .map { conversationRoleData ->
                ServiceDetailsGroupState(
                    role = conversationRoleData.userRole,
                    isSelfAdmin = conversationRoleData.selfRole is Conversation.Member.Role.Admin
                )
            }
    }

    private suspend fun observeIsServiceConversationMember() {
        observeIsServiceMember(
            serviceId = serviceId,
            conversationId = conversationId
        )
            .combine(observeGroupInfo(), ::Pair)
            .flowOn(dispatchers.io())
            .collect { (serviceMemberResult: ObserveIsServiceMemberResult, groupInfo: ServiceDetailsGroupState) ->
                val memberId = (serviceMemberResult as? ObserveIsServiceMemberResult.Success)?.userId
                updateViewStateButton(
                    serviceMemberId = memberId,
                    groupInfo = groupInfo
                )
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
        val buttonState = when (groupInfo.isSelfAdmin) {
            true -> {
                serviceMemberId?.let { ServiceDetailsButtonState.REMOVE }
                    ?: ServiceDetailsButtonState.ADD
            }

            false -> ServiceDetailsButtonState.HIDDEN
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
