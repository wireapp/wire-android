package com.wire.android.ui.userprofile.other

import MutingOptionsSheetContent
import androidx.activity.compose.BackHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.ui.common.dialogs.BlockUserDialogState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationMainSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.model.BlockState
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.conversation.MutedConversationStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.feature.conversation.ConversationUpdateStatusResult
import com.wire.kalium.logic.feature.conversation.GetOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleResult
import com.wire.kalium.logic.feature.conversation.UpdateConversationMemberRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun OtherUserProfileBottomSheetContent(

) {
    val viewModel: OtherUserProfileBottomSheetViewModel = hiltViewModel()

    when (bottomSheetState) {
        is BottomSheetContent.Conversation -> {
            val conversationId = bottomSheetState.conversationData.conversationId
            ConversationMainSheetContent(
                conversationSheetContent = bottomSheetState.conversationData,
// TODO(profile): enable when implemented
//
//                addConversationToFavourites = { eventsHandler.onAddConversationToFavourites(conversationId) },
//                moveConversationToFolder = { eventsHandler.onMoveConversationToFolder(conversationId) },
//                moveConversationToArchive = { eventsHandler.onMoveConversationToArchive(conversationId) },
//                clearConversationContent = { eventsHandler.onClearConversationContent(conversationId) },
                blockUserClick = blockUser,
                leaveGroup = { },
                deleteGroup = { },
                navigateToNotification = eventsHandler::setBottomSheetStateToMuteOptions
            )
        }
        is BottomSheetContent.Mute ->
            MutingOptionsSheetContent(
                mutingConversationState = bottomSheetState.conversationData.mutingConversationState,
                onMuteConversation = {
                    eventsHandler.onMutingConversationStatusChange(bottomSheetState.conversationData.conversationId, it)
                },
                onBackClick = eventsHandler::setBottomSheetStateToConversation
            )
        is BottomSheetContent.ChangeRole ->
            EditGroupRoleBottomSheet(
                groupState = bottomSheetState.otherUserProfileGroupInfo,
                changeMemberRole = eventsHandler::onChangeMemberRole,
                closeChangeRoleBottomSheet = closeBottomSheet
            )
        null -> {}
    }

    BackHandler(bottomSheetState != null) {
        if (bottomSheetState is BottomSheetContent.Mute) eventsHandler.setBottomSheetStateToConversation()
        else closeBottomSheet()
    }
}

@HiltViewModel
class OtherUserProfileBottomSheetViewModel(
    private val getConversation: GetOneToOneConversationUseCase,
    private val updateMemberRole: UpdateConversationMemberRoleUseCase,
    qualifiedIdMapper: QualifiedIdMapper,
    savedStateHandle: SavedStateHandle,
) : ViewModel(), OtherUserProfileBottomSheetEventsHandler {

    private val userId: QualifiedID = savedStateHandle.get<String>(EXTRA_USER_ID)!!.toQualifiedID(qualifiedIdMapper)

    private val conversationId: QualifiedID? = savedStateHandle.get<String>(EXTRA_CONVERSATION_ID)?.toQualifiedID(qualifiedIdMapper)

    // TODO This could be loaded on demand not on init.
    private fun observeConversationSheetContentIfNeeded(
        otherUser: OtherUser,
        userAvatarAsset: ImageAsset.UserAvatarAsset?
    ) {
        // if we are not connected with that user -> we don't have a direct conversation ->
        // -> no need to load data for ConversationBottomSheet
        if (otherUser.connectionStatus != ConnectionState.ACCEPTED) return

        viewModelScope.launch {
            when (val conversationResult = getConversation(userId)) {
                is GetOneToOneConversationUseCase.Result.Failure -> {
                    appLogger.d("Couldn't not getOrCreateOneToOneConversation for user id: $userId")
                    return@launch
                }
                is GetOneToOneConversationUseCase.Result.Success -> {
                    state = state.copy(
                        conversationSheetContent = ConversationSheetContent(
                            title = otherUser.name.orEmpty(),
                            conversationId = conversationResult.conversation.id,
                            mutingConversationState = conversationResult.conversation.mutedStatus,
                            conversationTypeDetail = ConversationTypeDetail.Private(
                                userAvatarAsset,
                                userId,
                                otherUser.BlockState
                            )
                        )
                    )
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

    override fun onMutingConversationStatusChange(conversationId: ConversationId?, status: MutedConversationStatus) {
        conversationId?.let {
            viewModelScope.launch {
                when (updateConversationMutedStatus(conversationId, status, Date().time)) {
                    ConversationUpdateStatusResult.Failure -> showInfoMessage(OtherUserProfileInfoMessageType.MutingOperationError)
                    ConversationUpdateStatusResult.Success -> {
                        state = state.updateMuteStatus(status)
                        appLogger.i("MutedStatus changed for conversation: $conversationId to $status")
                    }
                }
            }
        }
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

}

data class OtherUserProfileBottomSheetState(
    val otherUserProfileBottomSheetContent: OtherUserProfileBottomSheetContent
)
