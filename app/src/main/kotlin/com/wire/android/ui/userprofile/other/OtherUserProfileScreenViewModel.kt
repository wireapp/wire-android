package com.wire.android.ui.userprofile.other

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.di.AssistedViewModel
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavQualifiedId
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.navigation.nav
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.UserId
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList")
@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    override val savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val getUserInfo: GetUserInfoUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase
) : ViewModel(), AssistedViewModel<OtherUserProfileScreenViewModel.Params> {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())
    var connectionOperationState: ConnectionOperationState? by mutableStateOf(null)

    val userId: UserId = param.userId.qualifiedId
    val conversationId: ConversationId? = param.conversationId?.qualifiedId

    init {
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            when (val result = getUserInfo(userId)) {
                is GetUserInfoResult.Failure -> {
                    appLogger.d("Couldn't not find the user with provided id:${userId.value} and domain:${userId.domain}")
                    connectionOperationState = ConnectionOperationState.LoadUserInformationError()
                }
                is GetUserInfoResult.Success -> conversationId
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
            userName = otherUser.handle ?: String.EMPTY,
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
                            destination = VoyagerNavigationItem.Conversation(result.conversation.id.nav()),
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
                    appLogger.d(("Couldn't send a connect request to user ${userId}"))
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
                    appLogger.d(("Couldn't cancel a connect request to user ${userId}"))
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
                    appLogger.d(("Couldn't accept a connect request to user ${userId}"))
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
                    appLogger.d(("Couldn't ignore a connect request to user ${userId}"))
                    connectionOperationState = ConnectionOperationState.ConnectionRequestError()
                }
                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionStatus.NotConnected)
                    param.onConnectionIgnored(state.userName)
                    navigationManager.navigateBack()
                }
            }
        }
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    @Parcelize
    data class Params(
        val userId: NavQualifiedId,
        val conversationId: NavQualifiedId? = null,
        val onConnectionIgnored: (String) -> Unit,
    ): Parcelable
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
