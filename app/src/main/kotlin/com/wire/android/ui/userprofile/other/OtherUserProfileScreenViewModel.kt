package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.getBlockingState
import com.wire.android.ui.userprofile.common.UsernameMapper.mapUserLabel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.BlockUserResult
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCaseResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestResult
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val observeSelfUser: GetSelfUserUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val blockUser: BlockUserUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val getUserInfo: GetUserInfoUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val dispatchers: DispatcherProvider,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())
    var snackBarState: SnackBarState? by mutableStateOf(null)

    private val userId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_USER_ID)!!
    )

    private val conversationId: QualifiedID? =
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)?.let { qualifiedIdMapper.fromStringToQualifiedID(it) }

    init {
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            val userInfoResult = getUserInfo(userId)
            val conversationResult = getOrCreateOneToOneConversation(userId)
            when {
                userInfoResult is GetUserInfoResult.Failure -> {
                    appLogger.d("Couldn't not find the user with provided id: $userId")
                    snackBarState = SnackBarState.LoadUserInformationError()
                }
                conversationResult is CreateConversationResult.Failure -> {
                    appLogger.d("Couldn't not getOrCreateOneToOneConversation for user id: $userId")
                    snackBarState = SnackBarState.LoadUserInformationError()
                }
                conversationResult is CreateConversationResult.Success && userInfoResult is GetUserInfoResult.Success -> {
                    conversationId
                        .let { if (it != null) observeConversationRoleForUser(it, userId) else flowOf(it) }
                        .combine(observeSelfUser(), ::Pair)
                        .collect { (conversationRoleData, selfUser) ->
                            loadViewState(userInfoResult, conversationResult.conversation, conversationRoleData, selfUser.teamId)
                        }
                }
            }
        }
    }

    private fun loadViewState(
        getInfoResult: GetUserInfoResult.Success,
        directConversation: Conversation,
        conversationRoleData: ConversationRoleData?,
        selfTeamId: TeamId?
    ) {
        val userAvatarAsset = getInfoResult.otherUser.completePicture
            ?.let { pic -> ImageAsset.UserAvatarAsset(wireSessionImageLoader, pic) }

        state = state.copy(
            isDataLoading = false,
            userAvatarAsset = userAvatarAsset,
            fullName = getInfoResult.otherUser.name.orEmpty(),
            userName = mapUserLabel(getInfoResult.otherUser),
            teamName = getInfoResult.team?.name.orEmpty(),
            email = getInfoResult.otherUser.email.orEmpty(),
            phone = getInfoResult.otherUser.phone.orEmpty(),
            connectionStatus = getInfoResult.otherUser.connectionStatus,
            membership = userTypeMapper.toMembership(getInfoResult.otherUser.userType),
            groupState = conversationRoleData?.userRole?.let { userRole ->
                OtherUserProfileGroupState(
                    groupName = conversationRoleData.conversationName,
                    role = userRole,
                    isSelfAnAdmin = conversationRoleData.selfRole is Member.Role.Admin
                )
            },
            conversationSheetContent = ConversationSheetContent(
                title = getInfoResult.otherUser.name.orEmpty(),
                conversationId = directConversation.id,
                mutingConversationState = directConversation.mutedStatus,
                conversationTypeDetail = ConversationTypeDetail.Private(
                    userAvatarAsset,
                    userId,
                    getInfoResult.otherUser.getBlockingState(selfTeamId)
                )
            )
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
                    snackBarState = SnackBarState.ConnectionRequestError()
                }
                is SendConnectionRequestResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionState.SENT)
                    snackBarState = SnackBarState.SuccessConnectionSentRequest()
                }
            }
        }
    }

    fun cancelConnectionRequest() {
        viewModelScope.launch {
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't cancel a connect request to user $userId"))
                    snackBarState = SnackBarState.ConnectionRequestError()
                }
                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionState.NOT_CONNECTED)
                    snackBarState = SnackBarState.SuccessConnectionCancelRequest()
                }
            }
        }
    }

    fun acceptConnectionRequest() {
        viewModelScope.launch {
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't accept a connect request to user $userId"))
                    snackBarState = SnackBarState.ConnectionRequestError()
                }
                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionStatus = ConnectionState.ACCEPTED)
                    snackBarState = SnackBarState.SuccessConnectionAcceptRequest()
                }
            }
        }
    }

    fun ignoreConnectionRequest() {
        viewModelScope.launch {
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't ignore a connect request to user $userId"))
                    snackBarState = SnackBarState.ConnectionRequestError()
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

    fun muteConversation(directConversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {
        directConversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(directConversationId, mutedConversationStatus, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> snackBarState = SnackBarState.MutingOperationError()
                    ConversationUpdateStatusResult.Success ->
                        appLogger.d("MutedStatus changed for conversation: $directConversationId to $mutedConversationStatus")
                }
            }
        }
    }

    fun blockUser(id: UserId, userName: String) {
        viewModelScope.launch(dispatchers.io()) {
            val snackBarState = when (val result = blockUser(id)) {
                BlockUserResult.Success -> {
                    appLogger.d("User $id was blocked")
                    SnackBarState.BlockingUserOperationSuccess(userName)
                }
                is BlockUserResult.Failure -> {
                    appLogger.d("Error while blocking user $id ; Error ${result.coreFailure}")
                    SnackBarState.BlockingUserOperationError()
                }
            }
            state = state.copy(blockUserDialogSate = null)
            this@OtherUserProfileScreenViewModel.snackBarState = snackBarState
        }
    }

    fun onDismissBlockUserDialog() {
        state = state.copy(blockUserDialogSate = null)
    }

    fun onBlockUserClicked(id: UserId, name: String) {
        state = state.copy(blockUserDialogSate = BlockUserDialogState(name, id))
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun addConversationToFavourites(id: String = "") {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToFolder(id: String = "") {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun moveConversationToArchive(id: String = "") {
    }

    // TODO: needs to be implemented
    @Suppress("EmptyFunctionBlock")
    fun clearConversationContent(id: String = "") {
    }

    fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

    fun clearSnackBarState() {
        snackBarState = null
    }
}

/**
 * We are adding a [randomEventIdentifier] as [UUID], so the msg can be discarded every time after being generated.
 */
sealed class SnackBarState(private val randomEventIdentifier: UUID) {
    class SuccessConnectionSentRequest : SnackBarState(UUID.randomUUID())
    class SuccessConnectionAcceptRequest : SnackBarState(UUID.randomUUID())
    class SuccessConnectionCancelRequest : SnackBarState(UUID.randomUUID())
    class ConnectionRequestError : SnackBarState(UUID.randomUUID())
    class LoadUserInformationError : SnackBarState(UUID.randomUUID())
    class BlockingUserOperationError : SnackBarState(UUID.randomUUID())
    class BlockingUserOperationSuccess(val name: String) : SnackBarState(UUID.randomUUID())
    class MutingOperationError : SnackBarState(UUID.randomUUID())
}
