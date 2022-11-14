package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_GROUP_DELETED_NAME
import com.wire.android.navigation.EXTRA_GROUP_NAME_CHANGED
import com.wire.android.navigation.EXTRA_LEFT_GROUP
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.getBackNavArg
import com.wire.android.ui.common.bottomsheet.conversation.ConversationSheetContent
import com.wire.android.ui.common.bottomsheet.conversation.ConversationTypeDetail
import com.wire.android.ui.home.conversations.details.menu.GroupConversationDetailsBottomSheetEventsHandler
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.ui.toUIText
import com.wire.android.util.uiText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.team.GetSelfTeamUseCase
import com.wire.kalium.logic.feature.team.Result
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class GroupConversationDetailsViewModel @Inject constructor(
    private val navigationManager: NavigationManager,
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val updateConversationAccessRole: UpdateConversationAccessRoleUseCase,
    private val getSelfTeam: GetSelfTeamUseCase,
    private val observerSelfUser: GetSelfUserUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val removeMemberFromConversation: RemoveMemberFromConversationUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : GroupConversationParticipantsViewModel(
    savedStateHandle,
    navigationManager,
    observeConversationMembers,
    qualifiedIdMapper
), GroupConversationDetailsBottomSheetEventsHandler {

    override val maxNumberOfItems: Int get() = MAX_NUMBER_OF_PARTICIPANTS

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    var conversationSheetContent: ConversationSheetContent? by mutableStateOf(null)
        private set

    private val _groupOptionsState = MutableStateFlow(GroupConversationOptionsState(conversationId))
    val groupOptionsState: StateFlow<GroupConversationOptionsState> = _groupOptionsState

    var requestInProgress: Boolean by mutableStateOf(false)
        private set

    init {
        observeConversationDetails()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch {
            val groupDetailsFlow = observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                .map { it.conversationDetails }
                .filterIsInstance<ConversationDetails.Group>()
                .distinctUntilChanged()
                .flowOn(dispatcher.io())
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            val isSelfAdminFlow = observeConversationMembers(conversationId)
                .map { it.isSelfAnAdmin }
                .distinctUntilChanged()

            combine(
                groupDetailsFlow,
                isSelfAdminFlow,
                getSelfTeam(),
            ) { groupDetails, isSelfAnAdmin, selfTeam ->

                val isSelfInOwnerTeam =
                    selfTeam?.id != null && selfTeam.id == groupDetails.conversation.teamId?.value

                conversationSheetContent = ConversationSheetContent(
                    title = groupDetails.conversation.name.orEmpty(),
                    conversationId = conversationId,
                    mutingConversationState = groupDetails.conversation.mutedStatus,
                    conversationTypeDetail = ConversationTypeDetail.Group(conversationId, groupDetails.isSelfUserCreator),
                    isSelfUserMember = groupDetails.isSelfUserMember
                )
                val isGuestAllowed = groupDetails.conversation.isGuestAllowed() || groupDetails.conversation.isNonTeamMemberAllowed()

                updateState(
                    groupOptionsState.value.copy(
                        groupName = groupDetails.conversation.name.orEmpty(),
                        protocolInfo = groupDetails.conversation.protocol,
                        areAccessOptionsAvailable = groupDetails.conversation.isTeamGroup(),
                        isGuestAllowed = isGuestAllowed,
                        isServicesAllowed = groupDetails.conversation.isServicesAllowed(),
                        isUpdatingAllowed = isSelfAnAdmin,
                        isUpdatingGuestAllowed = isSelfAnAdmin && isSelfInOwnerTeam,
                    )
                )
            }.collect {}
        }
    }

    fun leaveGroup(leaveGroupState: GroupDialogState) {
        viewModelScope.launch {
            requestInProgress = true
            val response = withContext(dispatcher.io()) {
                val selfUser = observerSelfUser().first()
                removeMemberFromConversation(
                    leaveGroupState.conversationId,
                    selfUser.id
                )
            }
            when (response) {
                is RemoveMemberFromConversationUseCase.Result.Failure ->
                    showSnackBarMessage(response.cause.uiText())
                RemoveMemberFromConversationUseCase.Result.Success -> {
                    navigationManager.navigateBack(
                        mapOf(
                            EXTRA_LEFT_GROUP to true,
                        )
                    )
                }
            }
            requestInProgress = false
        }
    }

    fun deleteGroup(groupState: GroupDialogState) {
        viewModelScope.launch {
            requestInProgress = true
            when (val response = withContext(dispatcher.io()) { deleteTeamConversation(groupState.conversationId) }) {
                is Result.Failure.GenericFailure -> showSnackBarMessage(response.coreFailure.uiText())
                Result.Failure.NoTeamFailure -> showSnackBarMessage(CoreFailure.Unknown(null).uiText())
                Result.Success -> {
                    navigationManager.navigateBack(
                        mapOf(
                            EXTRA_GROUP_DELETED_NAME to groupState.conversationName,
                        )
                    )
                }
            }
        }
        requestInProgress = false
    }

    fun onGuestUpdate(enableGuestAndNonTeamMember: Boolean) {
        updateState(groupOptionsState.value.copy(loadingGuestOption = true, isGuestAllowed = enableGuestAndNonTeamMember))
        when (enableGuestAndNonTeamMember) {
            true -> updateGuestRemoteRequest(enableGuestAndNonTeamMember)
            false -> updateState(groupOptionsState.value.copy(changeGuestOptionConfirmationRequired = true))
        }
    }

    fun onServicesUpdate(enableServices: Boolean) {
        updateState(groupOptionsState.value.copy(loadingServicesOption = true, isServicesAllowed = enableServices))
        when (enableServices) {
            true -> updateServicesRemoteRequest(enableServices)
            false -> updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = true))
        }
    }

    fun onGuestDialogDismiss() {
        updateState(
            groupOptionsState.value.copy(
                loadingGuestOption = false,
                changeGuestOptionConfirmationRequired = false,
                isGuestAllowed = !groupOptionsState.value.isGuestAllowed
            )
        )
    }

    fun onGuestDialogConfirm() {
        updateState(groupOptionsState.value.copy(changeGuestOptionConfirmationRequired = false, loadingGuestOption = true))
        updateGuestRemoteRequest(false)
    }

    fun onServiceDialogDismiss() {
        updateState(
            groupOptionsState.value.copy(
                loadingServicesOption = false,
                changeServiceOptionConfirmationRequired = false,
                isServicesAllowed = !groupOptionsState.value.isServicesAllowed
            )
        )
    }

    fun onServiceDialogConfirm() {
        updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = false, loadingServicesOption = true))
        updateServicesRemoteRequest(false)
    }

    private fun updateGuestRemoteRequest(enableGuestAndNonTeamMember: Boolean) {
        viewModelScope.launch {
            val result = withContext(dispatcher.io()) {
                updateConversationAccess(
                    enableGuestAndNonTeamMember,
                    groupOptionsState.value.isServicesAllowed,
                    conversationId
                )
            }

            when (result) {
                is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                    groupOptionsState.value.copy(
                        isGuestAllowed = !enableGuestAndNonTeamMember,
                        error = GroupConversationOptionsState.Error.UpdateGuestError(result.cause)
                    )
                )
                UpdateConversationAccessRoleUseCase.Result.Success -> Unit
            }

            updateState(groupOptionsState.value.copy(loadingGuestOption = false))
        }
    }

    private fun updateServicesRemoteRequest(enableServices: Boolean) {
        viewModelScope.launch {
            val result = withContext(dispatcher.io()) {
                updateConversationAccess(
                    enableGuestAndNonTeamMember = groupOptionsState.value.isGuestAllowed,
                    enableServices = enableServices,
                    conversationId = conversationId
                )
            }

            when (result) {
                is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                    groupOptionsState.value.copy(
                        isServicesAllowed = !enableServices,
                        error = GroupConversationOptionsState.Error.UpdateServicesError(result.cause)
                    )
                )
                UpdateConversationAccessRoleUseCase.Result.Success -> Unit
            }

            updateState(groupOptionsState.value.copy(loadingServicesOption = false))
        }
    }

    private suspend fun updateConversationAccess(
        enableGuestAndNonTeamMember: Boolean,
        enableServices: Boolean,
        conversationId: ConversationId
    ) = updateConversationAccessRole(
        allowGuest = enableGuestAndNonTeamMember,
        allowNonTeamMember = enableGuestAndNonTeamMember,
        allowServices = enableServices,
        conversationId = conversationId
    )

    private fun updateState(newState: GroupConversationOptionsState) {
        _groupOptionsState.value = newState
    }

    fun navigateToFullParticipantsList() = viewModelScope.launch {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.GroupConversationAllParticipants.getRouteWithArgs(listOf(conversationId))
            )
        )
    }

    fun navigateToAddParticipants() = viewModelScope.launch {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.AddConversationParticipants.getRouteWithArgs(listOf(conversationId))
            )
        )
    }

    override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, status, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> {
                        showSnackBarMessage(UIText.StringResource(R.string.error_updating_muting_setting))
                    }
                    ConversationUpdateStatusResult.Success -> {
                        appLogger.i("MutedStatus changed for conversation: $conversationId to $status")
                    }
                }
            }
        }
    }

    @Suppress("EmptyFunctionBlock")
    override fun onAddConversationToFavourites(conversationId: ConversationId?) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onMoveConversationToFolder(conversationId: ConversationId?) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onMoveConversationToArchive(conversationId: ConversationId?) {
    }

    @Suppress("EmptyFunctionBlock")
    override fun onClearConversationContent(conversationId: ConversationId?) {
    }

    fun navigateToEditGroupName() {
        viewModelScope.launch {
            navigationManager.navigate(
                command = NavigationCommand(
                    destination = NavigationItem.EditConversationName.getRouteWithArgs(listOf(conversationId))
                )
            )
        }
    }

    suspend fun checkForPendingMessages() {
        with(savedStateHandle) {
            when (getBackNavArg<Boolean>(EXTRA_GROUP_NAME_CHANGED)) {
                true -> showSnackBarMessage("hola".toUIText())
                false -> showSnackBarMessage("nooo".toUIText())
                else -> showSnackBarMessage("null ?".toUIText())
            }
        }
    }

    companion object {
        const val MAX_NUMBER_OF_PARTICIPANTS = 4
    }
}
