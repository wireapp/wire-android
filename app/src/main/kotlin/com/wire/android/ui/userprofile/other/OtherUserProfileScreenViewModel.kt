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
import com.wire.android.model.SnackBarMessage
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.EXTRA_CONNECTION_IGNORED_USER_NAME
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveConversationRoleForUserUseCase
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.android.ui.userprofile.common.UsernameMapper.toUserLabel
import com.wire.android.ui.userprofile.group.RemoveConversationMemberState
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.BlockingUserOperationError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.BlockingUserOperationSuccess
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.ConnectionAcceptError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.ConnectionCancelError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.ConnectionIgnoreError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.ConnectionRequestError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.LoadUserInformationError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.RemoveConversationMemberError
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.SuccessConnectionAcceptRequest
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.SuccessConnectionCancelRequest
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.SuccessConnectionSentRequest
import com.wire.android.ui.userprofile.other.OtherUserProfileInfoMessageType.UnblockingUserOperationError
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
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
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.user.GetUserInfoResult
import com.wire.kalium.logic.feature.user.ObserveUserInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val blockUser: BlockUserUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val getConversation: GetOneToOneConversationUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
    private val observeUserInfo: ObserveUserInfoUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader,
    private val observeConversationRoleForUser: ObserveConversationRoleForUserUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val getOtherUserClients: GetOtherUserClientsUseCase,
    private val persistOtherUserClients: PersistOtherUserClientsUseCase,
    qualifiedIdMapper: QualifiedIdMapper
) : ViewModel(), OtherUserProfileEventsHandler, OtherUserProfileFooterEventsHandler, OtherUserProfileBottomSheetEventsHandler {

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)
    private val conversationId: QualifiedID? = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)?.toQualifiedID(qualifiedIdMapper)

    private var otherUser: OtherUser? = null

    var state: OtherUserProfileState by mutableStateOf(
        OtherUserProfileState(
            userId = userId,
            conversationId = conversationId
        )
    )

    private val _infoMessage = MutableSharedFlow<UIText>()
    val infoMessage = _infoMessage.asSharedFlow()

    init {
        observeUserInfo()
        persistClients()
    }

    private fun persistClients() {
        viewModelScope.launch(dispatchers.io()) {
            persistOtherUserClients(userId)
        }
    }

    private fun observeUserInfo() {
        viewModelScope.launch {
            observeUserInfo(userId).combine(observeGroupInfo(), ::Pair)
                .flowOn(dispatchers.io())
                .onStart { state = state.copy(isLoading = true) }
                .collect { (userInfoResult, groupInfoAvailability) ->
                    when (userInfoResult) {
                        GetUserInfoResult.Failure -> {
                            appLogger.d("Couldn't not find the user with provided id: $userId")
                            showInfoMessage(LoadUserInformationError)
                        }
                        is GetUserInfoResult.Success -> {
                            otherUser = userInfoResult.otherUser

                            otherUser?.let {
                                with(it) {
                                    val userAvatarAsset = completePicture
                                        ?.let { userAssetId ->
                                            ImageAsset.UserAvatarAsset(
                                                imageLoader = wireSessionImageLoader,
                                                userAssetId = userAssetId
                                            )
                                        }

                                    state = state.copy(
                                        isLoading = false,
                                        userAvatarAsset = userAvatarAsset,
                                        fullName = name.orEmpty(),
                                        userName = toUserLabel(),
                                        teamName = userInfoResult.team?.name.orEmpty(),
                                        email = email.orEmpty(),
                                        phone = phone.orEmpty(),
                                        connectionState = connectionStatus,
                                        membership = userTypeMapper.toMembership(userType),
                                        botService = botService,
                                        groupInfoAvailability = groupInfoAvailability,
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    fun getAdditionalConversationDetails() {
        if (state.conversationSheetContent == null) {
            viewModelScope.launch {
                when (val conversationResult = withContext(dispatchers.io()) { getConversation(userId) }) {
                    is GetOneToOneConversationUseCase.Result.Failure -> {
                        appLogger.d("Couldn't not getOrCreateOneToOneConversation for user id: $userId")
                    }
                    is GetOneToOneConversationUseCase.Result.Success -> {
                        state = state.copy(
                            conversationSheetContent = ConversationSheetContent(
                                title = otherUser!!.name.orEmpty(),
                                conversationId = conversationResult.conversation.id,
                                mutingConversationState = conversationResult.conversation.mutedStatus,
                                conversationTypeDetail = ConversationTypeDetail.Private(
                                    state.userAvatarAsset,
                                    userId,
                                    otherUser!!.BlockState
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun observeGroupInfo(): Flow<GroupInfoAvailability> {
        return conversationId?.let {
            observeConversationRoleForUser(it, userId)
                .map { conversationRoleData ->
                    conversationRoleData.userRole?.let { userRole ->
                        GroupInfoAvailability.Available(
                            OtherUserProfileGroupInfo(
                                groupName = conversationRoleData.conversationName,
                                role = userRole,
                                isSelfAdmin = conversationRoleData.selfRole is Conversation.Member.Role.Admin,
                                conversationId = conversationRoleData.conversationId
                            )
                        )
                    } ?: GroupInfoAvailability.NotAvailable
                }
        } ?: flowOf(GroupInfoAvailability.NotAvailable)
    }

    override fun onOpenConversation() {
        viewModelScope.launch {
            when (val result = withContext(dispatchers.io()) { getOrCreateOneToOneConversation(userId) }) {
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
                    showInfoMessage(ConnectionRequestError)
                }
                is SendConnectionRequestResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.SENT)
                    showInfoMessage(SuccessConnectionSentRequest)
                }
            }
        }
    }

    override fun onCancelConnectionRequest() {
        viewModelScope.launch {
            when (cancelConnectionRequest(userId)) {
                is CancelConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't cancel a connect request to user $userId"))
                    showInfoMessage(ConnectionCancelError)
                }
                is CancelConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.NOT_CONNECTED)
                    showInfoMessage(SuccessConnectionCancelRequest)
                }
            }
        }
    }

    override fun onChangeMemberRole(role: Conversation.Member.Role) {
        viewModelScope.launch {
            if (conversationId != null) {
                updateMemberRole(conversationId, userId, role).also {
                    if (it is UpdateConversationMemberRoleResult.Failure)
                        showInfoMessage(OtherUserProfileInfoMessageType.ChangeGroupRoleError)
                }
            }
        }
    }

    override fun onAcceptConnectionRequest() {
        viewModelScope.launch {
            when (acceptConnectionRequest(userId)) {
                is AcceptConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't accept a connect request to user $userId"))
                    showInfoMessage(ConnectionAcceptError)
                }
                is AcceptConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.ACCEPTED)
                    showInfoMessage(SuccessConnectionAcceptRequest)
                }
            }
        }
    }

    override fun onIgnoreConnectionRequest() {
        viewModelScope.launch {
            when (ignoreConnectionRequest(userId)) {
                is IgnoreConnectionRequestUseCaseResult.Failure -> {
                    appLogger.d(("Couldn't ignore a connect request to user $userId"))
                    showInfoMessage(ConnectionIgnoreError)
                }
                is IgnoreConnectionRequestUseCaseResult.Success -> {
                    state = state.copy(connectionState = ConnectionState.IGNORED)
                    navigationManager.navigateBack(
                        mapOf(
                            EXTRA_CONNECTION_IGNORED_USER_NAME to state.userName,
                        )
                    )
                }
            }
        }
    }

    override fun onRemoveConversationMember(removeConversationMemberState: RemoveConversationMemberState) {
        viewModelScope.launch {
            state = state.copy(requestInProgress = true)
            val response = withContext(dispatchers.io()) {
                removeMemberFromConversation(
                    removeConversationMemberState.conversationId,
                    userId
                )
            }

            if (response is RemoveMemberFromConversationUseCase.Result.Failure)
                showInfoMessage(RemoveConversationMemberError)

            state = state.copy(requestInProgress = false)
        }
    }

    override fun onBlockUser(blockUserState: BlockUserDialogState) {
        viewModelScope.launch {
            state = state.copy(requestInProgress = true)
            when (val result = withContext(dispatchers.io()) { blockUser(userId) }) {
                BlockUserResult.Success -> {
                    appLogger.i("User $userId was blocked")
                    showInfoMessage(BlockingUserOperationSuccess(blockUserState.userName))
                }
                is BlockUserResult.Failure -> {
                    appLogger.e("Error while blocking user $userId ; Error ${result.coreFailure}")
                    showInfoMessage(BlockingUserOperationError)
                }
            }
            state = state.copy(requestInProgress = false)
        }
    }

    override fun onUnblockUser(userId: UserId) {
        viewModelScope.launch {
            state = state.copy(requestInProgress = true)
            when (val result = withContext(dispatchers.io()) { unblockUser(userId) }) {
                UnblockUserResult.Success -> {
                    appLogger.i("User $userId was unblocked")
                }
                is UnblockUserResult.Failure -> {
                    appLogger.e("Error while unblocking user $userId ; Error ${result.coreFailure}")
                    showInfoMessage(UnblockingUserOperationError)
                }
            }
            state = state.copy(requestInProgress = false)
        }
    }

    override fun fetchOtherUserClients() {
        viewModelScope.launch {
            when (val result = withContext(dispatchers.io()) { getOtherUserClients(userId) }) {
                is GetOtherUserClientsResult.Failure.UserNotFound -> {
                    appLogger.e("User or Domain not found while fetching user clients ")
                }
                is GetOtherUserClientsResult.Failure.Generic -> {
                    appLogger.e("Error while fetching the user clients : ${result.genericFailure}")
                }
                is GetOtherUserClientsResult.Success -> {
                    state = state.copy(otherUserClients = result.otherUserClients)
                }
            }
        }
    }

    private suspend fun showInfoMessage(type: SnackBarMessage) {
        _infoMessage.emit(type.uiText)
    }

    override fun onMutingConversationStatusChange(status: MutedConversationStatus) {
        state.conversationSheetContent?.conversationId?.let { conversationId ->
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, status, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> showInfoMessage(OtherUserProfileInfoMessageType.MutingOperationError)
                    ConversationUpdateStatusResult.Success -> {
                        state = state.copy(
                            conversationSheetContent = state.conversationSheetContent!!.copy(mutingConversationState = status)
                        )
                        appLogger.i("MutedStatus changed for conversation: $conversationId to $status")
                    }
                }
            }
        }
    }

    @Suppress("EmptyFunctionBlock")
    override fun onAddConversationToFavourites() {

    }

    @Suppress("EmptyFunctionBlock")
    override fun onMoveConversationToFolder() {

    }

    @Suppress("EmptyFunctionBlock")
    override fun onMoveConversationToArchive() {

    }

    @Suppress("EmptyFunctionBlock")
    override fun onClearConversationContent() {

    }

    override fun navigateBack() = viewModelScope.launch { navigationManager.navigateBack() }
}
