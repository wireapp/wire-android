package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.R
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_GROUP_DELETED_NAME
import com.wire.android.navigation.EXTRA_LEFT_GROUP
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.DialogState
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UIText
import com.wire.android.util.uiText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.IsSelfUserMemberResult
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.ObserveIsSelfUserMemberUseCase
import com.wire.kalium.logic.feature.conversation.RemoveMemberFromConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationAccessRoleUseCase
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val observeIsSelfUserMember: ObserveIsSelfUserMemberUseCase,
    private val clearConversationContentUseCase: ClearConversationContentUseCase,
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : GroupConversationParticipantsViewModel(
    savedStateHandle,
    navigationManager,
    observeConversationMembers,
    qualifiedIdMapper
) {

    override val maxNumberOfItems: Int get() = MAX_NUMBER_OF_PARTICIPANTS

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    private val _groupOptionsState = MutableStateFlow(GroupConversationOptionsState(conversationId))
    val groupOptionsState: StateFlow<GroupConversationOptionsState> = _groupOptionsState

    var requestInProgress: Boolean by mutableStateOf(false)
        private set

    init {
        observeConversationDetails()
        checkIsSelfUserMember()
    }

    private fun observeConversationDetails() {
        viewModelScope.launch(dispatcher.io()) {

            val groupDetailsFlow = observeConversationDetails(conversationId)
                .filterIsInstance<ObserveConversationDetailsUseCase.Result.Success>()
                .map { it.conversationDetails }
                .filterIsInstance<ConversationDetails.Group>()
                .distinctUntilChanged()
                .shareIn(this, SharingStarted.WhileSubscribed(), 1)

            val isSelfAdminFlow = observeConversationMembers(conversationId)
                .map { it.isSelfAnAdmin }
                .distinctUntilChanged()

            launch {
                groupDetailsFlow
                    .collect { groupDetails ->
                        with(groupDetails) {
                            updateState(
                                groupOptionsState.value.copy(
                                    groupName = conversation.name.orEmpty(),
                                    protocolInfo = conversation.protocol,
                                    areAccessOptionsAvailable = conversation.isTeamGroup(),
                                    isGuestAllowed = (conversation.isGuestAllowed() || conversation.isNonTeamMemberAllowed()),
                                    isServicesAllowed = conversation.isServicesAllowed(),
                                )
                            )
                        }
                    }
            }

            launch {
                combine(
                    observerSelfUser().take(1),
                    groupDetailsFlow,
                    isSelfAdminFlow,
                    getSelfTeam()
                ) { selfUser, groupDetails, isSelfAnAdmin, selfTeam ->

                    val isSelfInOwnerTeam =
                        selfTeam?.id != null && selfTeam.id == groupDetails.conversation.teamId?.value

                    val isAbleToRemoveGroup = (selfUser.teamId != null
                            && groupDetails.conversation.creatorId.value == selfUser.id.value)

                    updateState(
                        groupOptionsState.value.copy(
                            isUpdatingAllowed = isSelfAnAdmin,
                            isUpdatingGuestAllowed = isSelfAnAdmin && isSelfInOwnerTeam,
                            isAbleToRemoveGroup = isAbleToRemoveGroup,
                        )
                    )
                }
                    .collect {}
            }
        }
    }

    private fun checkIsSelfUserMember() = viewModelScope.launch(dispatcher.io()) {
        observeIsSelfUserMember(conversationId).collect { result ->
            updateState(
                groupOptionsState.value.copy(
                    isSelfUserMember = when (result) {
                        is IsSelfUserMemberResult.Success -> result.isMember
                        is IsSelfUserMemberResult.Failure -> false
                    }
                )
            )
        }
    }

    //TODO: some duplication here with ConversationListViewModel
    fun leaveGroup(leaveGroupState: GroupDialogState) {
        executeWithProgress {
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
        }
    }

    //TODO: some duplication here with ConversationListViewModel
    fun deleteGroup(groupState: GroupDialogState) {
        executeWithProgress {
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
    }

    fun clearConversationContent(dialogState: DialogState) {
        with(dialogState) {
            executeWithProgress {
                val result = withContext(dispatcher.io()) { clearConversationContentUseCase(conversationId) }
                clearContentSnackbarResult(result, conversationTypeDetail)
            }
        }
    }

    private suspend fun clearContentSnackbarResult(
        clearContentResult: ClearConversationContentUseCase.Result,
        conversationTypeDetail: ConversationTypeDetail
    ) {
        if (conversationTypeDetail is ConversationTypeDetail.Connection) throw IllegalStateException("Unsupported conversation type to clear content, something went wrong?")

        if (clearContentResult is ClearConversationContentUseCase.Result.Failure) {
            showSnackBarMessage(UIText.StringResource(R.string.group_content_delete_failure))
        } else {
            showSnackBarMessage(UIText.StringResource(R.string.group_content_deleted))
        }
    }

    fun onGuestUpdate(enableGuestAndNonTeamMember: Boolean) {
        viewModelScope.launch {
            updateState(groupOptionsState.value.copy(loadingGuestOption = true, isGuestAllowed = enableGuestAndNonTeamMember))
            when (enableGuestAndNonTeamMember) {
                true -> updateGuestRemoteRequest(enableGuestAndNonTeamMember)
                false -> updateState(groupOptionsState.value.copy(changeGuestOptionConfirmationRequired = true))
            }
        }
    }

    fun onServicesUpdate(enableServices: Boolean) {
        viewModelScope.launch {
            updateState(groupOptionsState.value.copy(loadingServicesOption = true, isServicesAllowed = enableServices))
            when (enableServices) {
                true -> updateServicesRemoteRequest(enableServices)
                false -> updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = true))
            }
        }
    }

    fun onGuestDialogDismiss() {
        viewModelScope.launch {
            updateState(
                groupOptionsState.value.copy(
                    loadingGuestOption = false,
                    changeGuestOptionConfirmationRequired = false,
                    isGuestAllowed = !groupOptionsState.value.isGuestAllowed
                )
            )
        }
    }

    fun onGuestDialogConfirm() {
        viewModelScope.launch {
            updateState(groupOptionsState.value.copy(changeGuestOptionConfirmationRequired = false, loadingGuestOption = true))
            updateGuestRemoteRequest(false)
        }
    }

    fun onServiceDialogDismiss() {
        viewModelScope.launch {
            updateState(
                groupOptionsState.value.copy(
                    loadingServicesOption = false,
                    changeServiceOptionConfirmationRequired = false,
                    isServicesAllowed = !groupOptionsState.value.isServicesAllowed
                )
            )
        }
    }

    fun onServiceDialogConfirm() {
        viewModelScope.launch {
            updateState(groupOptionsState.value.copy(changeServiceOptionConfirmationRequired = false, loadingServicesOption = true))
            updateServicesRemoteRequest(false)
        }
    }

    private fun updateGuestRemoteRequest(enableGuestAndNonTeamMember: Boolean) {
        viewModelScope.launch {
            updateConversationAccess(enableGuestAndNonTeamMember, groupOptionsState.value.isServicesAllowed, conversationId).also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                        groupOptionsState.value.copy(
                            isGuestAllowed = !enableGuestAndNonTeamMember,
                            error = GroupConversationOptionsState.Error.UpdateGuestError(it.cause)
                        )
                    )
                    UpdateConversationAccessRoleUseCase.Result.Success -> Unit
                }
            }

            updateState(groupOptionsState.value.copy(loadingGuestOption = false))
        }
    }

    private fun updateServicesRemoteRequest(enableServices: Boolean) {
        viewModelScope.launch {
            updateConversationAccess(
                enableGuestAndNonTeamMember = groupOptionsState.value.isGuestAllowed,
                enableServices = enableServices,
                conversationId = conversationId
            ).also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                        groupOptionsState.value.copy(
                            isServicesAllowed = !enableServices,
                            error = GroupConversationOptionsState.Error.UpdateServicesError(it.cause)
                        )
                    )
                    UpdateConversationAccessRoleUseCase.Result.Success -> Unit
                }
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

    private suspend fun updateState(newState: GroupConversationOptionsState) {
        _groupOptionsState.emit(newState)
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

    //TODO: duplication here with ConversationViewModel, think about some progress mechanism ?
    private fun executeWithProgress(request: suspend () -> Unit) {
        requestInProgress = true

        viewModelScope.launch {
            request()
            requestInProgress = false
        }
    }

    companion object {
        const val MAX_NUMBER_OF_PARTICIPANTS = 4
    }
}
