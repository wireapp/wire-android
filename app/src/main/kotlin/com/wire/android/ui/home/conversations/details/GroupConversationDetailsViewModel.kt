package com.wire.android.ui.home.conversations.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_GROUP_DELETED_NAME
import com.wire.android.navigation.EXTRA_LEFT_GROUP
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.ui.home.conversations.details.options.GroupConversationOptionsState
import com.wire.android.ui.home.conversations.details.participants.GroupConversationParticipantsViewModel
import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.ui.home.conversationslist.model.GroupDialogState
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.uiText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
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
import kotlinx.coroutines.flow.SharingStarted
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
    savedStateHandle: SavedStateHandle,
    qualifiedIdMapper: QualifiedIdMapper
) : GroupConversationParticipantsViewModel(
    savedStateHandle,
    navigationManager,
    observeConversationMembers,
    qualifiedIdMapper
) {

    init {
        println("cyka init")
    }

    override val maxNumberOfItems: Int get() = MAX_NUMBER_OF_PARTICIPANTS

    private val conversationId: QualifiedID = qualifiedIdMapper.fromStringToQualifiedID(
        savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)!!
    )

    var groupOptionsState: GroupConversationOptionsState by mutableStateOf(GroupConversationOptionsState(conversationId))
    var requestInProgress: Boolean by mutableStateOf(false)

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

            viewModelScope.launch(dispatcher.io()) {
                groupDetailsFlow
                    .collect { groupDetails ->
                        with(groupDetails) {
                            println("cyka update 1")
                            updateState(
                                groupOptionsState.copy(
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

            viewModelScope.launch(dispatcher.io()) {
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

                    println("cyka update 2")
                    updateState(
                        groupOptionsState.copy(
                            isUpdatingAllowed = isSelfAnAdmin,
                            isUpdatingGuestAllowed = isSelfAnAdmin && isSelfInOwnerTeam,
                            isAbleToRemoveGroup = isAbleToRemoveGroup
                        )
                    )
                }
                    .collect {}
            }
        }
    }

    private fun checkIsSelfUserMember() = viewModelScope.launch {
        observeIsSelfUserMember(conversationId).collect { result ->
            groupOptionsState = groupOptionsState.copy(
                isSelfUserMember = when (result) {
                    is IsSelfUserMemberResult.Success -> result.isMember
                    is IsSelfUserMemberResult.Failure -> false
                }
            )
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
        groupOptionsState = groupOptionsState.copy(loadingGuestOption = true, isGuestAllowed = enableGuestAndNonTeamMember)
        when (enableGuestAndNonTeamMember) {
            true -> updateGuestRemoteRequest(enableGuestAndNonTeamMember)
            false -> updateState(groupOptionsState.copy(changeGuestOptionConfirmationRequired = true))
        }
    }

    fun onServicesUpdate(enableServices: Boolean) {
        updateState(groupOptionsState.copy(loadingServicesOption = true, isServicesAllowed = enableServices))
        when (enableServices) {
            true -> updateServicesRemoteRequest(enableServices)
            false -> updateState(groupOptionsState.copy(changeServiceOptionConfirmationRequired = true))
        }
    }

    fun onGuestDialogDismiss() {
        updateState(
            groupOptionsState.copy(
                loadingGuestOption = false,
                changeGuestOptionConfirmationRequired = false,
                isGuestAllowed = !groupOptionsState.isGuestAllowed
            )
        )
    }

    fun onGuestDialogConfirm() {
        updateState(groupOptionsState.copy(changeGuestOptionConfirmationRequired = false, loadingGuestOption = true))
        updateGuestRemoteRequest(false)
    }

    fun onServiceDialogDismiss() {
        updateState(
            groupOptionsState.copy(
                loadingServicesOption = false,
                changeServiceOptionConfirmationRequired = false,
                isServicesAllowed = !groupOptionsState.isServicesAllowed
            )
        )
    }

    fun onServiceDialogConfirm() {
        updateState(groupOptionsState.copy(changeServiceOptionConfirmationRequired = false, loadingServicesOption = true))
        updateServicesRemoteRequest(false)
    }

    private fun updateGuestRemoteRequest(enableGuestAndNonTeamMember: Boolean) {
        viewModelScope.launch {
            updateConversationAccess(enableGuestAndNonTeamMember, groupOptionsState.isServicesAllowed, conversationId).also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                        groupOptionsState.copy(
                            isGuestAllowed = !enableGuestAndNonTeamMember,
                            error = GroupConversationOptionsState.Error.UpdateGuestError(it.cause)
                        )
                    )
                    UpdateConversationAccessRoleUseCase.Result.Success -> Unit
                }
            }
        }.invokeOnCompletion { updateState(groupOptionsState.copy(loadingGuestOption = false)) }
    }

    private fun updateServicesRemoteRequest(enableServices: Boolean) {
        viewModelScope.launch {
            updateConversationAccess(
                enableGuestAndNonTeamMember = groupOptionsState.isGuestAllowed,
                enableServices = enableServices,
                conversationId = conversationId
            ).also {
                when (it) {
                    is UpdateConversationAccessRoleUseCase.Result.Failure -> updateState(
                        groupOptionsState.copy(
                            isServicesAllowed = !enableServices,
                            error = GroupConversationOptionsState.Error.UpdateServicesError(
                                it.cause
                            )
                        )
                    )
                    UpdateConversationAccessRoleUseCase.Result.Success -> Unit
                }
            }
        }.invokeOnCompletion {
            updateState(groupOptionsState.copy(loadingServicesOption = false))
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
        groupOptionsState = newState
    }

    fun navigateToFullParticipantsList() = viewModelScope.launch {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.GroupConversationAllParticipants.getRouteWithArgs(listOf(conversationId))
            )
        )
    }

    fun navigateToAddParticants() = viewModelScope.launch {
        navigationManager.navigate(
            command = NavigationCommand(
                destination = NavigationItem.AddConversationParticipants.getRouteWithArgs(listOf(conversationId))
            )
        )
    }

    companion object {
        const val MAX_NUMBER_OF_PARTICIPANTS = 4
    }
}
