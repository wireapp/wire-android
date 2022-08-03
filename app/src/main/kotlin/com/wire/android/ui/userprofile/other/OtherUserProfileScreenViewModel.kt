package com.wire.android.ui.userprofile.other

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset
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
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.OtherUser
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
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val getUserInfo: GetUserInfoUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())
    var connectionOperationState: ConnectionOperationState? by mutableStateOf(null)

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val conversationId: QualifiedID? = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)?.toQualifiedID(qualifiedIdMapper)

    init {
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            when (val result = getUserInfo(userId)) {
                is GetUserInfoResult.Failure -> {
                    appLogger.d("Couldn't not find the user with provided id:$userId.id and domain:$userId.domain")
                    connectionOperationState = ConnectionOperationState.LoadUserInformationError()
                }
                is GetUserInfoResult.Success ->
                    conversationId
                        .let { if (it != null) observeConversationRoleForUser(it, userId) else flowOf(it) }
                        .collect { loadViewState(result.otherUser, result.team, it) }
            }
        }
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
            connectionStatus = otherUser.connectionStatus.toOtherUserProfileConnectionStatus(),
            membership = userTypeMapper.toMembership(otherUser.userType),
            groupState = conversationRoleData?.userRole?.let { userRole ->
                OtherUserProfileGroupState(
                    groupName = conversationRoleData.conversationName,
                    role = userRole,
                    isSelfAnAdmin = conversationRoleData.selfRole is Member.Role.Admin
                )
            }
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
                    connectionOperationState = ConnectionOperationState.ConnectionRequestError()
                }
                is SendConnectionRequestResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionStatus.Sent)
                    connectionOperationState = ConnectionOperationState.SuccessConnectionSentRequest()
                }
            }
        }
    }

    fun cancelConnectionRequest() {
        viewModelScope.launch {
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't cancel a connect request to user $userId"))
                    connectionOperationState = ConnectionOperationState.ConnectionRequestError()
                }
                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionStatus.NotConnected)
                    connectionOperationState = ConnectionOperationState.SuccessConnectionCancelRequest()
                }
            }
        }
    }

    fun acceptConnectionRequest() {
        viewModelScope.launch {
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't accept a connect request to user $userId"))
                    connectionOperationState = ConnectionOperationState.ConnectionRequestError()
                }
                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionStatus.Connected)
                    connectionOperationState = ConnectionOperationState.SuccessConnectionAcceptRequest()
                }
            }
        }
    }

    fun ignoreConnectionRequest() {
        viewModelScope.launch {
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't ignore a connect request to user $userId"))
                    connectionOperationState = ConnectionOperationState.ConnectionRequestError()
                }
                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionStatus.NotConnected)
                    navigationManager.navigateBack(
                        mapOf(
                            EXTRA_CONNECTION_IGNORED_USER_NAME to state.userName,
                        )
                    )
                }
            }
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}

/**
 * We are adding a [randomEventIdentifier] as [UUID], so the msg can be discarded every time after being generated.
 */
sealed class ConnectionOperationState(private val randomEventIdentifier: UUID) {
    class SuccessConnectionSentRequest : ConnectionOperationState(UUID.randomUUID())
    class SuccessConnectionAcceptRequest : ConnectionOperationState(UUID.randomUUID())
    class SuccessConnectionCancelRequest : ConnectionOperationState(UUID.randomUUID())
    class ConnectionRequestError : ConnectionOperationState(UUID.randomUUID())
    class LoadUserInformationError : ConnectionOperationState(UUID.randomUUID())
}
