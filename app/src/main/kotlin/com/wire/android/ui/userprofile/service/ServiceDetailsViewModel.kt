package com.wire.android.ui.userprofile.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.userprofile.common.UsernameMapper
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ServiceDetailsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel() {

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val conversationId: QualifiedID = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!.toQualifiedID(qualifiedIdMapper)

    var serviceDetailsState by mutableStateOf(
        ServiceDetailsState(
            userId = userId,
            conversationId = conversationId,
            isDataLoading = true,
            isAvatarLoading = true
        )
    )

    init {
        viewModelScope.launch {
            observeUserInfoAndUpdateViewState()
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun addService() {
        // TODO: Add logic
    }

    fun removeService() {
        viewModelScope.launch {
            val response = withContext(dispatchers.io()) {
                removeMemberFromConversation(
                    conversationId = conversationId,
                    userIdToRemove = userId
                )
            }

            if (response is RemoveMemberFromConversationUseCase.Result.Failure) {
                // TODO: Add correct snackbar error message
                appLogger.i("[$TAG] - Error while trying to remove service from conversation.")
            }
        }
    }

    private suspend fun observeGroupInfo(): Flow<ServiceDetailsGroupState> {
        return observeConversationRoleForUser(conversationId, userId)
            .map { conversationRoleData ->
                ServiceDetailsGroupState(
                    role = conversationRoleData.userRole,
                    isSelfAdmin = conversationRoleData.selfRole is Conversation.Member.Role.Admin,
                    conversationId = conversationRoleData.conversationId
                )
            }
    }

    private fun observeUserInfoAndUpdateViewState() {
        viewModelScope.launch {
            observeUserInfo(userId)
                .combine(observeGroupInfo(), ::Pair)
                .flowOn(dispatchers.io())
                .collect { (userResult, groupInfo) ->
                    when (userResult) {
                        is GetUserInfoResult.Failure -> {
                            appLogger.d("[$TAG] - Couldn't not find the user with provided id: $userId")
                            // TODO: What to do here in case of failure?
                            // closeBottomSheetAndShowInfoMessage(OtherUserProfileInfoMessageType.LoadUserInformationError)
                        }

                        is GetUserInfoResult.Success -> {
                            updateUserInfoState(userResult, groupInfo)
                        }
                    }
                }
        }
    }

    private fun updateUserInfoState(userResult: GetUserInfoResult.Success, groupInfo: ServiceDetailsGroupState) {
        val otherUser = userResult.otherUser
        val userAvatarAsset = otherUser.completePicture
            ?.let { pic -> ImageAsset.UserAvatarAsset(wireSessionImageLoader, pic) }
        val buttonState = when (groupInfo.isSelfAdmin) {
            true -> {
                if (groupInfo.role != null) ServiceDetailsButtonState.REMOVE
                else ServiceDetailsButtonState.ADD
            }

            false -> ServiceDetailsButtonState.HIDDEN
        }

        serviceDetailsState = serviceDetailsState.copy(
            isDataLoading = false,
            isAvatarLoading = false,
            userAvatarAsset = userAvatarAsset,
            fullName = otherUser.name.orEmpty(),
            userName = UsernameMapper.mapUserLabel(otherUser),
            membership = userTypeMapper.toMembership(otherUser.userType),
            buttonState = buttonState,
        )
    }

    private companion object {
        const val TAG = "ServiceDetailsViewModel"
    }
}
