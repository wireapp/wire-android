package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset
import com.wire.android.model.PreservedState
import com.wire.android.model.toLoading
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONNECTION_IGNORED_USER_NAME
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.userprofile.common.UsernameMapper.mapUserLabel
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.feature.client.GetOtherUserClientsResult
import com.wire.kalium.logic.feature.client.GetOtherUserClientsUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val getUserInfo: GetUserInfoUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val otherUserClients: GetOtherUserClientsUseCase,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())

    var removeConversationMemberDialogState: PreservedState<RemoveConversationMemberState>?
            by mutableStateOf(null)

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val conversationId: QualifiedID? = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)?.toQualifiedID(qualifiedIdMapper)

    init {
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            when (val result = getUserInfo(userId)) {
                is GetUserInfoResult.Failure -> {
                    appLogger.d("Couldn't not find the user with provided id:$userId.id and domain:$userId.domain")
                    showInfoMessage(InfoMessageType.LoadUserInformationError)
                }
                is GetUserInfoResult.Success ->
                    conversationId
                        .let { if (it != null) observeConversationRoleForUser(it, userId) else flowOf(it) }
                        .collect { loadViewState(result.otherUser, result.team, it) }
            }
        }
        getOtherUserClients()
    }

    private fun loadViewState(otherUser: OtherUser, team: Team?, conversationRoleData: ConversationRoleData?) {
        state = state.copy(
            isDataLoading = false,
            userAvatarAsset = otherUser.completePicture?.let { pic -> ImageAsset.UserAvatarAsset(wireSessionImageLoader, pic) },
            fullName = otherUser.name ?: String.EMPTY,
            userName = mapUserLabel(otherUser),
            teamName = team?.name ?: String.EMPTY,
            email = otherUser.email ?: String.EMPTY,
            phone = otherUser.phone ?: String.EMPTY,
            connectionStatus = otherUser.connectionStatus,
            membership = userTypeMapper.toMembership(otherUser.userType),
            groupState = conversationRoleData?.userRole?.let { userRole ->
                OtherUserProfileGroupState(
                    groupName = conversationRoleData.conversationName,
                    role = userRole,
                    isSelfAdmin = conversationRoleData.selfRole is Member.Role.Admin,
                    conversationId = conversationRoleData.conversationId
                )
            },
            botService = otherUser.botService
        )
    }

    fun openConversation() {
        viewModelScope.launch {
            when (val result = getOrCreateOneToOneConversation(userId)) {
                is CreateConversationResult.Failure -> appLogger.d(("Couldn't retrieve or create the conversation"))
                is CreateConversationResult.Success ->
                    navigationManager.navigate(
                        command = NavigationCommand(
                            destination = NavigationItem.Conversation.getRouteWithArgs(listOf(result.conversation.id)),
                            backStackMode = BackStackMode.UPDATE_EXISTED
                        )
                    )
            }
        }
    }

    fun sendConnectionRequest() {
        viewModelScope.launch {
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is SendConnectionRequestResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionState.SENT)
                    showInfoMessage(InfoMessageType.SuccessConnectionSentRequest)
                }
            }
        }
    }

    fun cancelConnectionRequest() {
        viewModelScope.launch {
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't cancel a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionState.NOT_CONNECTED)
                    showInfoMessage(InfoMessageType.SuccessConnectionCancelRequest)
                }
            }
        }
    }

    fun acceptConnectionRequest() {
        viewModelScope.launch {
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't accept a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionState.ACCEPTED)
                    showInfoMessage(InfoMessageType.SuccessConnectionAcceptRequest)
                }
            }
        }
    }

    fun ignoreConnectionRequest() {
        viewModelScope.launch {
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't ignore a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionState.NOT_CONNECTED)
                    navigationManager.navigateBack(
                        mapOf(
                            EXTRA_CONNECTION_IGNORED_USER_NAME to state.userName,
                        )
                    )
                }
            }
        }
    }

    fun changeMemberRole(role: Member.Role) {
        viewModelScope.launch {
            if (conversationId != null) {
                updateMemberRole(conversationId, userId, role).also {
                    if (it is UpdateConversationMemberRoleResult.Failure)
                        showInfoMessage(InfoMessageType.ChangeGroupRoleError)
                }
            }
        }
    }

    suspend fun showInfoMessage(type: InfoMessageType) {
        _infoMessage.emit(type.uiText)
    }

    fun openRemoveConversationMemberDialog() {
        viewModelScope.launch {
            removeConversationMemberDialogState = PreservedState.State(
                RemoveConversationMemberState(
                    conversationId = conversationId!!,
                    fullName = state.fullName,
                    userName = state.userName,
                    userId = userId
                )
            )
        }
    }

    fun hideRemoveConversationMemberDialog() {
        viewModelScope.launch {
            removeConversationMemberDialogState = null
        }
    }

    fun removeConversationMember(preservedState: PreservedState<RemoveConversationMemberState>) {
        viewModelScope.launch {
            removeConversationMemberDialogState = preservedState.toLoading()
            val response = withContext(dispatchers.io()) {
                removeMemberFromConversation(
                    state.groupState!!.conversationId,
                    userId
                )
            }

            if (response is RemoveMemberFromConversationUseCase.Result.Failure)
                showInfoMessage(InfoMessageType.RemoveConversationMemberError)

            removeConversationMemberDialogState = null

        }
    }

    private fun getOtherUserClients() {
        viewModelScope.launch {
            otherUserClients(userId).let {
                when (it) {
                    is GetOtherUserClientsResult.Failure.UserNotFound -> {
                        appLogger.e("User or Domain not found while fetching user clients ")
                    }
                    is GetOtherUserClientsResult.Failure.Generic -> {
                        appLogger.e("Error while fetching the user clients : ${it.genericFailure}")
                    }
                    is GetOtherUserClientsResult.Success -> {
                        state = state.copy(otherUserClients = it.otherUserClients)
                    }
                }
            }
        }
    }


    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun openBottomSheet() = viewModelScope.launch {
//        if (!state.bottomSheetState.isVisible) {
//            bottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
//            state = state.copy(bottomSheetState = ModalBottomSheetValue.Expanded)
//        }
    }
}

/**
 * We are adding a [randomEventIdentifier] as [UUID], so the msg can be discarded every time after being generated.
 */
enum class InfoMessageType(val uiText: UIText) {
    // connection
    SuccessConnectionSentRequest(UIText.StringResource(R.string.connection_request_sent)),
    SuccessConnectionAcceptRequest(UIText.StringResource(R.string.connection_request_accepted)),
    SuccessConnectionCancelRequest(UIText.StringResource(R.string.connection_request_canceled)),
    ConnectionRequestError(UIText.StringResource(R.string.connection_request_sent_error)),
    LoadUserInformationError(UIText.StringResource(R.string.error_unknown_message)),

    // change group role
    ChangeGroupRoleError(UIText.StringResource(R.string.user_profile_role_change_error)),

    // remove conversation member
    RemoveConversationMemberError(UIText.StringResource(R.string.dialog_remove_conversation_member_error))

}
