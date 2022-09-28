package com.wire.android.ui.userprofile.other

import androidx.activity.compose.BackHandler
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.appLogger
import com.wire.android.model.ImageAsset
import com.wire.android.navigation.EXTRA_CONVERSATION_ID
import com.wire.android.navigation.EXTRA_USER_ID
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationNavigationOptions
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetState
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationTypeDetail
import com.wire.android.ui.home.conversationslist.bottomsheet.OtherUserNavigationOption
import com.wire.android.ui.home.conversationslist.bottomsheet.OtherUserNavigationOptions
import com.wire.android.ui.home.conversationslist.bottomsheet.Test
import com.wire.android.ui.home.conversationslist.bottomsheet.rememberConversationSheetState
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
fun OtherUserProfileBottomSheet(
    otherUserProfileBottomSheetState: OtherUserProfileBottomSheetState
) {

    when (otherUserProfileBottomSheetState) {
        OtherUserProfileBottomSheetState.NotRequested -> {}
        is OtherUserProfileBottomSheetState.Requested -> {
        }
    }


}

@Composable
fun OtherUSerProfileBottomSheetContent(
    otherUserNavigationOptions: OtherUserNavigationOptions,
) {
    val otherUserProfileScreenViewModel: OtherUserProfileScreenViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        otherUserProfileScreenViewModel.getAdditionalConversationDetails()
    }

    when (otherUserNavigationOptions) {
        is ConversationNavigationOptions.Home, ConversationNavigationOptions.MutingOptionsNotification -> {
            ConversationSheetState(conversationNavigationOptions = otherUserNavigationOptions)
        }
        is OtherUserNavigationOption.ChangeRole -> {
            EditGroupRoleBottomSheet(
                groupState = bottomSheetState.otherUserProfileGroupInfo,
                changeMemberRole = eventsHandler::onChangeMemberRole,
                closeChangeRoleBottomSheet = closeBottomSheet
//            )
        }
//        is OtherUserBottomSheetContent.Conversation -> {
//            val conversationId = bottomSheetState.conversationData.conversationId
//            ConversationMainSheetContent(
//                conversationSheetContent = bottomSheetState.conversationData,
//// TODO(profile): enable when implemented
////
////                addConversationToFavourites = { eventsHandler.onAddConversationToFavourites(conversationId) },
////                moveConversationToFolder = { eventsHandler.onMoveConversationToFolder(conversationId) },
////                moveConversationToArchive = { eventsHandler.onMoveConversationToArchive(conversationId) },
////                clearConversationContent = { eventsHandler.onClearConversationContent(conversationId) },
//                blockUserClick = blockUser,
//                leaveGroup = { },
//                deleteGroup = { },
//                navigateToNotification = eventsHandler::setBottomSheetStateToMuteOptions
//            )
//        }
//        is OtherUserBottomSheetContent.Mute ->
//            MutingOptionsSheetContent(
//                mutingConversationState = bottomSheetState.conversationData.mutingConversationState,
//                onMuteConversation = {
//                    eventsHandler.onMutingConversationStatusChange(bottomSheetState.conversationData.conversationId, it)
//                },
//                onBackClick = eventsHandler::setBottomSheetStateToConversation
//            )
//        is OtherUserBottomSheetContent.ChangeRole ->
//            EditGroupRoleBottomSheet(
//                groupState = bottomSheetState.otherUserProfileGroupInfo,
//                changeMemberRole = eventsHandler::onChangeMemberRole,
//                closeChangeRoleBottomSheet = closeBottomSheet
//            )

    }

    BackHandler(bottomSheetState != null) {
        if (bottomSheetState is OtherUserBottomSheetContent.Mute) eventsHandler.setBottomSheetStateToConversation()
        else closeBottomSheet()
    }

}


sealed class OtherUserProfileBottomSheetState {
    object NotRequested : OtherUserProfileBottomSheetState()

    data class Requested(val otherUserNavigationOption: OtherUserNavigationOption) : OtherUserProfileBottomSheetState()
}
