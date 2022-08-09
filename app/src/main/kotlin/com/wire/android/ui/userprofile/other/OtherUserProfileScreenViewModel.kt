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
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.Member
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.id.toQualifiedID
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
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.GetUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.util.Date
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
    qualifiedIdMapper: QualifiedIdMapper,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val conversationId: QualifiedID? = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)?.toQualifiedID(qualifiedIdMapper)

    init {
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            val userInfoResult = getUserInfo(userId)
            val conversationResult = getOrCreateOneToOneConversation(userId)
            when {
                userInfoResult is GetUserInfoResult.Failure -> {
                    appLogger.d("Couldn't not find the user with provided id: $userId")
                    showInfoMessage(InfoMessageType.LoadUserInformationError)
                }
                conversationResult is CreateConversationResult.Failure -> {
                    appLogger.d("Couldn't not getOrCreateOneToOneConversation for user id: $userId")
                    showInfoMessage(InfoMessageType.LoadUserInformationError)
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
            botService = getInfoResult.otherUser.botService,
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

                //TODO is it possible to change users role in conversation that we didn't come from but for some other conversation???
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

    fun muteConversation(directConversationId: ConversationId?, mutedConversationStatus: MutedConversationStatus) {
        directConversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(directConversationId, mutedConversationStatus, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> showInfoMessage(InfoMessageType.MutingOperationError)
                    ConversationUpdateStatusResult.Success ->
                        appLogger.d("MutedStatus changed for conversation: $directConversationId to $mutedConversationStatus")
                }
            }
        }
    }

    fun blockUser(id: UserId, userName: String) {
        viewModelScope.launch(dispatchers.io()) {
            when (val result = blockUser(id)) {
                BlockUserResult.Success -> {
                    appLogger.i("User $id was blocked")
                    showInfoMessage(InfoMessageType.BlockingUserOperationSuccess(userName))
                }
                is BlockUserResult.Failure -> {
                    appLogger.e("Error while blocking user $id ; Error ${result.coreFailure}")
                    showInfoMessage(InfoMessageType.BlockingUserOperationError)
                }
            }
            state = state.copy(blockUserDialogSate = null)
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

}

sealed class InfoMessageType(val uiText: UIText) {
    // connection
    object SuccessConnectionSentRequest : InfoMessageType(UIText.StringResource(R.string.connection_request_sent))
    object SuccessConnectionAcceptRequest : InfoMessageType(UIText.StringResource(R.string.connection_request_accepted))
    object SuccessConnectionCancelRequest : InfoMessageType(UIText.StringResource(R.string.connection_request_canceled))
    object ConnectionRequestError : InfoMessageType(UIText.StringResource(R.string.connection_request_sent_error))
    object LoadUserInformationError : InfoMessageType(UIText.StringResource(R.string.error_unknown_message))

    // change group role
    object ChangeGroupRoleError : InfoMessageType(UIText.StringResource(R.string.user_profile_role_change_error))

    // Conversation BottomSheet
    object BlockingUserOperationError : InfoMessageType(UIText.StringResource(R.string.error_blocking_user))
    class BlockingUserOperationSuccess(val name: String) : InfoMessageType(UIText.StringResource(R.string.blocking_user_success, name))
    object MutingOperationError : InfoMessageType(UIText.StringResource(R.string.error_updating_muting_setting));
}
