package com.wire.android.ui.userprofile.other

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
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.common.dialogs.UnblockUserDialogState
import com.wire.android.ui.home.conversations.details.participants.usecase.ConversationRoleData
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.getBlockingState
import com.wire.android.ui.userprofile.common.UsernameMapper.mapUserLabel
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
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
import com.wire.kalium.logic.feature.client.GetOtherUserClientsResult
import com.wire.kalium.logic.feature.client.GetOtherUserClientsUseCase
import com.wire.kalium.logic.feature.client.PersistOtherUserClientsUseCase
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
import com.wire.kalium.logic.feature.connection.UnblockUserResult
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.CreateConversationResult
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@Suppress("LongParameterList", "TooManyFunctions")
@HiltViewModel
class OtherUserProfileScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider,
    private val observeSelfUser: GetSelfUserUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val blockUser: BlockUserUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    private val getOtherUserClients: GetOtherUserClientsUseCase,
    private val persistOtherUserClients: PersistOtherUserClientsUseCase,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel(), OtherUserProfileEventsHandler, OtherUserProfileBottomSheetEventsHandler, OtherUserProfileFooterEventsHandler {

    var state: OtherUserProfileState by mutableStateOf(OtherUserProfileState())

    var removeConversationMemberDialogState: PreservedState<RemoveConversationMemberState>?
            by mutableStateOf(null)

    var blockUserDialogState: PreservedState<BlockUserDialogState>?
            by mutableStateOf(null)

    var unblockUserDialogState: PreservedState<UnblockUserDialogState>?
            by mutableStateOf(null)

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val conversationId: QualifiedID? = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)?.toQualifiedID(qualifiedIdMapper)

    init {
        state = state.copy(isDataLoading = true)
        viewModelScope.launch {
            when (val conversationResult = getOrCreateOneToOneConversation(userId)) {
                is CreateConversationResult.Failure -> {
                    appLogger.d("Couldn't not getOrCreateOneToOneConversation for user id: $userId")
                    showInfoMessage(InfoMessageType.LoadUserInformationError)
                }
                is CreateConversationResult.Success -> {
                    val observeConversationRoleData = conversationId
                        .let { if (it != null) observeConversationRoleForUser(it, userId) else flowOf(it) }

                    val observeSucceedUserInfo = observeUserInfo(userId)
                        .onEach {
                            if (it is GetUserInfoResult.Failure) {
                                appLogger.d("Couldn't not find the user with provided id: $userId")
                                showInfoMessage(InfoMessageType.LoadUserInformationError)
                            }
                        }
                        .filterIsInstance<GetUserInfoResult.Success>()

                    combine(observeConversationRoleData, observeSelfUser(), observeSucceedUserInfo, ::Triple)
                        .collect { (conversationRoleData, selfUser, userInfoResult) ->
                            loadViewState(userInfoResult, conversationResult.conversation, conversationRoleData, selfUser.teamId)
                        }
                }
            }
        }
        viewModelScope.launch {
            persistOtherUserClients(userId)
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

        state = with(getInfoResult) {
            state.copy(
                isDataLoading = false,
                userAvatarAsset = userAvatarAsset,
                fullName = otherUser.name.orEmpty(),
                userName = mapUserLabel(otherUser),
                teamName = team?.name.orEmpty(),
                email = otherUser.email.orEmpty(),
                phone = otherUser.phone.orEmpty(),
                connectionState = otherUser.connectionStatus,
                membership = userTypeMapper.toMembership(otherUser.userType),
                groupState = conversationRoleData?.userRole?.let { userRole ->
                    OtherUserProfileGroupState(
                        groupName = conversationRoleData.conversationName,
                        role = userRole,
                        isSelfAdmin = conversationRoleData.selfRole is Member.Role.Admin,
                        conversationId = conversationRoleData.conversationId
                    )
                },
                botService = otherUser.botService,
                conversationSheetContent = ConversationSheetContent(
                    title = otherUser.name.orEmpty(),
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
    }

    override fun onOpenConversation() {
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

    override fun onSendConnectionRequest() {
        viewModelScope.launch {
            when (sendConnectionRequest(userId)) {
                is SendConnectionRequestResult.Failure -> {
                    appLogger.d(("Couldn't send a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is SendConnectionRequestResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.SENT)
                    showInfoMessage(InfoMessageType.SuccessConnectionSentRequest)
                }
            }
        }
    }

    override fun onCancelConnectionRequest() {
        viewModelScope.launch {
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't cancel a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.NOT_CONNECTED)
                    showInfoMessage(InfoMessageType.SuccessConnectionCancelRequest)
                }
            }
        }
    }

    override fun onAcceptConnectionRequest() {
        viewModelScope.launch {
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't accept a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.ACCEPTED)
                    showInfoMessage(InfoMessageType.SuccessConnectionAcceptRequest)
                }
            }
        }
    }

    override fun onIgnoreConnectionRequest() {
        viewModelScope.launch {
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't ignore a connect request to user $userId"))
                    showInfoMessage(InfoMessageType.ConnectionRequestError)
                }
                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.NOT_CONNECTED)
                    navigationManager.navigateBack(
                        mapOf(
                            EXTRA_CONNECTION_IGNORED_USER_NAME to state.userName,
                        )
                    )
                }
            }
        }
    }

    override fun onChangeMemberRole(role: Member.Role) {
        viewModelScope.launch {
            if (conversationId != null) {
                updateMemberRole(conversationId, userId, role).also {
                    if (it is UpdateConversationMemberRoleResult.Failure)
                        showInfoMessage(InfoMessageType.ChangeGroupRoleError)
                }
            }
        }
    }

    override suspend fun showInfoMessage(type: InfoMessageType) {
        _infoMessage.emit(type.uiText)
    }

    override fun showRemoveConversationMemberDialog() {
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

    override fun hideRemoveConversationMemberDialog() {
        removeConversationMemberDialogState = null
    }

    override fun onRemoveConversationMember(state: PreservedState<RemoveConversationMemberState>) {
        viewModelScope.launch {
            removeConversationMemberDialogState = state.toLoading()
            val response = withContext(dispatchers.io()) {
                removeMemberFromConversation(
                    state.state.conversationId,
                    userId
                )
            }

            if (response is RemoveMemberFromConversationUseCase.Result.Failure)
                showInfoMessage(InfoMessageType.RemoveConversationMemberError)

            removeConversationMemberDialogState = null
        }
    }

    override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, status, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> showInfoMessage(InfoMessageType.MutingOperationError)
                    ConversationUpdateStatusResult.Success ->
                        appLogger.i("MutedStatus changed for conversation: $conversationId to $status")
                }
            }
        }
    }

    override fun onBlockUser(userId: UserId, userName: String) {
        viewModelScope.launch(dispatchers.io()) {
            blockUserDialogState = blockUserDialogState?.toLoading()
            when (val result = blockUser(userId)) {
                BlockUserResult.Success -> {
                    appLogger.i("User $userId was blocked")
                    showInfoMessage(InfoMessageType.BlockingUserOperationSuccess(userName))
                }
                is BlockUserResult.Failure -> {
                    appLogger.e("Error while blocking user $userId ; Error ${result.coreFailure}")
                    showInfoMessage(InfoMessageType.BlockingUserOperationError)
                }
            }
            blockUserDialogState = null
        }
    }

    override fun hideBlockUserDialog() {
        blockUserDialogState = null
    }

    override fun showBlockUserDialog(userId: UserId, userName: String) {
        blockUserDialogState = PreservedState.State(BlockUserDialogState(userName, userId))
    }

    override fun hideUnblockUserDialog() {
        unblockUserDialogState = null
    }

    override fun showUnblockUserDialog(userName: String) {
        unblockUserDialogState = PreservedState.State(UnblockUserDialogState(userName, userId))
    }

    override fun onUnblockUser(userId: UserId) {
        viewModelScope.launch(dispatchers.io()) {
            unblockUserDialogState = unblockUserDialogState?.toLoading()
            when (val result = unblockUser(userId)) {
                UnblockUserResult.Success -> {
                    appLogger.i("User $userId was unblocked")
                }
                is UnblockUserResult.Failure -> {
                    appLogger.e("Error while unblocking user $userId ; Error ${result.coreFailure}")
                    showInfoMessage(InfoMessageType.UnblockingUserOperationError)
                }
            }
            unblockUserDialogState = null
        }
    }

    @Suppress("EmptyFunctionBlock")
    override fun onAddConversationToFavourites(conversationId: ConversationId) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onMoveConversationToFolder(conversationId: ConversationId) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onMoveConversationToArchive(conversationId: ConversationId) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onClearConversationContent(conversationId: ConversationId) {
    }

    override fun setBottomSheetStateToConversation() {
        state = state.setBottomSheetStateToConversation()
    }

    override fun setBottomSheetStateToMuteOptions() {
        state = state.setBottomSheetStateToMuteOptions()
    }

    override fun setBottomSheetStateToChangeRole() {
        state = state.setBottomSheetStateToChangeRole()
    }

    fun clearBottomSheetState() {
        state = state.clearBottomSheetState()
    }

    override fun getOtherUserClients() {
        viewModelScope.launch {
            getOtherUserClients(userId).let {
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


    override fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }

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

    // remove conversation member
    object RemoveConversationMemberError : InfoMessageType(UIText.StringResource(R.string.dialog_remove_conversation_member_error))

    // Conversation BottomSheet
    object BlockingUserOperationError : InfoMessageType(UIText.StringResource(R.string.error_blocking_user))
    class BlockingUserOperationSuccess(val name: String) : InfoMessageType(UIText.StringResource(R.string.blocking_user_success, name))
    object MutingOperationError : InfoMessageType(UIText.StringResource(R.string.error_updating_muting_setting))

    object UnblockingUserOperationError : InfoMessageType(UIText.StringResource(R.string.error_unblocking_user))
}
