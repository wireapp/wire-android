package com.wire.android.ui.userprofile.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_BOT_SERVICE_ID
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.service.ServiceId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.service.GetServiceByIdUseCase
import com.wire.kalium.logic.feature.service.ObserveIsServiceMemberUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ServiceDetailsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val observerSelfUser: GetSelfUserUseCase,
    private val getServiceById: GetServiceByIdUseCase,
    private val observeIsServiceMember: ObserveIsServiceMemberUseCase,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val serviceDetailsMapper: ServiceDetailsMapper,
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel() {

    private val serviceId: ServiceId? = serviceDetailsMapper.fromStringToServiceId(savedStateHandle.get<String>(EXTRA_BOT_SERVICE_ID)!!)
    private val conversationId: QualifiedID = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.toQualifiedID(qualifiedIdMapper)

    private lateinit var selfUserId: UserId

    var serviceDetailsState by mutableStateOf(
        ServiceDetailsState(
            serviceId = serviceId,
            conversationId = conversationId,
            isDataLoading = true,
            isAvatarLoading = true
        )
    )

    init {
        viewModelScope.launch {
            selfUserId = observerSelfUser().first().id
            getServiceDetailsAndUpdateViewState()
            observeIsServiceConversationMember()
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun addService() {
        // TODO: Add logic
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

                if (response is RemoveMemberFromConversationUseCase.Result.Failure) {
                    // TODO: Add correct snackbar error message
                    appLogger.i("[$TAG] - Error while trying to remove service from conversation.")
                }
            }
        }
    }

    private suspend fun getServiceDetailsAndUpdateViewState() {
        viewModelScope.launch {
            serviceId?.let {
                getServiceById(serviceId = serviceId)?.let { service ->
                    val serviceAvatarAsset = service.completeAssetId?.let { asset ->
                        ImageAsset.UserAvatarAsset(wireSessionImageLoader, asset)
                    }

                    serviceDetailsState = serviceDetailsState.copy(
                        isDataLoading = false,
                        isAvatarLoading = false,
                        serviceAvatarAsset = serviceAvatarAsset,
                        serviceDetails = service
                    )
                } ?: serviceNotFound()
            } ?: navigateBack()
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
        viewModelScope.launch {
            serviceId?.let {
                observeIsServiceMember(
                    serviceId = serviceId,
                    conversationId = conversationId
                )
                    .combine(observeGroupInfo(), ::Pair)
                    .flowOn(dispatchers.io())
                    .collect { (serviceMemberId: QualifiedID?, groupInfo: ServiceDetailsGroupState) ->
                        updateViewStateButton(
                            serviceMemberId = serviceMemberId,
                            groupInfo = groupInfo
                        )
                    }
            }
        }
    }

    private fun serviceNotFound() {
        // TODO: Display error to user
        appLogger.d("[$TAG] - Couldn't not find the service with: $serviceId")
    }

    private fun updateViewStateButton(
        serviceMemberId: QualifiedID?,
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
