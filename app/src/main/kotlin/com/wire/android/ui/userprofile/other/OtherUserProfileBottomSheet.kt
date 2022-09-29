package com.wire.android.ui.userprofile.other

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetContent
import com.wire.android.ui.home.conversationslist.bottomsheet.ConversationSheetState
import com.wire.kalium.logic.data.conversation.MutedConversationStatus

@Composable
fun OtherUserProfileBottomSheet(otherUserBottomSheetContentState: OtherUserBottomSheetContentState) {
    when (val test = otherUserBottomSheetContentState.test) {
        is Test.Conversation -> ConversationSheetContent(
            test.conversationSheetState
        )
        is Test.RoleChange -> {
            if (test.groupInfoAvailibility is GroupInfoAvailibility.Available) {
                EditGroupRoleBottomSheet(
                    groupState = test.groupInfoAvailibility.otherUserProfileGroupInfo,
                )
            }
        }
        Test.Loading -> {}
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberOtherUserBottomSheetContentState(
    requestOnDemand: () -> Unit,
    conversationSheetContent: ConversationSheetContent?,
    groupInfoAvailibility: GroupInfoAvailibility
): OtherUserBottomSheetContentState {
    val modalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)

    return remember(conversationSheetContent, groupInfoAvailibility) {
        OtherUserBottomSheetContentState(
            modalBottomSheetState,
            requestOnDemand,
            conversationSheetContent,
            groupInfoAvailibility
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
class OtherUserBottomSheetContentState(
    val modalBottomSheetState: ModalBottomSheetState,
    val requestOnDemand: () -> Unit,
    val conversationSheetContent: ConversationSheetContent? = null,
    val groupInfoAvailibility: GroupInfoAvailibility = GroupInfoAvailibility.NotAvailable
) {

    var test: Test by mutableStateOf(Test.Loading)

    suspend fun showConversationOption() {
        show()

        test = Test.Conversation(ConversationSheetState(conversationSheetContent))
    }

    suspend fun showChangeRoleOption() {
        show()

        test = Test.RoleChange(groupInfoAvailibility)
    }

    suspend fun show() {
        if (conversationSheetContent == null) {
            requestOnDemand()
        }

        modalBottomSheetState.show()
    }

    suspend fun hide() {
        modalBottomSheetState.hide()
    }

    fun muteConversation(mutedConversationStatus: MutedConversationStatus) {
        (test as? Test.Conversation)?.conversationSheetState?.muteConversation(mutedConversationStatus)
    }

    fun toMutingNotificationOption() {
        (test as? Test.Conversation)?.conversationSheetState?.toMutingNotificationOption()
    }

    fun toHome() {
        (test as? Test.Conversation)?.conversationSheetState?.toHome()
    }

}

sealed class Test {
    data class Conversation(val conversationSheetState: ConversationSheetState) : Test()
    data class RoleChange(val groupInfoAvailibility: GroupInfoAvailibility) : Test()

    object Loading : Test()
}

//@Composable
//fun OtherUSerProfileBottomSheetContent(
//    otherUserNavigationOptions: OtherUserNavigationOptions,
//) {
////    val otherUserProfileScreenViewModel: OtherUserProfileScreenViewModel = hiltViewModel()
////
////    LaunchedEffect(Unit) {
////        otherUserProfileScreenViewModel.getAdditionalConversationDetails()
////    }
//
//    when (otherUserNavigationOptions) {
//        is ConversationNavigationOptions.Home, ConversationNavigationOptions.MutingOptionsNotification -> {
//            ConversationSheetState(conversationNavigationOptions = otherUserNavigationOptions)
//        }
//        is OtherUserNavigationOption.ChangeRole -> {
//            EditGroupRoleBottomSheet(
//                groupState = bottomSheetState.otherUserProfileGroupInfo,
//                changeMemberRole = eventsHandler::onChangeMemberRole,
//                closeChangeRoleBottomSheet = closeBottomSheet
////            )
//        }
////        is OtherUserBottomSheetContent.Conversation -> {
////            val conversationId = bottomSheetState.conversationData.conversationId
////            ConversationMainSheetContent(
////                conversationSheetContent = bottomSheetState.conversationData,
////// TODO(profile): enable when implemented
//////
//////                addConversationToFavourites = { eventsHandler.onAddConversationToFavourites(conversationId) },
//////                moveConversationToFolder = { eventsHandler.onMoveConversationToFolder(conversationId) },
//////                moveConversationToArchive = { eventsHandler.onMoveConversationToArchive(conversationId) },
//////                clearConversationContent = { eventsHandler.onClearConversationContent(conversationId) },
////                blockUserClick = blockUser,
////                leaveGroup = { },
////                deleteGroup = { },
////                navigateToNotification = eventsHandler::setBottomSheetStateToMuteOptions
////            )
////        }
////        is OtherUserBottomSheetContent.Mute ->
////            MutingOptionsSheetContent(
////                mutingConversationState = bottomSheetState.conversationData.mutingConversationState,
////                onMuteConversation = {
////                    eventsHandler.onMutingConversationStatusChange(bottomSheetState.conversationData.conversationId, it)
////                },
////                onBackClick = eventsHandler::setBottomSheetStateToConversation
////            )
////        is OtherUserBottomSheetContent.ChangeRole ->
////            EditGroupRoleBottomSheet(
////                groupState = bottomSheetState.otherUserProfileGroupInfo,
////                changeMemberRole = eventsHandler::onChangeMemberRole,
////                closeChangeRoleBottomSheet = closeBottomSheet
////            )
//
//    }
//
//    BackHandler(bottomSheetState != null) {
//        if (bottomSheetState is OtherUserBottomSheetContent.Mute) eventsHandler.setBottomSheetStateToConversation()
//        else closeBottomSheet()
//    }
//
//}

